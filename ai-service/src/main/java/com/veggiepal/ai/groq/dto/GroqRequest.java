package com.veggiepal.ai.groq.dto;

import java.util.List;

public record GroqRequest(
        String model,
        List<Message> messages,
        double temperature,
        int max_completion_tokens
) {
    public record Message(
            String role,
            String content
    ) {}
}