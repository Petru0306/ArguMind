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

    @Column(name = "title_en")
    private String titleEn;

    @Column(name = "category_en")
    private String categoryEn;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    public String getDisplayTitle(java.util.Locale locale) {
        if (locale != null && "en".equals(locale.getLanguage()) && titleEn != null && !titleEn.isBlank()) {
            return titleEn;
        }
        return title;
    }

    public String getDisplayCategory(java.util.Locale locale) {
        if (locale != null && "en".equals(locale.getLanguage()) && categoryEn != null && !categoryEn.isBlank()) {
            return categoryEn;
        }
        return category;
    }
}
