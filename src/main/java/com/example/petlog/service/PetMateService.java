package com.example.petlog.service;

import com.example.petlog.client.NotificationServiceClient;
import com.example.petlog.dto.request.LikeRequest;
import com.example.petlog.dto.request.PetMateFilterRequest;
import com.example.petlog.dto.request.PetMateRequest;
import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MatchResponse;
import com.example.petlog.dto.response.PendingRequestResponse;
import com.example.petlog.dto.response.PetMateResponse;
import com.example.petlog.entity.PetMate;
import com.example.petlog.entity.PetMateMatch;
import com.example.petlog.repository.PetMateMatchRepository;
import com.example.petlog.repository.PetMateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PetMateService {

    private final PetMateRepository petMateRepository;
    private final PetMateMatchRepository petMateMatchRepository;
    private final MessageService messageService;
    private final NotificationServiceClient notificationServiceClient;

    /**
     * 펫메이트 프로필 생성 또는 수정
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

    /**
     * 매칭 후보자 목록 조회 (필터링 및 거리 계산 포함)
     */
    public List<PetMateResponse> getCandidates(Long userId, PetMateFilterRequest filter) {
        List<PetMate> candidates;

        if (filter.getLatitude() != null && filter.getLongitude() != null) {
            candidates = petMateRepository.findNearbyPetMates(
                    filter.getLatitude(),
                    filter.getLongitude(),
                    filter.getRadiusKm(),
                    userId);
        } else {
            candidates = petMateRepository.findActivePetMatesExcludingUser(userId);
        }

        // 이미 좋아요하거나 매칭된 유저는 제외
        List<Long> interactedUserIds = petMateMatchRepository.findAllByUserId(userId).stream()
                .map(m -> m.getFromUserId().equals(userId) ? m.getToUserId() : m.getFromUserId())
                .collect(Collectors.toList());

        return candidates.stream()
                .filter(pm -> !interactedUserIds.contains(pm.getUserId())) // 이미 상호작용 한 유저 제외
                .filter(pm -> filterByGender(pm, filter.getUserGender()))
                .filter(pm -> filterByBreed(pm, filter.getPetBreed()))
                .filter(pm -> filterByActivityLevel(pm, filter.getMinActivityLevel(), filter.getMaxActivityLevel()))
                .map(pm -> convertToResponse(pm, calculateDistance(
                        filter.getLatitude(), filter.getLongitude(),
                        pm.getLatitude(), pm.getLongitude())))
                .collect(Collectors.toList());
    }

    /**
     * 좋아요 (매칭 시도)
     */
    @Transactional
    public MatchResponse like(LikeRequest request) {
        // 이미 좋아요를 눌렀는지 확인
        if (petMateMatchRepository.existsByFromUserIdAndToUserId(request.getFromUserId(), request.getToUserId())) {
            return MatchResponse.builder()
                    .isMatched(false)
                    .alreadyLiked(true)
                    .build();
        }

        // 상대방도 나를 좋아요 했는지 확인 (매칭 여부 판단)
        var mutualLike = petMateMatchRepository.findByFromUserIdAndToUserId(
                request.getToUserId(), request.getFromUserId());

        PetMateMatch match = PetMateMatch.builder()
                .fromUserId(request.getFromUserId())
                .toUserId(request.getToUserId())
                .build();

        Long chatRoomId = null;

        if (mutualLike.isPresent()) {
            // [매칭 성공!]
            match.setStatus(PetMateMatch.MatchStatus.MATCHED);
            match.setMatchedAt(LocalDateTime.now());

            // 상대방의 좋아요 상태도 MATCHED로 업데이트
            PetMateMatch otherMatch = mutualLike.get();
            otherMatch.setStatus(PetMateMatch.MatchStatus.MATCHED);
            otherMatch.setMatchedAt(LocalDateTime.now());
            petMateMatchRepository.save(otherMatch);

            // 1. 채팅방 자동 생성
            ChatRoomResponse chatRoom = messageService.createOrGetChatRoom(
                    request.getFromUserId(), request.getToUserId());
            chatRoomId = chatRoom.getId();

            // 2. 양쪽 유저에게 매칭 성공 알림 발송
            sendMatchNotification(request.getFromUserId(), request.getToUserId());
            sendMatchNotification(request.getToUserId(), request.getFromUserId());

        } else {
            // [매칭 대기]
            match.setStatus(PetMateMatch.MatchStatus.PENDING);
        }

        petMateMatchRepository.save(match);

        // 반환값 생성
        PetMate matchedPetMate = petMateRepository.findFirstByUserIdOrderByIdAsc(request.getToUserId()).orElse(null);

        return MatchResponse.builder()
                .matchId(match.getId())
                .matchedUserId(request.getToUserId())
                .matchedUserName(matchedPetMate != null ? matchedPetMate.getUserName() : null)
                .matchedUserAvatar(matchedPetMate != null ? matchedPetMate.getUserAvatar() : null)
                .petName(matchedPetMate != null ? matchedPetMate.getPetName() : null)
                .petPhoto(matchedPetMate != null ? matchedPetMate.getPetPhoto() : null)
                .isMatched(mutualLike.isPresent())
                .matchedAt(mutualLike.isPresent() ? LocalDateTime.now() : null)
                .chatRoomId(chatRoomId)
                .alreadyLiked(false)
                .build();
    }

    /**
     * 좋아요 취소 (PENDING 상태일 때만)
     */
    @Transactional
    public boolean unlike(LikeRequest request) {
        var existingLike = petMateMatchRepository.findByFromUserIdAndToUserId(
                request.getFromUserId(), request.getToUserId());

        if (existingLike.isPresent()) {
            PetMateMatch match = existingLike.get();
            if (match.getStatus() == PetMateMatch.MatchStatus.PENDING) {
                petMateMatchRepository.delete(match);
                return true;
            }
        }
        return false;
    }

    /**
     * 내가 좋아요를 보낸 사용자 ID 목록 조회 (PENDING 상태만)
     */
    public List<Long> getLikedUserIds(Long userId) {
        return petMateMatchRepository.findByFromUserId(userId).stream()
                .filter(m -> m.getStatus() == PetMateMatch.MatchStatus.PENDING)
                .map(PetMateMatch::getToUserId)
                .collect(Collectors.toList());
    }

    /**
     * 매칭된 목록 조회 (서로 좋아요)
     */
    public List<MatchResponse> getMatches(Long userId) {
        return petMateMatchRepository.findMatchedByUserId(userId).stream()
                .map(match -> {
                    Long matchedUserId = match.getFromUserId().equals(userId)
                            ? match.getToUserId()
                            : match.getFromUserId();
                    PetMate petMate = petMateRepository.findFirstByUserIdOrderByIdAsc(matchedUserId).orElse(null);

                    // 이미 채팅방이 존재할 것이므로 조회
                    // (성능상 목록 조회시마다 호출하기 부담스러우면 null 처리 후 상세에서 조회하거나, 캐싱 필요)
                    // 여기서는 단순화를 위해 ID 조회 로직 생략 또는 필요한 경우 추가

                    return MatchResponse.builder()
                            .matchId(match.getId())
                            .matchedUserId(matchedUserId)
                            .matchedUserName(petMate != null ? petMate.getUserName() : null)
                            .matchedUserAvatar(petMate != null ? petMate.getUserAvatar() : null)
                            .petName(petMate != null ? petMate.getPetName() : null)
                            .petPhoto(petMate != null ? petMate.getPetPhoto() : null)
                            .isMatched(true)
                            .matchedAt(match.getMatchedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 받은 매칭 요청 목록 조회
     */
    public List<PendingRequestResponse> getPendingRequests(Long userId) {
        return petMateMatchRepository.findPendingLikesForUser(userId).stream()
                .map(match -> {
                    PetMate sender = petMateRepository.findFirstByUserIdOrderByIdAsc(match.getFromUserId()).orElse(null);
                    return PendingRequestResponse.builder()
                            .matchId(match.getId())
                            .fromUserId(match.getFromUserId())
                            .fromUserName(sender != null ? sender.getUserName() : "알 수 없음")
                            .fromUserAvatar(sender != null ? sender.getUserAvatar() : null)
                            .petName(sender != null ? sender.getPetName() : null)
                            .petPhoto(sender != null ? sender.getPetPhoto() : null)
                            .matchScore(match.getMatchScore())
                            .createdAt(match.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 받은 매칭 요청 수 조회 (배지 알림용)
     */
    public Long getPendingRequestsCount(Long userId) {
        return petMateMatchRepository.countPendingRequests(userId);
    }

    /**
     * 매칭 요청 수락/거절 처리
     */
    @Transactional
    public MatchResponse respondToRequest(Long matchId, Long userId, Boolean accept) {
        PetMateMatch match = petMateMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("매칭 요청을 찾을 수 없습니다."));

        // 본인 확인 (요청 받은 사람이 맞는지)
        if (!match.getToUserId().equals(userId)) {
            throw new RuntimeException("권한이 없습니다.");
        }

        // 이미 처리된 요청인지 확인
        if (match.getStatus() != PetMateMatch.MatchStatus.PENDING) {
            throw new RuntimeException("이미 처리된 요청입니다.");
        }

        if (accept) {
            // [수락]
            // 1. 받은 요청(A->B) 상태를 MATCHED로 변경
            match.setStatus(PetMateMatch.MatchStatus.MATCHED);
            match.setMatchedAt(LocalDateTime.now());
            petMateMatchRepository.save(match);

            // 2. 반대 방향(B->A) 매칭 데이터 생성 및 MATCHED로 저장
            PetMateMatch reverseMatch = PetMateMatch.builder()
                    .fromUserId(userId)
                    .toUserId(match.getFromUserId())
                    .status(PetMateMatch.MatchStatus.MATCHED)
                    .matchedAt(LocalDateTime.now())
                    .matchScore(match.getMatchScore())
                    .build();
            petMateMatchRepository.save(reverseMatch);

            // 3. 채팅방 생성
            ChatRoomResponse chatRoom = messageService.createOrGetChatRoom(match.getFromUserId(), userId);

            // 4. 알림 발송 (요청자에게 매칭 성공 알림)
            sendMatchNotification(match.getFromUserId(), userId);

            // 응답 생성
            PetMate matchedUser = petMateRepository.findFirstByUserIdOrderByIdAsc(match.getFromUserId()).orElse(null);
            return MatchResponse.builder()
                    .matchId(match.getId())
                    .matchedUserId(match.getFromUserId())
                    .matchedUserName(matchedUser != null ? matchedUser.getUserName() : null)
                    .matchedUserAvatar(matchedUser != null ? matchedUser.getUserAvatar() : null)
                    .petName(matchedUser != null ? matchedUser.getPetName() : null)
                    .petPhoto(matchedUser != null ? matchedUser.getPetPhoto() : null)
                    .isMatched(true)
                    .matchedAt(LocalDateTime.now())
                    .chatRoomId(chatRoom.getId())
                    .alreadyLiked(false)
                    .build();

        } else {
            // [거절]
            // 요청 데이터 삭제 (또는 REJECTED 상태로 변경)
            petMateMatchRepository.delete(match);

            return MatchResponse.builder()
                    .isMatched(false)
                    .alreadyLiked(false)
                    .build();
        }
    }

    /**
     * 온라인 상태 업데이트
     */
    @Transactional
    public void updateOnlineStatus(Long userId, boolean isOnline) {
        petMateRepository.findFirstByUserIdOrderByIdAsc(userId).ifPresent(petMate -> {
            petMate.setIsOnline(isOnline);
            petMate.setLastActiveAt(LocalDateTime.now());
            petMateRepository.save(petMate);
        });
    }

    /**
     * 사용자 위치 정보 업데이트
     */
    @Transactional
    public boolean updateLocation(Long userId, Double latitude, Double longitude, String location) {
        PetMate petMate = petMateRepository.findFirstByUserIdOrderByIdAsc(userId)
                .orElseGet(() -> {
                    PetMate newPetMate = new PetMate();
                    newPetMate.setUserId(userId);
                    newPetMate.setUserName("사용자" + userId);
                    newPetMate.setUserGender("미설정");
                    newPetMate.setPetName("미등록");
                    newPetMate.setPetBreed("미등록");
                    newPetMate.setIsOnline(true);
                    newPetMate.setIsActive(true);
                    return newPetMate;
                });

        petMate.setLatitude(latitude);
        petMate.setLongitude(longitude);
        if (location != null) {
            petMate.setLocation(location);
        }
        petMateRepository.save(petMate);
        return true;
    }

    /**
     * 사용자의 저장된 위치 정보 조회
     */
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

    // --- Private Helper Methods ---

    /**
     * 매칭 성공 알림 발송
     */
    private void sendMatchNotification(Long receiverId, Long senderId) {
        try {
            PetMate sender = petMateRepository.findFirstByUserIdOrderByIdAsc(senderId).orElse(null);
            String senderName = (sender != null) ? sender.getUserName() : "알 수 없는 사용자";

            notificationServiceClient.sendNotification(new NotificationServiceClient.NotificationRequest(
                    receiverId,
                    "🎉 펫메이트 매칭 성공!",
                    senderName + "님과 매칭되었습니다. 지금 채팅을 시작해보세요!",
                    "MATCH"
            ));
        } catch (Exception e) {
            log.error("매칭 알림 발송 실패: receiverId={}, error={}", receiverId, e.getMessage());
        }
    }

    private boolean filterByGender(PetMate pm, String gender) {
        if (gender == null || "all".equalsIgnoreCase(gender))
            return true;
        if ("male".equalsIgnoreCase(gender))
            return "남성".equals(pm.getUserGender());
        if ("female".equalsIgnoreCase(gender))
            return "여성".equals(pm.getUserGender());
        return true;
    }

    private boolean filterByBreed(PetMate pm, String breed) {
        if (breed == null || "all".equalsIgnoreCase(breed))
            return true;
        return breed.equals(pm.getPetBreed());
    }

    private boolean filterByActivityLevel(PetMate pm, Integer min, Integer max) {
        if (min == null && max == null)
            return true;
        int level = pm.getActivityLevel() != null ? pm.getActivityLevel() : 0;
        if (min != null && level < min)
            return false;
        if (max != null && level > max)
            return false;
        return true;
    }

    private Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null)
            return null;

        final int R = 6371; // Earth's radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(R * c * 100.0) / 100.0; // Round to 2 decimal places
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
                .latitude(petMate.getLatitude())
                .longitude(petMate.getLongitude())
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