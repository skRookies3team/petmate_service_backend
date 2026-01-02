package com.example.petlog.repository;

import com.example.petlog.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 1. 특정 두 유저 간의 채팅방 조회 (user1 -> user2 순서)
    Optional<ChatRoom> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    // 2. 특정 두 유저 간의 채팅방 조회 (user2 -> user1 순서)
    Optional<ChatRoom> findByUser2IdAndUser1Id(Long user2Id, Long user1Id);

    // 3. 내 채팅방 목록 조회 (내가 user1이거나 user2인 경우 모두 조회)
    // SQL: SELECT * FROM chat_room WHERE user1_id = ? OR user2_id = ?
    List<ChatRoom> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);

    // 4. 활성화된 방만 찾고 싶다면 아래처럼 사용 (Service에서 사용 시)
    List<ChatRoom> findByUser1IdOrUser2IdAndIsActiveTrue(Long user1Id, Long user2Id);
}