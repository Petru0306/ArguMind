package com.ArguMind.ArguMind;

import com.ArguMind.ArguMind.dto.EloResultDto;
import com.ArguMind.ArguMind.service.EloCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EloCalculatorTests {

    private final EloCalculator eloCalculator = new EloCalculator();

    @Test
    @DisplayName("Când jucători egali se înfruntă, variația trebuie să fie echilibrată (~16 puncte)")
    void testBalancedMatch() {
        EloResultDto result = eloCalculator.calculateElo(1000, 1000, "PRO");
        
        // Expected score for both is 0.5. 
        // newRating = 1000 + 32 * (1 - 0.5) = 1000 + 16 = 1016
        // newRatingContra = 1000 + 32 * (0 - 0.5) = 1000 - 16 = 984
        
        assertTrue(result.getNewProRating() > 1000);
        assertTrue(result.getNewContraRating() < 1000);
        assertTrue(result.getNewProRating() == 1016);
        assertTrue(result.getNewContraRating() == 984);
    }

    @Test
    @DisplayName("Când un underdog învinge un favorit, trebuie să primească multe puncte")
    void testUnderdogVictory() {
        // PRO (1000) vs CONTRA (2000)
        EloResultDto result = eloCalculator.calculateElo(1000, 2000, "PRO");
        
        int pointsGained = result.getNewProRating() - 1000;
        int pointsLost = 2000 - result.getNewContraRating();
        
        assertTrue(pointsGained > 16, "Underdog should gain more than standard points");
        assertTrue(pointsGained == 32, "In this extreme case, gained points should be max (K=32)");
        System.out.println("Underdog (1000) beat Favorit (2000). Points gained: " + pointsGained);
    }

    @Test
    @DisplayName("Când un favorit învinge un underdog, trebuie să primească foarte puține puncte")
    void testFavoriteVictory() {
        // PRO (2000) vs CONTRA (1000)
        EloResultDto result = eloCalculator.calculateElo(2000, 1000, "PRO");
        
        int pointsGained = result.getNewProRating() - 2000;
        
        assertTrue(pointsGained < 16, "Favorite should gain fewer than standard points");
        assertTrue(pointsGained == 0, "In this extreme case, gained points should be 0 (rounded)");
        System.out.println("Favorit (2000) beat Underdog (1000). Points gained: " + pointsGained);
    }

    @Test
    @DisplayName("Egalitatea trebuie să apropie scorurile")
    void testDraw() {
        // Un jucător mai slab reușește o egalitate cu unul mai bun
        EloResultDto result = eloCalculator.calculateElo(1000, 1500, "DRAW");
        
        assertTrue(result.getNewProRating() > 1000, "Underdog should gain points from a draw against strong opponent");
        assertTrue(result.getNewContraRating() < 1500, "Favorite should lose points from a draw against weak opponent");
        System.out.println("Draw 1000 vs 1500. New ratings: " + result.getNewProRating() + " vs " + result.getNewContraRating());
    }
}
