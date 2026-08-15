package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.HasPermissionRequest;
import com.MyAnimaLog.Pets.application.dto.HasPermissionResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HasPermissionServiceTest {

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @InjectMocks
    private HasPermissionService hasPermissionService;

    private UUID petId;
    private UUID userId;
    private PetUserAccess access;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        userId = UUID.randomUUID();

        access = PetUserAccess.builder()
                .id(UUID.randomUUID())
                .petId(petId)
                .userId(userId)
                .accessRole(Rol.EDITOR)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void execute_shouldReturnHasPermissionTrue_whenAccessExists() {
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId))
                .thenReturn(Optional.of(access));

        HasPermissionRequest request = HasPermissionRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        HasPermissionResponse result = hasPermissionService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isHasPermission()).isTrue();
        assertThat(result.getRole()).isEqualTo(Rol.EDITOR);
    }

    @Test
    void execute_shouldReturnHasPermissionFalse_whenAccessDoesNotExist() {
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId))
                .thenReturn(Optional.empty());

        HasPermissionRequest request = HasPermissionRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        HasPermissionResponse result = hasPermissionService.execute(request);

        assertThat(result).isNotNull();
        assertThat(result.isHasPermission()).isFalse();
        assertThat(result.getRole()).isNull();
    }

    @Test
    void execute_shouldCallRepositoryFindByPetIdAndUserId_once() {
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId))
                .thenReturn(Optional.of(access));

        HasPermissionRequest request = HasPermissionRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        hasPermissionService.execute(request);

        verify(petUserAccessRepositoryPort, times(1)).findByPetIdAndUserId(petId, userId);
    }

    @Test
    void execute_shouldReturnViewerRole_whenAccessRoleIsViewer() {
        PetUserAccess viewerAccess = access.toBuilder().accessRole(Rol.VIEWER).build();
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId))
                .thenReturn(Optional.of(viewerAccess));

        HasPermissionRequest request = HasPermissionRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        HasPermissionResponse result = hasPermissionService.execute(request);

        assertThat(result.isHasPermission()).isTrue();
        assertThat(result.getRole()).isEqualTo(Rol.VIEWER);
    }

    @Test
    void execute_shouldReturnOwnerRole_whenAccessRoleIsOwner() {
        PetUserAccess ownerAccess = access.toBuilder().accessRole(Rol.OWNER).build();
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, userId))
                .thenReturn(Optional.of(ownerAccess));

        HasPermissionRequest request = HasPermissionRequest.builder()
                .petId(petId)
                .userId(userId)
                .build();

        HasPermissionResponse result = hasPermissionService.execute(request);

        assertThat(result.isHasPermission()).isTrue();
        assertThat(result.getRole()).isEqualTo(Rol.OWNER);
    }
}