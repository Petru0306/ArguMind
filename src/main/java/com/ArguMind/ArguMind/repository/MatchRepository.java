package com.ArguMind.ArguMind.repository;

import com.ArguMind.ArguMind.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStatus(String status);
    Optional<Match> findFirstByTopicAndStatus(String topic, String status);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM matches WHERE (pro_user_id = :userId OR contra_user_id = :userId) AND status IN ('PENDING', 'ACTIVE') ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<Match> findOngoingMatchForUser(Long userId);

    Optional<Match> findFirstByStatusOrderByIdAsc(String status);

    long countByWinnerId(Long userId);
    long countByStatusAndProUserIdOrStatusAndContraUserId(String status1, Long proUserId, String status2, Long contraUserId);
    
    @org.springframework.data.jpa.repository.Query("SELECT m FROM Match m WHERE (m.proUser.id = :userId OR m.contraUser.id = :userId) AND m.status = 'FINISHED' ORDER BY m.id DESC")
    List<Match> findRecentFinishedMatchesByUserId(Long userId, org.springframework.data.domain.Pageable pageable);
}
