package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.LeaderboardEntryDto;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.MatchRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardEntryDto> getLeaderboard() {
        List<User> users = userRepository.findTop100ByOrderByEloRatingDesc();
        List<LeaderboardEntryDto> entries = new ArrayList<>();
        int position = 1;
        for (User user : users) {
            entries.add(LeaderboardEntryDto.builder()
                    .position(position++)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .eloRating(user.getEloRating())
                    .rankTitle(user.getRankTitle())
                    .wins(matchRepository.countByWinnerId(user.getId()))
                    .build());
        }
        return entries;
    }
}
