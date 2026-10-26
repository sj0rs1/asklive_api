package com.example.asklive_api.dto;

import jakarta.validation.constraints.Size;

public record JoinRequest(@Size(max = 50) String displayName) {
}
