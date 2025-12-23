package com.example.petlog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pet_mate_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetMateMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromUserId;

    @Column(nullable = false)
    private Long toUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private Integer matchScore;

    private LocalDateTime matchedAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MatchStatus {
        PENDING,    // 좋아요만 보낸 상태
        MATCHED,    // 서로 좋아요 (매칭 성공)
        REJECTED    // 거절됨
    }
}
