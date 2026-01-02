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

    @Query("SELECT m FROM PetMateMatch m WHERE m.toUserId = :userId AND m.status = 'PENDING'")
    List<PetMateMatch> findPendingLikesForUser(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM PetMateMatch m " +
            "WHERE m.fromUserId = :fromUserId AND m.toUserId = :toUserId")
    boolean existsByFromUserIdAndToUserId(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

    @Query("SELECT COUNT(m) FROM PetMateMatch m WHERE (m.fromUserId = :userId OR m.toUserId = :userId) AND m.status = 'MATCHED'")
    Long countMatchesByUserId(@Param("userId") Long userId);

    List<PetMateMatch> findByFromUserId(Long fromUserId);

    @Query("SELECT m FROM PetMateMatch m WHERE m.fromUserId = :userId AND m.status = 'PENDING'")
    List<PetMateMatch> findSentPendingRequests(@Param("userId") Long userId);

    // 두 유저가 매칭되었는지 확인
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM PetMateMatch m " +
            "WHERE ((m.fromUserId = :userId1 AND m.toUserId = :userId2) OR (m.fromUserId = :userId2 AND m.toUserId = :userId1)) "
            +
            "AND m.status = 'MATCHED'")
    boolean isMatched(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
