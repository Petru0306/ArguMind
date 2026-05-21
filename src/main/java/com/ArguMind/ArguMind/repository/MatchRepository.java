package com.ArguMind.ArguMind.repository;

import com.ArguMind.ArguMind.model.Match;
import com.ArguMind.ArguMind.model.GameMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStatus(String status);
    Optional<Match> findFirstByTopicAndStatus(String topic, String status);

    @Query(value = """
            SELECT * FROM matches
            WHERE status IN ('PENDING', 'ACTIVE')
              AND (pro_user_id = :userId OR contra_user_id = :userId
                   OR pro_user2_id = :userId OR contra_user2_id = :userId)
            ORDER BY id DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Match> findOngoingMatchForUser(@Param("userId") Long userId);

    Optional<Match> findFirstByStatusOrderByIdAsc(String status);

    boolean existsByJoinCode(String joinCode);

    Optional<Match> findByJoinCodeIgnoreCaseAndStatus(String joinCode, String status);

    @Query("""
            SELECT m FROM Match m
            WHERE m.status = 'PENDING'
              AND m.contraUser IS NULL
              AND m.proUser.id <> :userId
              AND m.gameMode = :gameMode
            ORDER BY m.id ASC
            """)
    Optional<Match> findJoinablePendingMatch(@Param("userId") Long userId, @Param("gameMode") GameMode gameMode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM Match m WHERE m.status = 'PENDING' AND m.createdAt IS NOT NULL AND m.createdAt < :cutoff")
    int deleteStalePendingMatches(@Param("cutoff") Instant cutoff);

    long countByWinnerId(Long userId);
    long countByStatusAndProUserIdOrStatusAndContraUserId(String status1, Long proUserId, String status2, Long contraUserId);
    
    @org.springframework.data.jpa.repository.Query("SELECT m FROM Match m WHERE (m.proUser.id = :userId OR m.contraUser.id = :userId) AND m.status = 'FINISHED' ORDER BY m.id DESC")
    List<Match> findRecentFinishedMatchesByUserId(Long userId, org.springframework.data.domain.Pageable pageable);
}
