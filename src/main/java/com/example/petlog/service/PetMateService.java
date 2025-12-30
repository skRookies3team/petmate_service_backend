package com.example.petlog.service;

import com.example.petlog.client.UserServiceClient;
import com.example.petlog.dto.request.LikeRequest;
import com.example.petlog.dto.request.PetMateFilterRequest;
import com.example.petlog.dto.request.PetMateRequest;
import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MatchResponse;
import com.example.petlog.dto.response.PetMateResponse;
import com.example.petlog.dto.response.UserInfoResponse;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PetMateService {

    private final PetMateRepository petMateRepository;
    private final PetMateMatchRepository petMateMatchRepository;
    private final MessageService messageService;
    private final UserServiceClient userServiceClient;

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

        // Apply filters
        return candidates.stream()
                .filter(pm -> filterByGender(pm, filter.getUserGender()))
                .filter(pm -> filterByBreed(pm, filter.getPetBreed()))
                .filter(pm -> filterByActivityLevel(pm, filter.getMinActivityLevel(), filter.getMaxActivityLevel()))
                .map(pm -> convertToResponse(pm, calculateDistance(
                        filter.getLatitude(), filter.getLongitude(),
                        pm.getLatitude(), pm.getLongitude())))
                .collect(Collectors.toList());
    }

    @Transactional
    public MatchResponse like(LikeRequest request) {
        // Check if already liked
        if (petMateMatchRepository.existsByFromUserIdAndToUserId(request.getFromUserId(), request.getToUserId())) {
            return MatchResponse.builder()
                    .isMatched(false)
                    .alreadyLiked(true)
                    .build();
        }

        // Check if mutual like exists
        var mutualLike = petMateMatchRepository.findByFromUserIdAndToUserId(
                request.getToUserId(), request.getFromUserId());

        PetMateMatch match = PetMateMatch.builder()
                .fromUserId(request.getFromUserId())
                .toUserId(request.getToUserId())
                .build();

        Long chatRoomId = null;

        if (mutualLike.isPresent()) {
            // It's a match!
            match.setStatus(PetMateMatch.MatchStatus.MATCHED);
            match.setMatchedAt(LocalDateTime.now());

            // Update the other person's like to matched
            PetMateMatch otherMatch = mutualLike.get();
            otherMatch.setStatus(PetMateMatch.MatchStatus.MATCHED);
            otherMatch.setMatchedAt(LocalDateTime.now());
            petMateMatchRepository.save(otherMatch);

            // Create chat room for the matched users
            ChatRoomResponse chatRoom = messageService.createOrGetChatRoom(
                    request.getFromUserId(), request.getToUserId());
            chatRoomId = chatRoom.getId();
        } else {
            match.setStatus(PetMateMatch.MatchStatus.PENDING);
        }

        petMateMatchRepository.save(match);

        // Get matched user info
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

    @Transactional
    public boolean unlike(LikeRequest request) {
        var existingLike = petMateMatchRepository.findByFromUserIdAndToUserId(
                request.getFromUserId(), request.getToUserId());

        if (existingLike.isPresent()) {
            PetMateMatch match = existingLike.get();
            // Only allow cancellation of PENDING likes
            if (match.getStatus() == PetMateMatch.MatchStatus.PENDING) {
                petMateMatchRepository.delete(match);
                return true;
            }
        }
        return false;
    }

    public List<Long> getLikedUserIds(Long userId) {
        return petMateMatchRepository.findByFromUserId(userId).stream()
                .filter(m -> m.getStatus() == PetMateMatch.MatchStatus.PENDING)
                .map(PetMateMatch::getToUserId)
                .collect(Collectors.toList());
    }

    public List<MatchResponse> getMatches(Long userId) {
        return petMateMatchRepository.findMatchedByUserId(userId).stream()
                .map(match -> {
                    Long matchedUserId = match.getFromUserId().equals(userId)
                            ? match.getToUserId()
                            : match.getFromUserId();
                    PetMate petMate = petMateRepository.findFirstByUserIdOrderByIdAsc(matchedUserId).orElse(null);

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

    @Transactional
    public void updateOnlineStatus(Long userId, boolean isOnline) {
        petMateRepository.findFirstByUserIdOrderByIdAsc(userId).ifPresent(petMate -> {
            petMate.setIsOnline(isOnline);
            petMate.setLastActiveAt(LocalDateTime.now());
            petMateRepository.save(petMate);
        });
    }

    /**
     * 사용자 위치 정보만 업데이트 (경량 API)
     * 레코드가 없으면 user-service에서 실제 사용자 정보를 가져와서 새로 생성
     */
    @Transactional
    public boolean updateLocation(Long userId, Double latitude, Double longitude, String location) {
        PetMate petMate = petMateRepository.findFirstByUserIdOrderByIdAsc(userId)
                .orElseGet(() -> createPetMateFromUserService(userId));

        petMate.setLatitude(latitude);
        petMate.setLongitude(longitude);
        if (location != null) {
            petMate.setLocation(location);
        }
        petMateRepository.save(petMate);
        return true;
    }

    /**
     * user-service에서 실제 사용자 정보를 가져와 PetMate 프로필 생성
     */
    private PetMate createPetMateFromUserService(Long userId) {
        PetMate newPetMate = new PetMate();
        newPetMate.setUserId(userId);
        newPetMate.setIsOnline(true);
        newPetMate.setIsActive(true);

        try {
            // user-service에서 실제 사용자 정보 가져오기
            UserInfoResponse userInfo = userServiceClient.getUserInfo(userId);

            if (userInfo != null) {
                // 사용자 정보 설정
                newPetMate.setUserName(userInfo.getUsername() != null ? userInfo.getUsername() : "사용자" + userId);
                newPetMate.setUserAvatar(userInfo.getProfileImage());

                // 성별 매핑 (MALE -> 남성, FEMALE -> 여성)
                if (userInfo.getGenderType() != null) {
                    String gender = switch (userInfo.getGenderType().toUpperCase()) {
                        case "MALE", "M" -> "남성";
                        case "FEMALE", "F" -> "여성";
                        default -> "미설정";
                    };
                    newPetMate.setUserGender(gender);
                } else {
                    newPetMate.setUserGender("미설정");
                }

                // 첫 번째 펫 정보 설정 (있는 경우)
                if (userInfo.getPets() != null && !userInfo.getPets().isEmpty()) {
                    UserInfoResponse.PetInfo firstPet = userInfo.getPets().get(0);
                    newPetMate.setPetName(firstPet.getPetName() != null ? firstPet.getPetName() : "미등록");
                    newPetMate.setPetBreed(firstPet.getBreed() != null ? firstPet.getBreed() : "미등록");
                    newPetMate.setPetPhoto(firstPet.getProfileImage());
                    newPetMate.setPetAge(firstPet.getAge());

                    // 펫 성별 매핑
                    if (firstPet.getGenderType() != null) {
                        String petGender = switch (firstPet.getGenderType().toUpperCase()) {
                            case "MALE", "M" -> "남아";
                            case "FEMALE", "F" -> "여아";
                            default -> null;
                        };
                        newPetMate.setPetGender(petGender);
                    }
                } else {
                    newPetMate.setPetName("미등록");
                    newPetMate.setPetBreed("미등록");
                }

                log.info("Successfully fetched user info from user-service for userId: {}", userId);
            } else {
                setDefaultValues(newPetMate, userId);
            }
        } catch (Exception e) {
            // user-service 호출 실패 시 기본값으로 설정
            log.warn("Failed to fetch user info from user-service for userId: {}. Using default values. Error: {}",
                    userId, e.getMessage());
            setDefaultValues(newPetMate, userId);
        }

        return newPetMate;
    }

    /**
     * 기본값 설정 (user-service 연동 실패 시)
     */
    private void setDefaultValues(PetMate petMate, Long userId) {
        petMate.setUserName("사용자" + userId);
        petMate.setUserGender("미설정");
        petMate.setPetName("미등록");
        petMate.setPetBreed("미등록");
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
                .location(petMate.getLocation())
                .commonInterests(new ArrayList<>()) // TODO: Implement interests
                .matchScore(calculateMatchScore(petMate))
                .isOnline(petMate.getIsOnline())
                .lastActiveAt(petMate.getLastActiveAt())
                .build();
    }

    private Integer calculateMatchScore(PetMate petMate) {
        // Simple match score calculation - can be enhanced
        return (int) (Math.random() * 30 + 70); // Random 70-100 for now
    }
}
