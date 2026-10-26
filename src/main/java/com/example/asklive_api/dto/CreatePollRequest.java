package com.example.asklive_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePollRequest(@NotBlank @Size(max = 255) String question, Boolean multipleChoice,
        @NotEmpty @Size(min = 2) List<@NotBlank @Size(max = 100) String> options) {
}
