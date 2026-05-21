package com.ArguMind.ArguMind.repository;

import com.ArguMind.ArguMind.model.DebateTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DebateTopicRepository extends JpaRepository<DebateTopic, Long> {
    List<DebateTopic> findByIsActiveTrue();
    Optional<DebateTopic> findByTitle(String title);
}
