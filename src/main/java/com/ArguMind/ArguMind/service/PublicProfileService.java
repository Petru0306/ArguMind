package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.PublicProfileDto;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.MatchRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicProfileService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public PublicProfileDto getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Jucătorul nu există."));

        long wins = matchRepository.countByWinnerId(user.getId());
        long totalFinished = matchRepository.countByStatusAndProUserIdOrStatusAndContraUserId(
                "FINISHED", user.getId(), "FINISHED", user.getId());

        return PublicProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .eloRating(user.getEloRating())
                .rankTitle(user.getRankTitle())
                .wins(wins)
                .losses(Math.max(0, totalFinished - wins))
                .totalMatches(totalFinished)
                .recentMatches(matchRepository.findRecentFinishedMatchesByUserId(
                        user.getId(), PageRequest.of(0, 8)))
                .build();
    }
}
