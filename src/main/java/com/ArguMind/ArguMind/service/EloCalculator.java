package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.EloResultDto;
import org.springframework.stereotype.Service;

/**
 * Serviciu care implementează algoritmul de rating ELO.
 * Acesta calculează probabilitatea de câștig bazată pe diferența de scor
 * și actualizează rating-ul folosind factorul de magnitudine K.
 */
@Service
public class EloCalculator {

    private static final int K_FACTOR = 32;

    /**
     * Calculează noile scoruri ELO pentru ambii jucători.
     * 
     * @param proRating Scorul curent al jucătorului PRO
     * @param contraRating Scorul curent al jucătorului CONTRA
     * @param winner Rezultatul meciului ("PRO", "CONTRA" sau "DRAW")
     * @return Un obiect EloResultDto cu noile scoruri calculate
     */
    public EloResultDto calculateElo(int proRating, int contraRating, String winner) {
        
        // 1. Calculăm scorul așteptat (probabilitatea de câștig) pentru ambii jucători
        double expectedScorePro = calculateExpectedScore(proRating, contraRating);
        double expectedScoreContra = calculateExpectedScore(contraRating, proRating);

        // 2. Determinăm scorul real (S)
        double actualScorePro;
        double actualScoreContra;

        if ("PRO".equals(winner)) {
            actualScorePro = 1.0;
            actualScoreContra = 0.0;
        } else if ("CONTRA".equals(winner)) {
            actualScorePro = 0.0;
            actualScoreContra = 1.0;
        } else {
            // Egalitate
            actualScorePro = 0.5;
            actualScoreContra = 0.5;
        }

        // 3. Aplicăm formula de actualizare: R' = R + K * (S - E)
        int newProRating = (int) Math.round(proRating + K_FACTOR * (actualScorePro - expectedScorePro));
        int newContraRating = (int) Math.round(contraRating + K_FACTOR * (actualScoreContra - expectedScoreContra));

        return EloResultDto.builder()
                .newProRating(newProRating)
                .newContraRating(newContraRating)
                .build();
    }

    private double calculateExpectedScore(int playerRating, int opponentRating) {
        return 1.0 / (1.0 + Math.pow(10.0, (double) (opponentRating - playerRating) / 400.0));
    }
}
