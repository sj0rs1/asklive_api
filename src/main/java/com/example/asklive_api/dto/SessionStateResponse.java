package com.example.asklive_api.dto;

import com.example.asklive_api.domain.SessionStatus;

import java.util.List;

public record SessionStateResponse(String code, SessionStatus status, PollResponse poll,
        List<QuestionResponse> questions) {
}
