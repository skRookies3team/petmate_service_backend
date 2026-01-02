package com.example.petlog.repository;

import com.example.petlog.entity.PetMate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetMateRepository extends JpaRepository<PetMate, Long> {

    // [추가] 유저 ID로 프로필 존재 여부 확인 (이게 없어서 에러가 났습니다)
    boolean existsByUserId(Long userId);

    Optional<PetMate> findFirstByUserIdOrderByIdAsc(Long userId);

    List<PetMate> findByIsActiveTrue();

    List<PetMate> findByIsOnlineTrue();

    @Query("SELECT p FROM PetMate p WHERE p.isActive = true AND p.userId != :userId")
    List<PetMate> findActivePetMatesExcludingUser(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM (" +
            "SELECT *, " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
            "cos(radians(longitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(latitude)))) AS distance " +
            "FROM pet_mates " +
            "WHERE is_active = true AND user_id != :userId" +
            ") AS subquery " +
            "WHERE distance < :radius " +
            "ORDER BY distance", nativeQuery = true)
    List<PetMate> findNearbyPetMates(
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("radius") Double radiusKm,
            @Param("userId") Long userId);

    List<PetMate> findByPetBreed(String petBreed);

    List<PetMate> findByUserGender(String userGender);
}