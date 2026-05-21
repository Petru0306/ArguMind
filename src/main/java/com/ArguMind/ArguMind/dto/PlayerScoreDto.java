package com.ArguMind.ArguMind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerScoreDto {
    private Integer logic;
    private Integer clarity;
    private Integer evidence;
    private Integer rhetoric;
    private Integer total;
}
