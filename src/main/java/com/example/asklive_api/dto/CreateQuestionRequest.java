package com.example.asklive_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(@NotBlank @Size(max = 500) String text) {
}
