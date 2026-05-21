package com.ArguMind.ArguMind.dto;

import com.ArguMind.ArguMind.model.GameMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponseDto {
    private Long id;
    private String topic;
    private String status;
    private GameMode gameMode;
    private Integer initialTime; // Timpul în secunde conform modului
    private Long proUserId;
    private Long contraUserId;
    private Long winnerId;
    private String currentTurn; // "PRO" sau "CONTRA"
    private Integer roundNumber;
    
    private Integer proEloChange;
    private Integer contraEloChange;
    private String proFeedback;
    private String contraFeedback;

    // Scoruri persistate (adăugate pentru rezultate)
    private Integer proLogicScore;
    private Integer proClarityScore;
    private Integer proRhetoricScore;
    private Integer proEvidenceScore;

    private Integer contraLogicScore;
    private Integer contraClarityScore;
    private Integer contraRhetoricScore;
    private Integer contraEvidenceScore;
}
