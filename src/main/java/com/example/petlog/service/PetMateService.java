package com.example.petlog.service;

import com.example.petlog.client.UserServiceClient;
import com.example.petlog.dto.request.LikeRequest;
import com.example.petlog.dto.request.PetMateFilterRequest;
import com.example.petlog.dto.request.PetMateRequest;
import com.example.petlog.dto.response.*;
import com.example.petlog.entity.PetMate;
import com.example.petlog.entity.PetMateMatch;
import com.example.petlog.repository.ChatRoomRepository;
import com.example.petlog.repository.PetMateMatchRepository;
import com.example.petlog.repository.PetMateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetMateService {

    private final PetMateRepository petMateRepository;
    private final PetMateMatchRepository petMateMatchRepository;
    private final MessageService messageService;
    private final UserServiceClient userServiceClient;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * [핵심 방어 로직]
     * DB에 프로필이 없으면 User-Service에서 정보를 가져와 자동 생성합니다.
     * User-Service가 죽어있어도 기본값으로 생성하여 500 에러를 방지합니다.
     */
    private void ensurePetMateProfileExists(Long userId) {
        if (userId == null) return;

        if (!petMateRepository.existsByUserId(userId)) {
            log.info("PetMate 프로필 없음. 자동 생성 시도 -> UserId: {}", userId);
            PetMate newProfile = new PetMate();
            newProfile.setUserId(userId);
            newProfile.setIsActive(true);
            newProfile.setIsOnline(true); // 기본적으로 온라인 처리

            try {
                // User Service 호출
                UserInfoResponse userInfo = userServiceClient.getUserInfo(userId);
                if (userInfo != null) {
                    newProfile.setUserName(userInfo.getUsername());
                    newProfile.setUserAvatar(userInfo.getProfileImage());
                    // 성별 매핑 (MALE -> 남성)
                    newProfile.setUserGender(mapGender(userInfo.getGenderType()));

                    // 펫 정보가 있다면 첫 번째 펫 정보 입력
                    if (userInfo.getPets() != null && !userInfo.getPets().isEmpty()) {
                        var firstPet = userInfo.getPets().get(0);
                        newProfile.setPetName(firstPet.getPetName());
                        newProfile.setPetBreed(firstPet.getBreed());
                        newProfile.setPetPhoto(firstPet.getProfileImage());
                        newProfile.setPetAge(firstPet.getAge());
                        newProfile.setPetGender(mapGender(firstPet.getGenderType()));
                    } else {
                        newProfile.setPetName("반려동물");
                        newProfile.setPetBreed("믹스견");
                    }
                }
            } catch (Exception e) {
                log.warn("User Service 연동 실패 (ID: {}). 기본값으로 생성합니다. Error: {}", userId, e.getMessage());
                newProfile.setUserName("User " + userId);
                newProfile.setPetName("반려동물");
                newProfile.setPetBreed("알수없음");
                newProfile.setUserGender("미설정");
            }
            petMateRepository.save(newProfile);
        }
    }

    private String mapGender(String genderType) {
        if (genderType == null) return "미설정";
        return switch (genderType.toUpperCase()) {
            case "MALE", "M" -> "남성";
            case "FEMALE", "F" -> "여성";
            default -> "미설정";
        };
    }

    /**
     * 후보 추천 조회
     */
    @Transactional
    public List<PetMateResponse> getCandidates(Long userId, PetMateFilterRequest filter) {
        ensurePetMateProfileExists(userId); // 내 프로필 확인

        List<PetMate> candidates;

        // 위치 기반 검색 vs 전체 검색
        if (filter.getLatitude() != null && filter.getLongitude() != null) {
            candidates = petMateRepository.findNearbyPetMates(
                    filter.getLatitude(),
                    filter.getLongitude(),
                    filter.getRadiusKm(),
                    userId);
        } else {
            candidates = petMateRepository.findActivePetMatesExcludingUser(userId);
        }

        // 필터링 및 변환
        return candidates.stream()
                .filter(pm -> filterByGender(pm, filter.getUserGender()))
                .filter(pm -> filterByBreed(pm, filter.getPetBreed()))
                .filter(pm -> filterByActivityLevel(pm, filter.getMinActivityLevel(), filter.getMaxActivityLevel()))
                .map(pm -> convertToResponse(pm, calculateDistance(
                        filter.getLatitude(), filter.getLongitude(),
                        pm.getLatitude(), pm.getLongitude())))
                .collect(Collectors.toList());
    }

    /**
     * 좋아요 (Like)
     */
    @Transactional
    public MatchResponse like(LikeRequest request) {
        Long senderId = request.getFromUserId();
        Long receiverId = request.getToUserId();

        // 양쪽 유저 프로필 존재 확인 (방어 로직)
        ensurePetMateProfileExists(senderId);
        ensurePetMateProfileExists(receiverId);

        // 이미 좋아요 했는지 확인
        if (petMateMatchRepository.existsByFromUserIdAndToUserId(senderId, receiverId)) {
            return MatchResponse.builder().isMatched(false).alreadyLiked(true).build();
        }

        // 상대방이 나를 이미 좋아했는지 확인 (매칭 성사 여부)
        var mutualLike = petMateMatchRepository.findByFromUserIdAndToUserId(receiverId, senderId);

        PetMateMatch match = PetMateMatch.builder()
                .fromUserId(senderId)
                .toUserId(receiverId)
                .build();

        Long chatRoomId = null;

        if (mutualLike.isPresent()) {
            // 매칭 성사!
            match.setStatus(PetMateMatch.MatchStatus.MATCHED);
            match.setMatchedAt(LocalDateTime.now());

            // 상대방 상태도 MATCHED로 변경
            PetMateMatch otherMatch = mutualLike.get();
            otherMatch.setStatus(PetMateMatch.MatchStatus.MATCHED);
            otherMatch.setMatchedAt(LocalDateTime.now());
            petMateMatchRepository.save(otherMatch);

            // 채팅방 생성 (실패해도 매칭은 취소되지 않도록 예외 처리)
            try {
                ChatRoomResponse chatRoom = messageService.createOrGetChatRoom(senderId, receiverId);
                chatRoomId = chatRoom.getId();
            } catch (Exception e) {
                log.error("채팅방 생성 실패: {}", e.getMessage());
            }
        } else {
            match.setStatus(PetMateMatch.MatchStatus.PENDING);
        }

        petMateMatchRepository.save(match);

        // 응답 생성
        PetMate matchedUser = petMateRepository.findFirstByUserIdOrderByIdAsc(receiverId).orElse(new PetMate());

        return MatchResponse.builder()
                .matchId(match.getId())
                .matchedUserId(receiverId)
                .matchedUserName(matchedUser.getUserName())
                .matchedUserAvatar(matchedUser.getUserAvatar())
                .petName(matchedUser.getPetName())
                .petPhoto(matchedUser.getPetPhoto())
                .isMatched(mutualLike.isPresent())
                .matchedAt(mutualLike.isPresent() ? LocalDateTime.now() : null)
                .chatRoomId(chatRoomId)
                .alreadyLiked(false)
                .build();
    }

    /**
     * 좋아요 취소 (Unlike)
     */
    @Transactional
    public boolean unlike(LikeRequest request) {
        var existingLike = petMateMatchRepository.findByFromUserIdAndToUserId(
                request.getFromUserId(), request.getToUserId());

        if (existingLike.isPresent()) {
            PetMateMatch match = existingLike.get();
            // PENDING 상태일 때만 취소 가능 (이미 매칭된 건 친구 끊기로 해야 함)
            if (match.getStatus() == PetMateMatch.MatchStatus.PENDING) {
                petMateMatchRepository.delete(match);
                return true;
            }
        }
        return false;
    }

    /**
     * 내가 보낸 요청 목록
     */
    public List<PendingRequestResponse> getSentRequests(Long userId) {
        ensurePetMateProfileExists(userId);
        return petMateMatchRepository.findSentPendingRequests(userId).stream()
                .map(match -> {
                    PetMate receiver = petMateRepository.findFirstByUserIdOrderByIdAsc(match.getToUserId()).orElse(new PetMate());
                    return PendingRequestResponse.builder()
                            .matchId(match.getId())
                            .fromUserId(match.getToUserId()) // 받는 사람 ID
                            .fromUserName(receiver.getUserName() != null ? receiver.getUserName() : "알 수 없음")
                            .fromUserAvatar(receiver.getUserAvatar())
                            .petName(receiver.getPetName())
                            .petPhoto(receiver.getPetPhoto())
                            .petBreed(receiver.getPetBreed())
                            .petAge(receiver.getPetAge())
                            .location(receiver.getLocation())
                            .createdAt(match.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 나에게 온 요청 목록
     */
    public List<PendingRequestResponse> getPendingRequests(Long userId) {
        ensurePetMateProfileExists(userId);
        return petMateMatchRepository.findPendingLikesForUser(userId).stream()
                .map(match -> {
                    PetMate requester = petMateRepository.findFirstByUserIdOrderByIdAsc(match.getFromUserId()).orElse(new PetMate());
                    return PendingRequestResponse.builder()
                            .matchId(match.getId())
                            .fromUserId(match.getFromUserId())
                            .fromUserName(requester.getUserName() != null ? requester.getUserName() : "알 수 없음")
                            .fromUserAvatar(requester.getUserAvatar())
                            .petName(requester.getPetName())
                            .petPhoto(requester.getPetPhoto())
                            .petBreed(requester.getPetBreed())
                            .petAge(requester.getPetAge())
                            .location(requester.getLocation())
                            .createdAt(match.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Long getPendingRequestsCount(Long userId) {
        return (long) petMateMatchRepository.findPendingLikesForUser(userId).size();
    }

    /**
     * 매칭된 친구 목록
     */
    public List<MatchResponse> getMatches(Long userId) {
        ensurePetMateProfileExists(userId);
        return petMateMatchRepository.findMatchedByUserId(userId).stream()
                .map(match -> {
                    Long matchedUserId = match.getFromUserId().equals(userId) ? match.getToUserId() : match.getFromUserId();
                    PetMate petMate = petMateRepository.findFirstByUserIdOrderByIdAsc(matchedUserId).orElse(new PetMate());

                    return MatchResponse.builder()
                            .matchId(match.getId())
                            .matchedUserId(matchedUserId)
                            .matchedUserName(petMate.getUserName() != null ? petMate.getUserName() : "알 수 없음")
                            .matchedUserAvatar(petMate.getUserAvatar())
                            .petName(petMate.getPetName())
                            .petPhoto(petMate.getPetPhoto())
                            .isMatched(true)
                            .matchedAt(match.getMatchedAt())
                            .build();
                })
                .filter(distinctByKey(MatchResponse::getMatchedUserId)) // 중복 제거
                .collect(Collectors.toList());
    }

    // 중복 제거용 유틸
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        ConcurrentHashMap.KeySetView<Object, Boolean> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * 프로필 생성 및 수정
     */
    @Transactional
    public PetMateResponse createOrUpdateProfile(PetMateRequest request) {
        PetMate petMate = petMateRepository.findFirstByUserIdOrderByIdAsc(request.getUserId())
                .orElse(new PetMate());

        petMate.setUserId(request.getUserId());
        petMate.setUserName(request.getUserName());
        petMate.setUserAvatar(request.getUserAvatar());
        petMate.setUserGender(request.getUserGender());
        petMate.setPetName(request.getPetName());
        petMate.setPetBreed(request.getPetBreed());
        petMate.setPetAge(request.getPetAge());
        petMate.setPetGender(request.getPetGender());
        petMate.setPetPhoto(request.getPetPhoto());
        petMate.setBio(request.getBio());
        petMate.setActivityLevel(request.getActivityLevel());
        petMate.setLatitude(request.getLatitude());
        petMate.setLongitude(request.getLongitude());
        petMate.setLocation(request.getLocation());
        petMate.setIsActive(true);

        PetMate saved = petMateRepository.save(petMate);
        return convertToResponse(saved, null);
    }

    // [기타 메서드]
    public List<Long> getLikedUserIds(Long userId) {
        return petMateMatchRepository.findByFromUserId(userId).stream()
                .filter(m -> m.getStatus() == PetMateMatch.MatchStatus.PENDING)
                .map(PetMateMatch::getToUserId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateOnlineStatus(Long userId, boolean isOnline) {
        ensurePetMateProfileExists(userId);
        petMateRepository.findFirstByUserIdOrderByIdAsc(userId).ifPresent(petMate -> {
            petMate.setIsOnline(isOnline);
            petMate.setLastActiveAt(LocalDateTime.now());
            petMateRepository.save(petMate);
        });
    }

    @Transactional
    public boolean updateLocation(Long userId, Double latitude, Double longitude, String location) {
        ensurePetMateProfileExists(userId);
        PetMate petMate = petMateRepository.findFirstByUserIdOrderByIdAsc(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다.")); // ensure했으므로 여기까진 안 옴

        petMate.setLatitude(latitude);
        petMate.setLongitude(longitude);
        if (location != null) petMate.setLocation(location);

        petMateRepository.save(petMate);
        return true;
    }

    public PetMateResponse getSavedLocation(Long userId) {
        return petMateRepository.findFirstByUserIdOrderByIdAsc(userId)
                .map(pm -> PetMateResponse.builder()
                        .userId(pm.getUserId())
                        .latitude(pm.getLatitude())
                        .longitude(pm.getLongitude())
                        .location(pm.getLocation())
                        .build())
                .orElse(null);
    }

    @Transactional
    public MatchResponse respondToRequest(Long matchId, Long userId, boolean accept) {
        PetMateMatch match = petMateMatchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("요청을 찾을 수 없습니다: " + matchId));

        if (!match.getToUserId().equals(userId)) {
            throw new IllegalArgumentException("이 요청에 응답할 권한이 없습니다.");
        }

        if (match.getStatus() != PetMateMatch.MatchStatus.PENDING) {
            return MatchResponse.builder().matchId(matchId).isMatched(match.getStatus() == PetMateMatch.MatchStatus.MATCHED).alreadyLiked(true).build();
        }

        Long chatRoomId = null;
        PetMate requester = petMateRepository.findFirstByUserIdOrderByIdAsc(match.getFromUserId()).orElse(new PetMate());

        if (accept) {
            match.setStatus(PetMateMatch.MatchStatus.MATCHED);
            match.setMatchedAt(LocalDateTime.now());

            var reverseMatch = petMateMatchRepository.findByFromUserIdAndToUserId(userId, match.getFromUserId());
            if (reverseMatch.isEmpty()) {
                PetMateMatch newMatch = PetMateMatch.builder()
                        .fromUserId(userId)
                        .toUserId(match.getFromUserId())
                        .status(PetMateMatch.MatchStatus.MATCHED)
                        .matchedAt(LocalDateTime.now())
                        .build();
                petMateMatchRepository.save(newMatch);
            } else {
                PetMateMatch existing = reverseMatch.get();
                existing.setStatus(PetMateMatch.MatchStatus.MATCHED);
                existing.setMatchedAt(LocalDateTime.now());
                petMateMatchRepository.save(existing);
            }

            try {
                ChatRoomResponse chatRoom = messageService.createOrGetChatRoom(userId, match.getFromUserId());
                chatRoomId = chatRoom.getId();
            } catch (Exception e) {
                log.error("채팅방 생성 실패", e);
            }
        } else {
            match.setStatus(PetMateMatch.MatchStatus.REJECTED);
        }

        petMateMatchRepository.save(match);

        return MatchResponse.builder()
                .matchId(matchId)
                .matchedUserId(match.getFromUserId())
                .matchedUserName(requester.getUserName())
                .matchedUserAvatar(requester.getUserAvatar())
                .petName(requester.getPetName())
                .petPhoto(requester.getPetPhoto())
                .isMatched(accept)
                .matchedAt(accept ? LocalDateTime.now() : null)
                .chatRoomId(chatRoomId)
                .alreadyLiked(false)
                .build();
    }

    @Transactional
    public boolean unfriend(Long userId, Long matchedUserId) {
        var match1 = petMateMatchRepository.findByFromUserIdAndToUserId(userId, matchedUserId);
        var match2 = petMateMatchRepository.findByFromUserIdAndToUserId(matchedUserId, userId);

        boolean deleted = false;
        if (match1.isPresent()) {
            petMateMatchRepository.delete(match1.get());
            deleted = true;
        }
        if (match2.isPresent()) {
            petMateMatchRepository.delete(match2.get());
            deleted = true;
        }
        return deleted;
    }

    // [Filter Logic]
    private boolean filterByGender(PetMate pm, String gender) {
        if (gender == null || "all".equalsIgnoreCase(gender)) return true;
        if ("male".equalsIgnoreCase(gender)) return "남성".equals(pm.getUserGender());
        if ("female".equalsIgnoreCase(gender)) return "여성".equals(pm.getUserGender());
        return true;
    }

    private boolean filterByBreed(PetMate pm, String breed) {
        if (breed == null || "all".equalsIgnoreCase(breed)) return true;
        return breed.equals(pm.getPetBreed());
    }

    private boolean filterByActivityLevel(PetMate pm, Integer min, Integer max) {
        if (min == null && max == null) return true;
        int level = pm.getActivityLevel() != null ? pm.getActivityLevel() : 0;
        if (min != null && level < min) return false;
        if (max != null && level > max) return false;
        return true;
    }

    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 100.0) / 100.0;
    }

    private PetMateResponse convertToResponse(PetMate petMate, Double distance) {
        return PetMateResponse.builder()
                .id(petMate.getId())
                .userId(petMate.getUserId())
                .userName(petMate.getUserName())
                .userAvatar(petMate.getUserAvatar())
                .userGender(petMate.getUserGender())
                .petName(petMate.getPetName())
                .petBreed(petMate.getPetBreed())
                .petAge(petMate.getPetAge())
                .petGender(petMate.getPetGender())
                .petPhoto(petMate.getPetPhoto())
                .bio(petMate.getBio())
                .activityLevel(petMate.getActivityLevel())
                .distance(distance)
                .location(petMate.getLocation())
                .commonInterests(new ArrayList<>())
                .matchScore(calculateMatchScore(petMate))
                .isOnline(petMate.getIsOnline())
                .lastActiveAt(petMate.getLastActiveAt())
                .build();
    }

    private Integer calculateMatchScore(PetMate petMate) {
        return (int) (Math.random() * 30 + 70);
    }
}