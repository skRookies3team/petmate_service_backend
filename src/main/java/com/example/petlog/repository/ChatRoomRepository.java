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

    @Query("SELECT c FROM ChatRoom c WHERE (c.user1Id = :userId1 AND c.user2Id = :userId2) OR (c.user1Id = :userId2 AND c.user2Id = :userId1)")
    Optional<ChatRoom> findByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT c FROM ChatRoom c WHERE c.user1Id = :userId OR c.user2Id = :userId ORDER BY c.lastMessageAt DESC")
    List<ChatRoom> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM ChatRoom c WHERE (c.user1Id = :userId OR c.user2Id = :userId) AND c.isActive = true ORDER BY c.lastMessageAt DESC")
    List<ChatRoom> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ChatRoom c " +
            "WHERE (c.user1Id = :userId1 AND c.user2Id = :userId2) OR (c.user1Id = :userId2 AND c.user2Id = :userId1)")
    boolean existsByUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
