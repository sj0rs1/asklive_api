package com.example.asklive_api.dto;

import com.example.asklive_api.domain.PollStatus;

import java.util.List;

public record PollResponse(Long pollId, String question, boolean multipleChoice, PollStatus status,
        List<PollOptionResponse> options) {
}
