package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.IsOwnerRequest;
import com.MyAnimaLog.Pets.application.dto.IsOwnerResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
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
class IsOwnerServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @InjectMocks
    private IsOwnerService isOwnerService;

    private UUID petId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    @Test
    void execute_shouldReturnIsOwnerTrue_whenUserIsOwner() {
        when(petRepositoryPort.existsByIdAndOwnerId(petId, ownerId)).thenReturn(true);

        IsOwnerResponse result = isOwnerService.execute(requestFor(ownerId));

        assertThat(result).isNotNull();
        assertThat(result.isOwner()).isTrue();
    }

    @Test
    void execute_shouldReturnIsOwnerFalse_whenUserIsNotOwner() {
        UUID otherUser = UUID.randomUUID();
        when(petRepositoryPort.existsByIdAndOwnerId(petId, otherUser)).thenReturn(false);

        IsOwnerResponse result = isOwnerService.execute(requestFor(otherUser));

        assertThat(result).isNotNull();
        assertThat(result.isOwner()).isFalse();
    }

    @Test
    void execute_shouldReturnIsOwnerFalse_whenPetDoesNotExist() {
        when(petRepositoryPort.existsByIdAndOwnerId(petId, ownerId)).thenReturn(false);

        IsOwnerResponse result = isOwnerService.execute(requestFor(ownerId));

        assertThat(result.isOwner()).isFalse();
    }

    @Test
    void execute_shouldQueryRepository_onlyOnce() {
        when(petRepositoryPort.existsByIdAndOwnerId(petId, ownerId)).thenReturn(true);

        isOwnerService.execute(requestFor(ownerId));

        verify(petRepositoryPort, times(1)).existsByIdAndOwnerId(petId, ownerId);
        verifyNoMoreInteractions(petRepositoryPort);
    }

    private IsOwnerRequest requestFor(UUID userId) {
        return IsOwnerRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();
    }
}
