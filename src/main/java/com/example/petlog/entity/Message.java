package com.example.petlog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(nullable = false)
    private Long senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    // 텍스트, 이미지, 시스템 메시지 등을 구분
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // 내부 Enum 정의
    public enum MessageType {
        TEXT, IMAGE, SYSTEM
    }
}