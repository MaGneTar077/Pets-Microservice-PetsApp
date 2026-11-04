package com.MyAnimaLog.Pets.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JwtTokenValidatorTest {

    private static final String ISSUER = "myanimalog-user-service";
    private static final String AUDIENCE = "myanimalog-api";

    private final OAuth2TokenValidator<Jwt> validator = JwtTokenValidator.of(ISSUER, AUDIENCE);

    @Test
    void shouldAccept_whenIssuerAndAudienceMatch() {
        OAuth2TokenValidatorResult result = validator.validate(token(ISSUER, AUDIENCE, Instant.now().plusSeconds(600)));

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldReject_whenIssuerIsWrong() {
        OAuth2TokenValidatorResult result =
                validator.validate(token("otro-emisor", AUDIENCE, Instant.now().plusSeconds(600)));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void shouldReject_whenAudienceIsWrong() {
        OAuth2TokenValidatorResult result =
                validator.validate(token(ISSUER, "otra-audiencia", Instant.now().plusSeconds(600)));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void shouldReject_whenTokenIsExpired() {
        OAuth2TokenValidatorResult result = validator.validate(
                token(ISSUER, AUDIENCE, Instant.now().minus(5, ChronoUnit.MINUTES)));

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt token(String issuer, String audience, Instant expiresAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim(JwtClaimNames.ISS, issuer)
                .claim(JwtClaimNames.AUD, List.of(audience))
                .claim(JwtClaimNames.SUB, UUID.randomUUID().toString())
                .issuedAt(expiresAt.minus(15, ChronoUnit.MINUTES))
                .expiresAt(expiresAt)
                .build();
    }
}
