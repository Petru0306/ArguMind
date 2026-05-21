package com.ArguMind.ArguMind.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fallacy_guides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FallacyGuide {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String example;

    @Column(columnDefinition = "TEXT")
    private String howToAvoid;

    @Column(name = "category_en")
    private String categoryEn;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "example_en", columnDefinition = "TEXT")
    private String exampleEn;

    @Column(name = "how_to_avoid_en", columnDefinition = "TEXT")
    private String howToAvoidEn;

    public String getDisplayCategory(java.util.Locale locale) {
        if (locale != null && "en".equals(locale.getLanguage()) && categoryEn != null && !categoryEn.isBlank()) {
            return categoryEn;
        }
        return category;
    }

    public String getDisplayDescription(java.util.Locale locale) {
        if (locale != null && "en".equals(locale.getLanguage()) && descriptionEn != null && !descriptionEn.isBlank()) {
            return descriptionEn;
        }
        return description;
    }

    public String getDisplayExample(java.util.Locale locale) {
        if (locale != null && "en".equals(locale.getLanguage()) && exampleEn != null && !exampleEn.isBlank()) {
            return exampleEn;
        }
        return example;
    }

    public String getDisplayHowToAvoid(java.util.Locale locale) {
        if (locale != null && "en".equals(locale.getLanguage()) && howToAvoidEn != null && !howToAvoidEn.isBlank()) {
            return howToAvoidEn;
        }
        return howToAvoid;
    }
}
