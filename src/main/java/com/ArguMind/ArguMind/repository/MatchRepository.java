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
}
