package com.MyAnimaLog.Pets.infrastructure.adapters;

import com.MyAnimaLog.Pets.application.dto.AuthenticatedUser;
import com.MyAnimaLog.Pets.application.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthenticatedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SpringAuthenticatedUserAdapter implements AuthenticatedUserPort {

    @Override
    public AuthenticatedUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new UnauthenticatedException();
        }

        Jwt jwt = jwtAuthentication.getToken();
        return new AuthenticatedUser(
                parseSubject(jwt.getSubject()),
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
    }

    private UUID parseSubject(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new UnauthenticatedException("Token subject is not a valid user id");
        }
    }
}
