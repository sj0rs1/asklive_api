package com.example.asklive_api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record VoteRequest(@NotEmpty List<@NotNull Long> optionIds) {
}
