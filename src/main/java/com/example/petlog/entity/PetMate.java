package com.example.petlog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pet_mates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetMate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String userName;

    private String userAvatar;

    @Column(nullable = false)
    private String userGender;

    @Column(nullable = false)
    private String petName;

    @Column(nullable = false)
    private String petBreed;

    private Integer petAge;

    private String petGender;

    private String petPhoto;

    private String bio;

    private Integer activityLevel;

    private Double latitude;

    private Double longitude;

    private String location;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isOnline = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    private LocalDateTime lastActiveAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
