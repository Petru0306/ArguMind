package com.ArguMind.ArguMind.repository;

import com.ArguMind.ArguMind.model.FallacyGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FallacyGuideRepository extends JpaRepository<FallacyGuide, Long> {
    List<FallacyGuide> findAllByOrderByCategoryAscNameAsc();
    boolean existsByName(String name);
    Optional<FallacyGuide> findByName(String name);
}
