package com.MyAnimaLog.Pets.infrastructure.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

/**
 * Convierte el JWT en un {@link JwtAuthenticationToken} con el {@code sub} como nombre del
 * principal y <b>sin</b> autoridades: así ningún claim del token (por ejemplo un {@code scope})
 * puede otorgar {@code INTERNAL_SERVICE}, que solo concede {@link InternalApiKeyFilter}.
 * Las autoridades de rol de clínica (CONTRATOS_COMPARTIDOS.md §1.3) llegan en PETS-03.
 */
public class JwtClaimsConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        return new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject());
    }
}
