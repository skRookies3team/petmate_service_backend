package com.example.petlog.service;

import com.example.petlog.dto.request.LikeRequest;
import com.example.petlog.dto.request.PetMateFilterRequest;
import com.example.petlog.dto.request.PetMateRequest;
import com.example.petlog.dto.response.ChatRoomResponse;
import com.example.petlog.dto.response.MatchResponse;
import com.example.petlog.dto.response.PetMateResponse;
import com.example.petlog.entity.PetMate;
import com.example.petlog.entity.PetMateMatch;
import com.example.petlog.repository.PetMateMatchRepository;
import com.example.petlog.repository.PetMateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetMateService {

    private final PetMateRepository petMateRepository;
    private final PetMateMatchRepository petMateMatchRepository;
    private final MessageService messageService;

    @Transactional
    public PetMateResponse createOrUpdateProfile(PetMateRequest request) {
        PetMate petMate = petMateRepository.findByUserId(request.getUserId())
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
        PetMate matchedPetMate = petMateRepository.findByUserId(request.getToUserId()).orElse(null);

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
                    PetMate petMate = petMateRepository.findByUserId(matchedUserId).orElse(null);

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
        petMateRepository.findByUserId(userId).ifPresent(petMate -> {
            petMate.setIsOnline(isOnline);
            petMate.setLastActiveAt(LocalDateTime.now());
            petMateRepository.save(petMate);
        });
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
