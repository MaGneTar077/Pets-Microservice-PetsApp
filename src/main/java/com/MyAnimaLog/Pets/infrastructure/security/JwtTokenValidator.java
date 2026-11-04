package com.MyAnimaLog.Pets.infrastructure.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.util.List;

/**
 * Construye el validador de {@code iss}/{@code aud} que usa el {@code JwtDecoder} del resource
 * server (CONTRATOS_COMPARTIDOS.md §1.1). Separado de {@code SecurityConfig} para poder probarlo
 * sin un {@code JwtDecoder} real, que necesitaría red para bajar el JWKS.
 */
public final class JwtTokenValidator {

    private JwtTokenValidator() {
    }

    public static OAuth2TokenValidator<Jwt> of(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(audience));
        return new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);
    }
}
