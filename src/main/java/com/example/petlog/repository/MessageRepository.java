package com.example.petlog.repository;

import com.example.petlog.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    Page<Message> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.senderId != :userId AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.senderId != :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.chatRoom.id = :chatRoomId AND m.senderId != :userId AND m.isRead = false")
    void markAsRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.createdAt DESC LIMIT 1")
    Message findLatestMessage(@Param("chatRoomId") Long chatRoomId);
}
