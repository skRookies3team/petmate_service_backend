package com.example.petlog.repository;

import com.example.petlog.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    // 채팅방 메시지 내역 조회
    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    // 읽지 않은 메시지 수 카운트
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :roomId AND m.senderId != :myId AND m.isRead = false")
    Long countUnreadMessages(@Param("roomId") Long chatRoomId, @Param("myId") Long myId);

    // 메시지 읽음 처리
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.chatRoom.id = :roomId AND m.senderId != :myId AND m.isRead = false")
    void markAsRead(@Param("roomId") Long chatRoomId, @Param("myId") Long myId);
}