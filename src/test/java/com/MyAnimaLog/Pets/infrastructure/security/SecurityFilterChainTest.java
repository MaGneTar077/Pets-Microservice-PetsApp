package com.MyAnimaLog.Pets.infrastructure.security;

import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.dto.IsOwnerResponse;
import com.MyAnimaLog.Pets.application.ports.in.GetPetsBatchUseCase;
import com.MyAnimaLog.Pets.application.ports.in.IsOwnerUseCase;
import com.MyAnimaLog.Pets.infrastructure.adapters.SpringAuthenticatedUserAdapter;
import com.MyAnimaLog.Pets.infrastructure.config.GlobalExceptionHandler;
import com.MyAnimaLog.Pets.infrastructure.config.SecurityConfig;
import com.MyAnimaLog.Pets.infrastructure.controllers.GetPetsBatchController;
import com.MyAnimaLog.Pets.infrastructure.controllers.InternalIsOwnerController;
import com.MyAnimaLog.Pets.infrastructure.controllers.IsOwnerController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprueba la cadena de seguridad real: JWT para las rutas normales,
 * {@code X-Internal-Api-Key} para {@code /internal/**}, y que ninguna de las dos sirva para la otra.
 */
@WebMvcTest(controllers = {InternalIsOwnerController.class, GetPetsBatchController.class, IsOwnerController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class, SpringAuthenticatedUserAdapter.class})
@TestPropertySource(properties = {
        "app.security.internal-api-key=clave-interna-de-prueba",
        "app.security.issuer=myanimalog-user-service",
        "app.security.audience=myanimalog-api",
        "app.security.jwks-uri=http://localhost:8080/.well-known/jwks.json"
})
class SecurityFilterChainTest {

    private static final String KEY = "clave-interna-de-prueba";
    private static final String VALID_TOKEN = "token-valido";
    private static final String INVALID_TOKEN = "token-con-iss-o-aud-incorrectos";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private IsOwnerUseCase isOwnerUseCase;

    @MockitoBean
    private GetPetsBatchUseCase getPetsBatchUseCase;

    private UUID petId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        userId = UUID.randomUUID();

        when(jwtDecoder.decode(VALID_TOKEN)).thenAnswer(invocation -> jwtFor(userId));
        when(jwtDecoder.decode(INVALID_TOKEN))
                .thenThrow(new BadJwtException("The iss claim is not valid"));
    }

    // ---------- /internal/** : solo X-Internal-Api-Key ----------

    @Test
    void internalRoute_shouldReturn401_whenApiKeyIsMissing() throws Exception {
        mockMvc.perform(get("/internal/pets/{petId}/owner/{userId}", petId, userId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void internalRoute_shouldReturn401_whenApiKeyIsWrong() throws Exception {
        mockMvc.perform(get("/internal/pets/{petId}/owner/{userId}", petId, userId)
                        .header("X-Internal-Api-Key", "clave-falsa"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void internalRoute_shouldReturn200_whenApiKeyIsCorrectAndThereIsNoJwt() throws Exception {
        when(isOwnerUseCase.execute(any(IsOwnerRequest.class)))
                .thenReturn(IsOwnerResponse.builder().isOwner(true).build());

        mockMvc.perform(get("/internal/pets/{petId}/owner/{userId}", petId, userId)
                        .header("X-Internal-Api-Key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOwner").value(true));
    }

    @Test
    void internalRoute_shouldReturn401_whenOnlyAUserJwtIsSent() throws Exception {
        mockMvc.perform(get("/internal/pets/{petId}/owner/{userId}", petId, userId)
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    // ---------- rutas normales: solo JWT ----------

    @Test
    void protectedRoute_shouldReturn401_whenThereIsNoToken() throws Exception {
        mockMvc.perform(get("/pets/{petId}/owner/{userId}", petId, userId))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void protectedRoute_shouldPass_whenTokenIsValid() throws Exception {
        when(isOwnerUseCase.execute(any(IsOwnerRequest.class)))
                .thenReturn(IsOwnerResponse.builder().isOwner(true).build());

        mockMvc.perform(get("/pets/{petId}/owner/{userId}", petId, userId)
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(true));
    }

    @Test
    void protectedRoute_shouldReturn401_whenIssuerOrAudienceAreWrong() throws Exception {
        mockMvc.perform(get("/pets/{petId}/owner/{userId}", petId, userId)
                        .header("Authorization", "Bearer " + INVALID_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void protectedRoute_shouldReturn401_whenOnlyTheInternalApiKeyIsSent() throws Exception {
        mockMvc.perform(get("/pets/{petId}/owner/{userId}", petId, userId)
                        .header("X-Internal-Api-Key", KEY))
                .andExpect(status().isUnauthorized());
    }

    // ---------- alias deprecados: solo la propia relación ----------

    @Test
    void deprecatedAlias_shouldReturn403_whenAskingAboutAnotherUser() throws Exception {
        mockMvc.perform(get("/pets/{petId}/owner/{userId}", petId, UUID.randomUUID())
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    // ---------- errores de cliente ----------

    @Test
    void unknownRoute_shouldReturn404() throws Exception {
        mockMvc.perform(get("/esta-ruta-no-existe")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrongMethod_shouldReturn405() throws Exception {
        mockMvc.perform(post("/internal/pets/{petId}/owner/{userId}", petId, userId)
                        .header("X-Internal-Api-Key", KEY))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void malformedBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/internal/pets/batch")
                        .header("X-Internal-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": [\"no-es-un-uuid\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void malformedPathVariable_shouldReturn400() throws Exception {
        mockMvc.perform(get("/internal/pets/no-es-un-uuid/owner/{userId}", userId)
                        .header("X-Internal-Api-Key", KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    private Jwt jwtFor(UUID subject) {
        Instant now = Instant.now();
        return Jwt.withTokenValue(VALID_TOKEN)
                .header("alg", "RS256")
                .claim(JwtClaimNames.ISS, "myanimalog-user-service")
                .claim(JwtClaimNames.AUD, List.of("myanimalog-api"))
                .claim(JwtClaimNames.SUB, subject.toString())
                .claim("email", "ana@correo.com")
                .claim("email_verified", true)
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .build();
    }
}
