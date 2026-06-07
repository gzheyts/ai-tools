package com.example.demo.dto;

import java.time.Instant;
import java.util.UUID;

public record PersonResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    Instant createdAt
) {}
