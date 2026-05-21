package com.ArguMind.ArguMind.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "join_code", unique = true, length = 8)
    private String joinCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void ensureCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "game_mode")
    @Builder.Default
    private GameMode gameMode = GameMode.STANDARD;

    @ManyToOne
    @JoinColumn(name = "pro_user_id")
    private User proUser;

    @ManyToOne
    @JoinColumn(name = "contra_user_id")
    private User contraUser;

    @ManyToOne
    @JoinColumn(name = "pro_user2_id")
    private User proUser2;

    @ManyToOne
    @JoinColumn(name = "contra_user2_id")
    private User contraUser2;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    // Scoruri PRO
    private Integer proLogicScore;
    private Integer proClarityScore;
    private Integer proRhetoricScore;
    private Integer proEvidenceScore;

    // Scoruri CONTRA
    private Integer contraLogicScore;
    private Integer contraClarityScore;
    private Integer contraRhetoricScore;
    private Integer contraEvidenceScore;

    private Integer proEloChange;
    private Integer contraEloChange;

    @Column(columnDefinition = "TEXT")
    private String proFeedback;

    @Column(columnDefinition = "TEXT")
    private String contraFeedback;
}
