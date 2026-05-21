package com.ArguMind.ArguMind.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatRequestDto {
    private String topic;
    private String message;
    private String personality;
    private List<AiChatMessageDto> history = new ArrayList<>();
}
