package com.example.petlog.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetMateResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String userGender;
    private String petName;
    private String petBreed;
    private Integer petAge;
    private String petGender;
    private String petPhoto;
    private String bio;
    private Integer activityLevel;
    private Double distance;
    private String location;
    private List<String> commonInterests;
    private Integer matchScore;
    private Boolean isOnline;
    private LocalDateTime lastActiveAt;
}
