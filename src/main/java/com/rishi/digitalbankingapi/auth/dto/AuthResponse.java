package com.rishi.digitalbankingapi.auth.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String role
) {
}
