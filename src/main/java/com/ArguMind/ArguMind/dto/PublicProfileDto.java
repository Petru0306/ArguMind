package com.ArguMind.ArguMind.dto;

import com.ArguMind.ArguMind.model.Match;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PublicProfileDto {
    private Long id;
    private String username;
    private Integer eloRating;
    private String rankTitle;
    private long wins;
    private long losses;
    private long totalMatches;
    private List<Match> recentMatches;
}
