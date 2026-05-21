package com.ArguMind.ArguMind.model;

import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumn(name = "winner_id")
    private User winner;
}
