package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetSharedProfilesRequest;
import com.MyAnimaLog.Pets.application.dto.SharedProfileResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.application.security.CurrentUserIdentity;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSharedProfilesServiceTest {

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @Mock
    private CurrentUserIdentity currentUserIdentity;

    @InjectMocks
    private GetSharedProfilesService getSharedProfilesService;

    private UUID userId;
    private PetUserAccess access;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(currentUserIdentity.resolveUserId(any())).thenReturn(userId);

        access = PetUserAccess.builder()
                .id(UUID.randomUUID())
                .petId(UUID.randomUUID())
                .userId(userId)
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnList_whenSharedProfilesExist() {
        when(petUserAccessRepositoryPort.findAllByUserId(userId)).thenReturn(List.of(access));

        GetSharedProfilesRequest request = GetSharedProfilesRequest.builder()
                .userId(userId).build();

        List<SharedProfileResponse> result = getSharedProfilesService.execute(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getAccessRole()).isEqualTo(Rol.EDITOR);
    }

    @Test
    void execute_shouldReturnEmptyList_whenNoSharedProfilesExist() {
        when(petUserAccessRepositoryPort.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        GetSharedProfilesRequest request = GetSharedProfilesRequest.builder()
                .userId(userId).build();

        List<SharedProfileResponse> result = getSharedProfilesService.execute(request);

        assertThat(result).isEmpty();
    }

    @Test
    void execute_shouldCallRepositoryFindAllByUserId_once() {
        when(petUserAccessRepositoryPort.findAllByUserId(userId)).thenReturn(List.of(access));

        GetSharedProfilesRequest request = GetSharedProfilesRequest.builder()
                .userId(userId).build();

        getSharedProfilesService.execute(request);

        verify(petUserAccessRepositoryPort, times(1)).findAllByUserId(userId);
    }

    @Test
    void execute_shouldReturnMultipleProfiles_whenMultipleSharedProfilesExist() {
        PetUserAccess access2 = access.toBuilder()
                .id(UUID.randomUUID())
                .petId(UUID.randomUUID())
                .accessRole(Rol.VIEWER)
                .build();

        when(petUserAccessRepositoryPort.findAllByUserId(userId)).thenReturn(List.of(access, access2));

        GetSharedProfilesRequest request = GetSharedProfilesRequest.builder()
                .userId(userId).build();

        List<SharedProfileResponse> result = getSharedProfilesService.execute(request);

        assertThat(result).hasSize(2);
    }

    @Test
    void execute_shouldUseTheTokenIdentity_whenCalledFromSharedMe() {
        when(petUserAccessRepositoryPort.findAllByUserId(userId)).thenReturn(List.of());

        getSharedProfilesService.execute(GetSharedProfilesRequest.builder().build());

        verify(currentUserIdentity).resolveUserId(null);
        verify(petUserAccessRepositoryPort).findAllByUserId(userId);
    }

    @Test
    void execute_shouldThrow403_whenTheAliasAsksForAnotherUser() {
        UUID otherUser = UUID.randomUUID();
        when(currentUserIdentity.resolveUserId(otherUser)).thenThrow(new IdentityMismatchException());

        GetSharedProfilesRequest request = GetSharedProfilesRequest.builder().userId(otherUser).build();

        assertThatThrownBy(() -> getSharedProfilesService.execute(request))
                .isInstanceOf(IdentityMismatchException.class);

        verifyNoInteractions(petUserAccessRepositoryPort);
    }
}
