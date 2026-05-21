package com.ArguMind.ArguMind.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MatchFinishedEvent {
    private final Long matchId;
}
