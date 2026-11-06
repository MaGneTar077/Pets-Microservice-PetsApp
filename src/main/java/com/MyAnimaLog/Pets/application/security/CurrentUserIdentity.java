package com.MyAnimaLog.Pets.application.security;

import com.MyAnimaLog.Pets.application.dto.AuthenticatedUser;
import com.MyAnimaLog.Pets.application.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Única fuente de identidad del servicio: el token.
 *
 * <p>Los {@code resolve*} implementan la regla de compatibilidad mientras el frontend migra
 * (PETS-02): si el cliente no manda el identificador se usa el del token; si lo manda igual se
 * acepta; si lo manda distinto es un 403. Así ningún servicio vuelve a comparar identidades a mano.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserIdentity {

    private final AuthenticatedUserPort authenticatedUserPort;

    public AuthenticatedUser current() {
        return authenticatedUserPort.current();
    }

    public UUID userId() {
        return current().userId();
    }

    public String email() {
        return current().email();
    }

    public boolean emailVerified() {
        return current().emailVerified();
    }

    /** @param fromRequest identificador que todavía llega por body, query o path; puede ser null. */
    public UUID resolveUserId(UUID fromRequest) {
        UUID tokenUserId = userId();
        if (fromRequest == null || fromRequest.equals(tokenUserId)) {
            return tokenUserId;
        }
        throw new IdentityMismatchException();
    }

    /** Los correos se comparan sin distinguir mayúsculas. */
    public String resolveEmail(String fromRequest) {
        String tokenEmail = email();
        if (fromRequest == null || fromRequest.isBlank()) {
            return tokenEmail;
        }
        if (tokenEmail != null && fromRequest.equalsIgnoreCase(tokenEmail)) {
            return tokenEmail;
        }
        throw new IdentityMismatchException();
    }
}
