package com.ArguMind.ArguMind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameEventDto {
    public enum EventType {
        START, TYPING, SUBMIT, TURN_CHANGE, PROCESSING_AI, FINISHED
    }

    private EventType type;
    private String senderUsername;
    private Object payload;
}
