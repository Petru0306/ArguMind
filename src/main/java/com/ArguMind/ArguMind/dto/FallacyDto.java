package com.ArguMind.ArguMind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FallacyDto {
    private String player; // "PRO" or "CONTRA"
    private Integer round;
    private String fallacyName;
    private String offendingText;
    private String explanation;
}
