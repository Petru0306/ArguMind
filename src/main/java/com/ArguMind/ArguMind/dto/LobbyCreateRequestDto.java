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
public class LobbyCreateRequestDto {
    private String topic;
    private GameMode gameMode;
}
