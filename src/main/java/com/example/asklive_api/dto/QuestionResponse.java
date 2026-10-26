package com.example.asklive_api.dto;

import java.time.Instant;

public record QuestionResponse(Long id, String text, String authorName, long upvotes, Instant createdAt) {
}
