package com.ArguMind.ArguMind.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "debate_topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebateTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;
}
