package com.MyAnimaLog.Pets.application.security;

import com.MyAnimaLog.Pets.application.dto.AuthenticatedUser;
import com.MyAnimaLog.Pets.application.ports.out.AuthenticatedUserPort;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserIdentityTest {

    @Mock
    private AuthenticatedUserPort authenticatedUserPort;

    @InjectMocks
    private CurrentUserIdentity currentUserIdentity;

    private UUID tokenUserId;

    @BeforeEach
    void setUp() {
        tokenUserId = UUID.randomUUID();
        when(authenticatedUserPort.current())
                .thenReturn(new AuthenticatedUser(tokenUserId, "Ana@Correo.com", true));
    }

    @Test
    void resolveUserId_shouldUseTokenIdentity_whenRequestDoesNotSendIt() {
        assertThat(currentUserIdentity.resolveUserId(null)).isEqualTo(tokenUserId);
    }

    @Test
    void resolveUserId_shouldAccept_whenRequestSendsTheSameIdentity() {
        assertThat(currentUserIdentity.resolveUserId(tokenUserId)).isEqualTo(tokenUserId);
    }

    @Test
    void resolveUserId_shouldThrow_whenRequestSendsAnotherIdentity() {
        UUID otherUser = UUID.randomUUID();

        assertThatThrownBy(() -> currentUserIdentity.resolveUserId(otherUser))
                .isInstanceOf(IdentityMismatchException.class);
    }

    @Test
    void resolveEmail_shouldUseTokenEmail_whenRequestDoesNotSendIt() {
        assertThat(currentUserIdentity.resolveEmail(null)).isEqualTo("Ana@Correo.com");
        assertThat(currentUserIdentity.resolveEmail("  ")).isEqualTo("Ana@Correo.com");
    }

    @Test
    void resolveEmail_shouldAccept_whenRequestSendsTheSameEmailInAnotherCase() {
        assertThat(currentUserIdentity.resolveEmail("ANA@correo.COM")).isEqualTo("Ana@Correo.com");
    }

    @Test
    void resolveEmail_shouldThrow_whenRequestSendsAnotherEmail() {
        assertThatThrownBy(() -> currentUserIdentity.resolveEmail("otro@correo.com"))
                .isInstanceOf(IdentityMismatchException.class);
    }

    @Test
    void shouldExposeTheTokenClaims() {
        assertThat(currentUserIdentity.userId()).isEqualTo(tokenUserId);
        assertThat(currentUserIdentity.email()).isEqualTo("Ana@Correo.com");
        assertThat(currentUserIdentity.emailVerified()).isTrue();
    }
}
