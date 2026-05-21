package com.ArguMind.ArguMind.model;

public enum GameMode {
    BLITZ(120),
    RAPID(300),
    STANDARD(600),
    EXTENDED(900),
    TEAMS_2V2(420);

    private final int turnTimeSeconds;

    GameMode(int turnTimeSeconds) {
        this.turnTimeSeconds = turnTimeSeconds;
    }

    public int getTurnTimeSeconds() {
        return turnTimeSeconds;
    }

    public int getRequiredPlayers() {
        return this == TEAMS_2V2 ? 4 : 2;
    }

    public int getTotalArguments() {
        return this == TEAMS_2V2 ? 4 : 4;
    }
}
