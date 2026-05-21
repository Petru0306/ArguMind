package com.ArguMind.ArguMind.model;

public enum AiCoachPersonality {
    SERIOUS("Serios", "Ton academic, direct, focus pe logică și structură."),
    GENTLE("Blând", "Încurajator, răbdător, explică pas cu pas fără a intimida."),
    FUNNY("Amuzant", "Umor light, metafore creative, dar păstrează substanța argumentativă."),
    SOCRATIC("Socratic", "Pune întrebări ghidate; nu dă răspunsuri gata, ci te face să gândești."),
    COMPETITIVE("Competitiv", "Provocator, contra-argumente dure, ca un adversar de turneu."),
    DEVIL_ADVOCATE("Avocatul Diavolului", "Atacă orice poziție — te forțează să îți întărești argumentele.");

    private final String label;
    private final String description;

    AiCoachPersonality(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static AiCoachPersonality fromString(String value) {
        if (value == null || value.isBlank()) {
            return SERIOUS;
        }
        try {
            return AiCoachPersonality.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SERIOUS;
        }
    }
}
