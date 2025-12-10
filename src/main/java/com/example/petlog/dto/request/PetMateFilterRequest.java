package com.example.petlog.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetMateFilterRequest {

    private Double latitude;
    private Double longitude;
    @Builder.Default
    private Double radiusKm = 3.0;
    private String userGender; // "all", "male", "female"
    private String petBreed; // "all" or specific breed
    private Integer minActivityLevel;
    private Integer maxActivityLevel;
}
