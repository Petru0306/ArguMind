package com.ArguMind.ArguMind.service;

import org.springframework.stereotype.Service;

@Service
public class RankService {

    public String titleForElo(int elo) {
        if (elo >= 1600) return "GRANDMASTER";
        if (elo >= 1450) return "MASTER";
        if (elo >= 1300) return "EXPERT";
        if (elo >= 1150) return "DEBATER";
        if (elo >= 1050) return "APPRENTICE";
        return "NOVICE";
    }
}
