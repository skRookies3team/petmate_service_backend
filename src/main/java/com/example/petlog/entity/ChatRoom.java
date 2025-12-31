package com.example.petlog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter // Service에서 상태 변경(setLastMessage 등)을 위해 Setter 필요
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_room")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long user1Id;

    @Column(nullable = false)
    private Long user2Id;

    // 매칭 취소 시 방을 비활성화하거나 삭제하기 위한 플래그
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ★ 성능 최적화: 채팅방 목록 조회 시 메시지 테이블 조인 없이 마지막 내용 표시
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}