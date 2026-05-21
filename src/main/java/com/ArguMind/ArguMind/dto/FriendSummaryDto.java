package com.ArguMind.ArguMind.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FriendSummaryDto {
    private Long friendshipId;
    private Long userId;
    private String username;
    private Integer eloRating;
    private String rankTitle;
}
