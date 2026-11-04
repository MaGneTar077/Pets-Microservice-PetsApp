package com.MyAnimaLog.Pets.infrastructure.config;

import com.MyAnimaLog.Pets.infrastructure.security.InternalApiKeyFilter;
import com.MyAnimaLog.Pets.infrastructure.security.JwtClaimsConverter;
import com.MyAnimaLog.Pets.infrastructure.security.JwtTokenValidator;
import com.MyAnimaLog.Pets.infrastructure.security.RestAccessDeniedHandler;
import com.MyAnimaLog.Pets.infrastructure.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.security.internal-api-key:}") String internalApiKey) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**",
                                "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/internal/**").hasAuthority("INTERNAL_SERVICE")
                        .anyRequest().authenticated())
                .addFilterBefore(new InternalApiKeyFilter(internalApiKey, restAuthenticationEntryPoint),
                        BearerTokenAuthenticationFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtClaimsConverter()))
                        .authenticationEntryPoint(restAuthenticationEntryPoint));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${app.security.jwks-uri:}") String jwkSetUri,
            @Value("${app.security.issuer}") String issuer,
            @Value("${app.security.audience}") String audience) {
        if (jwkSetUri.isBlank()) {
            throw new IllegalStateException(
                    "JWT_JWKS_URI no está configurada. El servicio verifica los tokens RS256 del User "
                            + "service contra su JWKS y no puede arrancar sin esa URL.");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtTokenValidator.of(issuer, audience));
        return decoder;
    }
}
