package com.example.petlog.repository;

import com.example.petlog.entity.PetMateMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetMateMatchRepository extends JpaRepository<PetMateMatch, Long> {

    Optional<PetMateMatch> findByFromUserIdAndToUserId(Long fromUserId, Long toUserId);

    @Query("SELECT m FROM PetMateMatch m WHERE m.fromUserId = :userId OR m.toUserId = :userId")
    List<PetMateMatch> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT m FROM PetMateMatch m WHERE (m.fromUserId = :userId OR m.toUserId = :userId) AND m.status = 'MATCHED'")
    List<PetMateMatch> findMatchedByUserId(@Param("userId") Long userId);

    // [중요] 나에게 온 대기 중인 요청 조회
    @Query("SELECT m FROM PetMateMatch m WHERE m.toUserId = :userId AND m.status = 'PENDING'")
    List<PetMateMatch> findPendingLikesForUser(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM PetMateMatch m " +
            "WHERE m.fromUserId = :fromUserId AND m.toUserId = :toUserId")
    boolean existsByFromUserIdAndToUserId(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    // [추가] 대기 중인 요청 수 카운트 (배지 알림용)
    @Query("SELECT COUNT(m) FROM PetMateMatch m WHERE m.toUserId = :userId AND m.status = 'PENDING'")
    Long countPendingRequests(@Param("userId") Long userId);

    List<PetMateMatch> findByFromUserId(Long fromUserId);
}