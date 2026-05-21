package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.ArgumentSubmitDto;
import com.ArguMind.ArguMind.dto.MatchResponseDto;
import com.ArguMind.ArguMind.dto.MatchmakingRequestDto;
import com.ArguMind.ArguMind.model.Argument;
import com.ArguMind.ArguMind.model.Match;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.ArgumentRepository;
import com.ArguMind.ArguMind.repository.MatchRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ArgumentRepository argumentRepository;
    private final AiJudgeService aiJudgeService;

    @Transactional
    public MatchResponseDto joinMatchmaking(MatchmakingRequestDto request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Caută un meci PENDING pe aceeași temă
        return matchRepository.findFirstByTopicAndStatus(request.getTopic(), "PENDING")
                .map(match -> {
                    // Dacă userul curent e deja proUser, nu poate intra tot el ca contraUser
                    if (match.getProUser().getId().equals(user.getId())) {
                        return mapToResponseDto(match);
                    }
                    match.setContraUser(user);
                    match.setStatus("ACTIVE");
                    return mapToResponseDto(matchRepository.save(match));
                })
                .orElseGet(() -> {
                    // Dacă nu există, creează unul nou
                    Match newMatch = Match.builder()
                            .topic(request.getTopic())
                            .proUser(user)
                            .status("PENDING")
                            .build();
                    return mapToResponseDto(matchRepository.save(newMatch));
                });
    }

    @Transactional
    public void submitArgument(Long matchId, ArgumentSubmitDto submitDto) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!"ACTIVE".equals(match.getStatus())) {
            throw new RuntimeException("Match is not active");
        }

        User user = userRepository.findById(submitDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verifică dacă userul face parte din meci
        boolean isPro = match.getProUser().getId().equals(user.getId());
        boolean isContra = match.getContraUser() != null && match.getContraUser().getId().equals(user.getId());

        if (!isPro && !isContra) {
            throw new RuntimeException("User is not part of this match");
        }

        // Determină rândul
        List<Argument> existingArguments = argumentRepository.findByMatchIdOrderByRoundNumberAsc(matchId);
        int argCount = existingArguments.size();

        // Runda 1: arg 0 (PRO), arg 1 (CONTRA)
        // Runda 2: arg 2 (PRO), arg 3 (CONTRA)
        boolean isProTurn = argCount % 2 == 0;

        if (isProTurn && !isPro) {
            throw new RuntimeException("It's PRO user's turn");
        }
        if (!isProTurn && !isContra) {
            throw new RuntimeException("It's CONTRA user's turn");
        }

        int roundNumber = (argCount / 2) + 1;

        Argument argument = Argument.builder()
                .match(match)
                .user(user)
                .textContent(submitDto.getTextContent())
                .roundNumber(roundNumber)
                .build();

        argumentRepository.save(argument);

        // Dacă s-au trimis 4 argumente (2 runde complete), trecem la PROCESSING_AI
        if (argCount + 1 >= 4) {
            match.setStatus("PROCESSING_AI");
            matchRepository.save(match);
            
            // Declanșăm evaluarea AI (momentan sincron pentru simplitate)
            aiJudgeService.evaluateMatch(matchId);
        }
    }

    @Transactional(readOnly = true)
    public MatchResponseDto getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return mapToResponseDto(match);
    }

    private MatchResponseDto mapToResponseDto(Match match) {
        return MatchResponseDto.builder()
                .id(match.getId())
                .topic(match.getTopic())
                .status(match.getStatus())
                .proUserId(match.getProUser().getId())
                .contraUserId(match.getContraUser() != null ? match.getContraUser().getId() : null)
                .winnerId(match.getWinner() != null ? match.getWinner().getId() : null)
                .build();
    }
}
