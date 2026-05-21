package com.ArguMind.ArguMind.repository;

import com.ArguMind.ArguMind.model.LogicalFallacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LogicalFallacyRepository extends JpaRepository<LogicalFallacy, Long> {
    List<LogicalFallacy> findByMatchId(Long matchId);
}
