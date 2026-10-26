package com.example.asklive_api.dto;

import java.time.Instant;

import com.example.asklive_api.domain.QuestionStatus;

public record HostQuestionResponse(Long id, String text, String authorName, long upvotes, Instant createdAt,
        QuestionStatus status) {
}
