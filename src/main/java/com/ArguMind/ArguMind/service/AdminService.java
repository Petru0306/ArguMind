package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.GlobalStatsDto;
import com.ArguMind.ArguMind.model.DebateTopic;
import com.ArguMind.ArguMind.model.Match;
import com.ArguMind.ArguMind.repository.DebateTopicRepository;
import com.ArguMind.ArguMind.repository.MatchRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final DebateTopicRepository debateTopicRepository;

    @Transactional
    public DebateTopic addTopic(String title, String category) {
        DebateTopic topic = DebateTopic.builder()
                .title(title)
                .category(category)
                .build();
        return debateTopicRepository.save(topic);
    }

    @Transactional(readOnly = true)
    public GlobalStatsDto getGlobalStats() {
        List<Match> allMatches = matchRepository.findAll();

        return GlobalStatsDto.builder()
                .totalUsers(userRepository.count())
                .totalMatches(allMatches.size())
                .matchesByStatus(allMatches.stream()
                        .collect(Collectors.groupingBy(Match::getStatus, Collectors.counting())))
                .matchesByGameMode(allMatches.stream()
                        .collect(Collectors.groupingBy(m -> m.getGameMode().name(), Collectors.counting())))
                .build();
    }
}
