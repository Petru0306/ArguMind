package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.MatchResponseDto;
import com.ArguMind.ArguMind.dto.ProfileUpdateDto;
import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.exception.EmailAlreadyExistsException;
import com.ArguMind.ArguMind.exception.UsernameAlreadyExistsException;
import com.ArguMind.ArguMind.model.Match;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.DebateTopicRepository;
import com.ArguMind.ArguMind.repository.FallacyGuideRepository;
import com.ArguMind.ArguMind.repository.LogicalFallacyRepository;
import com.ArguMind.ArguMind.repository.MatchRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import com.ArguMind.ArguMind.service.LeaderboardService;
import com.ArguMind.ArguMind.service.MatchService;
import com.ArguMind.ArguMind.model.AiCoachPersonality;
import com.ArguMind.ArguMind.service.FriendService;
import com.ArguMind.ArguMind.service.PublicProfileService;
import com.ArguMind.ArguMind.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserService userService;
    private final MatchService matchService;
    private final LeaderboardService leaderboardService;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final LogicalFallacyRepository logicalFallacyRepository;
    private final DebateTopicRepository debateTopicRepository;
    private final FallacyGuideRepository fallacyGuideRepository;
    private final PublicProfileService publicProfileService;
    private final FriendService friendService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("topicCount", debateTopicRepository.count());
        model.addAttribute("featuredTopics", debateTopicRepository.findByIsActiveTrue().stream().limit(6).toList());
        return "index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/fallacies")
    public String fallacies(Model model) {
        model.addAttribute("fallacyGuides", fallacyGuideRepository.findAllByOrderByCategoryAscNameAsc());
        return "fallacies";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(Model model) {
        model.addAttribute("entries", leaderboardService.getLeaderboard());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            userRepository.findByUsername(auth.getName()).ifPresent(u ->
                    model.addAttribute("currentUserId", u.getId()));
        }
        return "leaderboard";
    }

    @GetMapping("/topics")
    public String topics(@RequestParam(required = false) String category, Model model) {
        Locale locale = LocaleContextHolder.getLocale();
        var all = debateTopicRepository.findByIsActiveTrue();
        model.addAttribute("topics", all);
        model.addAttribute("categories", all.stream()
                .map(t -> t.getDisplayCategory(locale))
                .distinct()
                .sorted()
                .toList());
        model.addAttribute("selectedCategory", category);
        if (category != null && !category.isBlank()) {
            model.addAttribute("topics", all.stream()
                    .filter(t -> category.equalsIgnoreCase(t.getCategory())
                            || category.equalsIgnoreCase(t.getCategoryEn()))
                    .toList());
        }
        return "topics";
    }

    @GetMapping("/player/{username}")
    public String publicPlayer(@PathVariable String username, Model model) {
        try {
            var profile = publicProfileService.getPublicProfile(username);
            model.addAttribute("profile", profile);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long viewerId = null;
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                viewerId = userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null);
                model.addAttribute("currentUserId", viewerId);
            }
            if (viewerId != null) {
                FriendService.FriendRelation rel = friendService.getRelation(viewerId, profile.getId());
                model.addAttribute("friendRelation", rel.name());
                friendService.findFriendshipBetween(viewerId, profile.getId()).ifPresent(f -> {
                    if (rel == FriendService.FriendRelation.PENDING_IN) {
                        model.addAttribute("incomingFriendshipId", f.getId());
                    }
                });
            } else {
                model.addAttribute("friendRelation", "NONE");
            }
            return "player";
        } catch (IllegalArgumentException e) {
            return "redirect:/leaderboard";
        }
    }

    @GetMapping("/ai-debate")
    public String aiDebate(@RequestParam(required = false) String topic, Model model) {
        model.addAttribute("debateTopics", debateTopicRepository.findByIsActiveTrue());
        model.addAttribute("personalities", AiCoachPersonality.values());
        if (topic != null && !topic.isBlank()) {
            model.addAttribute("selectedTopic", topic);
        }
        return "ai-debate";
    }

    @GetMapping("/lessons")
    public String lessons() {
        return "lessons";
    }

    @GetMapping("/lessons/argumentare")
    public String lessonArgumentation() {
        return localizedLessonView("lesson-argumentare");
    }

    @GetMapping("/lessons/replica")
    public String lessonReplica() {
        return localizedLessonView("lesson-replica");
    }

    @GetMapping("/lessons/anticipare")
    public String lessonAnticipare() {
        return localizedLessonView("lesson-anticipare");
    }

    @GetMapping("/lessons/dovezi")
    public String lessonDovezi() {
        return localizedLessonView("lesson-dovezi");
    }

    @GetMapping("/lessons/retorica")
    public String lessonRetorica() {
        return localizedLessonView("lesson-retorica");
    }

    private String localizedLessonView(String base) {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale != null && "en".equals(locale.getLanguage())) {
            return base + "-en";
        }
        return base;
    }

    @GetMapping("/modes/{mode}")
    public String modeShortcut(@PathVariable String mode) {
        String m = mode.toUpperCase().replace("-", "_");
        if (!List.of("BLITZ", "RAPID", "STANDARD", "EXTENDED", "TEAMS_2V2").contains(m)) {
            return "redirect:/dashboard";
        }
        return "redirect:/dashboard?gameMode=" + m;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute UserRegistrationDto registrationDto,
                               RedirectAttributes redirectAttributes) {
        try {
            validateRegistration(registrationDto);
            userService.registerUser(registrationDto);
            return "redirect:/login?success";
        } catch (UsernameAlreadyExistsException e) {
            redirectAttributes.addFlashAttribute("errorKey", "error.user.exists");
            return "redirect:/register";
        } catch (EmailAlreadyExistsException e) {
            redirectAttributes.addFlashAttribute("errorKey", "error.email.exists");
            return "redirect:/register";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String gameMode,
                            @RequestParam(required = false) String topic,
                            Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        userRepository.findByUsername(auth.getName()).ifPresent(user -> populateUserStats(model, user));
        if (gameMode != null && List.of("BLITZ", "RAPID", "STANDARD", "EXTENDED", "TEAMS_2V2")
                .contains(gameMode.toUpperCase())) {
            model.addAttribute("selectedGameMode", gameMode.toUpperCase());
        }
        if (topic != null && !topic.isBlank()) {
            model.addAttribute("selectedTopic", topic);
        }
        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        populateUserStats(model, user);
        model.addAttribute("profileUser", user);
        model.addAttribute("friends", friendService.listFriends(user.getId()));
        model.addAttribute("friendRequests", friendService.listPendingIncoming(user.getId()));
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute ProfileUpdateDto profileUpdateDto,
                                RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        try {
            userService.updateProfile(user.getId(), profileUpdateDto);
            redirectAttributes.addFlashAttribute("successKey", "profile.update.success");
        } catch (EmailAlreadyExistsException e) {
            redirectAttributes.addFlashAttribute("errorKey", "error.email.exists");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/arena/{matchId}")
    public String arena(@PathVariable Long matchId, Model model) {
        MatchResponseDto match = matchService.getMatchById(matchId);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Long userId = userRepository.findByUsername(auth.getName())
                .map(User::getId)
                .orElse(null);

        if (userId != null) {
            matchService.verifyParticipant(matchId, userId);
        }

        if ("FINISHED".equals(match.getStatus())) {
            return "redirect:/arena/" + matchId + "/results";
        }

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
            matchService.verifyParticipant(matchId, user.getId());
        });

        boolean aiReady = match.getProLogicScore() != null && match.getContraLogicScore() != null;
        boolean aiPending = "PROCESSING_AI".equals(match.getStatus())
                || ("FINISHED".equals(match.getStatus()) && !aiReady);

        model.addAttribute("match", match);
        model.addAttribute("currentUsername", auth.getName());
        model.addAttribute("fallacies", logicalFallacyRepository.findByMatchId(matchId));
        model.addAttribute("aiPending", aiPending);
        model.addAttribute("aiFailed", match.getProFeedback() != null
                && (match.getProFeedback().contains("Eroare tehnică") || match.getProFeedback().contains("Limită API")));
        return "results";
    }

    private void populateUserStats(Model model, User user) {
        model.addAttribute("currentUserId", user.getId());
        model.addAttribute("currentUserElo", user.getEloRating());
        model.addAttribute("currentUserRank", user.getRankTitle());
        model.addAttribute("currentUserEmail", user.getEmail());

        long wins = matchRepository.countByWinnerId(user.getId());
        long totalFinished = matchRepository.countByStatusAndProUserIdOrStatusAndContraUserId(
                "FINISHED", user.getId(), "FINISHED", user.getId());
        model.addAttribute("wins", wins);
        model.addAttribute("losses", totalFinished - wins);

        List<Match> finishedMatches = matchRepository.findRecentFinishedMatchesByUserId(
                user.getId(), PageRequest.of(0, 10));

        double avgLogic = finishedMatches.stream()
                .map(m -> m.getProUser().getId().equals(user.getId()) ? m.getProLogicScore() : m.getContraLogicScore())
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0);

        model.addAttribute("logicAccuracy", avgLogic > 0 ? String.format("%.1f/10", avgLogic) : "N/A");
        model.addAttribute("recentMatches", finishedMatches);
    }

    private void validateRegistration(UserRegistrationDto dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new IllegalArgumentException("Completează username-ul.");
        }
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Completează adresa de email.");
        }
        if (dto.getPassword() == null || dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("Parola trebuie să aibă minim 8 caractere.");
        }
        if (dto.getConfirmPassword() == null || !dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Parolele nu coincid.");
        }
    }
}
