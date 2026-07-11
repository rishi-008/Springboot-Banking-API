package com.rishi.digitalbankingapi.auth;

import com.rishi.digitalbankingapi.auth.dto.AuthResponse;
import com.rishi.digitalbankingapi.auth.dto.LoginRequest;
import com.rishi.digitalbankingapi.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
