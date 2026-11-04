package com.MyAnimaLog.Pets.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Autentica las peticiones a {@code /internal/**} con el header {@code X-Internal-Api-Key}
 * (CONTRATOS_COMPARTIDOS.md §2) en lugar de un JWT de usuario. Para cualquier otra ruta no hace
 * nada y deja que el resource server resuelva la autenticación como siempre.
 *
 * <p>No es un {@code @Component}: se instancia en {@code SecurityConfig} para que Spring Boot no
 * lo registre además como filtro de servlet, lo que lo ejecutaría fuera de la cadena de seguridad.
 */
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Api-Key";
    private static final String INTERNAL_SERVICE_AUTHORITY = "INTERNAL_SERVICE";

    private final String internalApiKey;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public InternalApiKeyFilter(String internalApiKey, AuthenticationEntryPoint authenticationEntryPoint) {
        // Copiar el secreto desde Secret Manager arrastra espacios o un salto de línea con facilidad.
        String configuredKey = internalApiKey == null ? "" : internalApiKey.trim();
        if (configuredKey.isEmpty()) {
            throw new IllegalStateException(
                    "INTERNAL_API_KEY no está configurada o está en blanco. Con la clave vacía un header "
                            + "vacío coincidiría y /internal/** quedaría abierto a cualquiera.");
        }
        this.internalApiKey = configuredKey;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (provided == null || !constantTimeEquals(provided, internalApiKey)) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Missing or invalid " + HEADER));
            return;
        }

        PreAuthenticatedAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(
                "internal-service", null, List.of(new SimpleGrantedAuthority(INTERNAL_SERVICE_AUTHORITY)));
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String provided, String expected) {
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
