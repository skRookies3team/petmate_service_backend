package com.example.petlog.dto.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetMateRequest {

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
    private Double latitude;
    private Double longitude;
    private String location;
    private List<String> interests;
}
