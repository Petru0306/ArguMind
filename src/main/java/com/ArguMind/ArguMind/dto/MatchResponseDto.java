package com.ArguMind.ArguMind.dto;

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
    private Long proUserId;
    private Long contraUserId;
    private Long winnerId;
}
