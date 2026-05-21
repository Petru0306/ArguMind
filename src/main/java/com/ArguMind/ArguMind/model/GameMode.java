package com.ArguMind.ArguMind.model;

public enum GameMode {
    BLITZ(120), // 2 minute
    RAPID(300), // 5 minute
    STANDARD(600); // 10 minute

    private final int turnTimeSeconds;

    GameMode(int turnTimeSeconds) {
        this.turnTimeSeconds = turnTimeSeconds;
    }

    public int getTurnTimeSeconds() {
        return turnTimeSeconds;
    }
}
