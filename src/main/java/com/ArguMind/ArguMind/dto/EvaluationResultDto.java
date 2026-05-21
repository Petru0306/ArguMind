package com.ArguMind.ArguMind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResultDto {
    private PlayerScoreDto proScores;
    private PlayerScoreDto contraScores;
    private List<FallacyDto> fallacies;
    private String winner; // "PRO" or "CONTRA"
}
