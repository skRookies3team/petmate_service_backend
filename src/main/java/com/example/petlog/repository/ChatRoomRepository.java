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

    // [핵심] 쿼리를 직접 명시하여 JPA가 메서드 이름을 해석하다 실패하는 것을 방지
    @Query("SELECT c FROM ChatRoom c WHERE c.user1Id = :user1Id AND c.user2Id = :user2Id")
    Optional<ChatRoom> findByUser1IdAndUser2Id(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT c FROM ChatRoom c WHERE c.user1Id = :user2Id AND c.user2Id = :user1Id")
    Optional<ChatRoom> findByUser2IdAndUser1Id(@Param("user2Id") Long user2Id, @Param("user1Id") Long user1Id);

    // 내 채팅방 목록 조회 (user1이거나 user2인 경우)
    @Query("SELECT c FROM ChatRoom c WHERE c.user1Id = :userId OR c.user2Id = :userId")
    List<ChatRoom> findMyChatRooms(@Param("userId") Long userId);
}