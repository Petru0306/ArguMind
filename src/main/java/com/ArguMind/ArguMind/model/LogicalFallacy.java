package com.ArguMind.ArguMind.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "logical_fallacies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogicalFallacy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private String player; // "PRO" or "CONTRA"

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "fallacy_name", nullable = false)
    private String fallacyName;

    @Column(name = "offending_text", columnDefinition = "TEXT")
    private String offendingText;

    @Column(columnDefinition = "TEXT")
    private String explanation;
}
