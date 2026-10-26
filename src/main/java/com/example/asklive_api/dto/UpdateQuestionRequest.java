package com.example.asklive_api.dto;

import com.example.asklive_api.domain.QuestionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionRequest(@NotNull QuestionStatus status) {
}
