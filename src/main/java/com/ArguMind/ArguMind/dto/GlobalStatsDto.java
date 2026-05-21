package com.ArguMind.ArguMind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalStatsDto {
    private long totalUsers;
    private long totalMatches;
    private Map<String, Long> matchesByStatus;
    private Map<String, Long> matchesByGameMode;
}
