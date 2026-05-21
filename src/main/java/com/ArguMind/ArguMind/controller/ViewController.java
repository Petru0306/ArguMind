package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.MatchResponseDto;
import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.model.Match;
import com.ArguMind.ArguMind.repository.LogicalFallacyRepository;
import com.ArguMind.ArguMind.repository.MatchRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import com.ArguMind.ArguMind.service.MatchService;
import com.ArguMind.ArguMind.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserService userService;
    private final MatchService matchService;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final LogicalFallacyRepository logicalFallacyRepository;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Mapare temporară pentru POST /login pentru a evita eroarea 405 
     * în timpul previzualizării cu securitatea dezactivată.
     */
    @PostMapping("/login")
    public String loginPost() {
        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute UserRegistrationDto registrationDto) {
        userService.registerUser(registrationDto);
        return "redirect:/login?success";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        userRepository.findByUsername(auth.getName()).ifPresent(user -> {
            model.addAttribute("currentUserId", user.getId());
            model.addAttribute("currentUserElo", user.getEloRating());
            model.addAttribute("currentUserRank", user.getRankTitle());

            // Statistici Reale
            long wins = matchRepository.countByWinnerId(user.getId());
            long totalFinished = matchRepository.countByStatusAndProUserIdOrStatusAndContraUserId("FINISHED", user.getId(), "FINISHED", user.getId());
            long losses = totalFinished - wins;

            model.addAttribute("wins", wins);
            model.addAttribute("losses", losses);

            // Calcul Acuratețe Logică (Media scorurilor de logică din meciurile finalizate)
            List<Match> finishedMatches = matchRepository.findRecentFinishedMatchesByUserId(user.getId(), 
                    org.springframework.data.domain.PageRequest.of(0, 10));
            
            double avgLogic = finishedMatches.stream()
                .map(m -> m.getProUser().getId().equals(user.getId()) ? m.getProLogicScore() : m.getContraLogicScore())
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0);
            
            model.addAttribute("logicAccuracy", avgLogic > 0 ? String.format("%.1f/10", avgLogic) : "N/A");
            model.addAttribute("recentMatches", finishedMatches);
        });
        return "dashboard";
    }

    @GetMapping("/arena/{matchId}")
    public String arena(@PathVariable Long matchId, Model model) {
        MatchResponseDto match = matchService.getMatchById(matchId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        Long userId = userRepository.findByUsername(auth.getName())
                .map(u -> u.getId())
                .orElse(null);
        
        model.addAttribute("match", match);
        model.addAttribute("currentUsername", auth.getName());
        model.addAttribute("currentUserId", userId);
        
        return "arena";
    }

    @GetMapping("/arena/{matchId}/results")
    public String results(@PathVariable Long matchId, Model model) {
        MatchResponseDto match = matchService.getMatchById(matchId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        userRepository.findByUsername(auth.getName()).ifPresent(user -> {
            model.addAttribute("currentUserId", user.getId());
        });

        model.addAttribute("match", match);
        model.addAttribute("currentUsername", auth.getName());
        
        // Preluăm erorile logice reale
        model.addAttribute("fallacies", logicalFallacyRepository.findByMatchId(matchId));
        
        return "results";
    }
}
