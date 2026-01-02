package com.example.petlog.repository;

import com.example.petlog.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 1. 채팅방의 모든 메시지 조회 (오래된 순) - 채팅방 입장 시 사용
    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    // 2. 최신 메시지 50개 조회 (최신 순) - 페이징/미리보기용
    List<Message> findTop50ByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    // 3. 읽지 않은 메시지 목록 조회
    List<Message> findByChatRoomIdAndIsReadFalse(Long chatRoomId);

    // 4. 특정 채팅방에서 상대방이 보낸 안 읽은 메시지 개수
    Long countByChatRoomIdAndIsReadFalseAndSenderIdNot(Long chatRoomId, Long senderId);
}