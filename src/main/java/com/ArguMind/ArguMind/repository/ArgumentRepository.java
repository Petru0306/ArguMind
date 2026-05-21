package com.ArguMind.ArguMind.repository;

import com.ArguMind.ArguMind.model.Argument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArgumentRepository extends JpaRepository<Argument, Long> {
    List<Argument> findByMatchIdOrderByRoundNumberAsc(Long matchId);
}
