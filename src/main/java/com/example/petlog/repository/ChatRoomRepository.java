package com.example.petlog.repository;

import com.example.petlog.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 두 유저 간의 채팅방 찾기
    @Query("SELECT c FROM ChatRoom c WHERE (c.user1Id = :u1 AND c.user2Id = :u2) OR (c.user1Id = :u2 AND c.user2Id = :u1)")
    Optional<ChatRoom> findByUsers(@Param("u1") Long user1Id, @Param("u2") Long user2Id);

    // 내 채팅방 목록 조회 (활성 상태인 것만, 최근 메시지 순)
    @Query("SELECT c FROM ChatRoom c WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.isActive = true ORDER BY c.lastMessageAt DESC")
    List<ChatRoom> findActiveByUserId(@Param("userId") Long userId);
}