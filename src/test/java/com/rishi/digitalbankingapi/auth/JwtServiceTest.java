package com.rishi.digitalbankingapi.auth;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Both secrets must be >=64 bytes (512 bits) so jjwt's auto-selected HMAC
    // algorithm (HS512) is satisfied by either key; otherwise a
    // WeakKeyException masks the SignatureException this test is after.
    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-signing-1234567890!!";

    @Test
    void generatedTokenRoundTripsUsername() {
        JwtService jwtService = new JwtService(SECRET, 60_000L);

        String token = jwtService.generateToken("alice", "USER");

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.isTokenValid(token, "alice")).isTrue();
    }

    @Test
    void tokenIsInvalidForADifferentUsername() {
        JwtService jwtService = new JwtService(SECRET, 60_000L);

        String token = jwtService.generateToken("alice", "USER");

        assertThat(jwtService.isTokenValid(token, "bob")).isFalse();
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        JwtService jwtService = new JwtService(SECRET, 1L);

        String token = jwtService.generateToken("alice", "USER");
        Thread.sleep(10);

        assertThat(jwtService.isTokenValid(token, "alice")).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        JwtService signer = new JwtService(SECRET, 60_000L);
        JwtService verifier = new JwtService("a-completely-different-secret-key-that-is-also-long-enough-here!!", 60_000L);

        String token = signer.generateToken("alice", "USER");

        assertThatThrownBy(() -> verifier.extractUsername(token))
                .isInstanceOf(SignatureException.class);
    }
}
