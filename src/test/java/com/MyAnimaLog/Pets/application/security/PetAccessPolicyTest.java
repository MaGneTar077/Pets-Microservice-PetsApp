package com.MyAnimaLog.Pets.application.security;

import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotBelongsToOwnerException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PetAccessPolicyTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @Mock
    private PetUserAccessRepositoryPort petUserAccessRepositoryPort;

    @Mock
    private CurrentUserIdentity currentUserIdentity;

    @InjectMocks
    private PetAccessPolicy petAccessPolicy;

    private UUID petId;
    private UUID ownerId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
    }

    // ---------- dueño ----------

    @Test
    void owner_shouldHaveEveryLevelOfAccess() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(ownerId);

        assertThat(petAccessPolicy.resolve(petId)).contains(Rol.OWNER);
        assertThat(petAccessPolicy.requireOwner(petId).getId()).isEqualTo(petId);
        assertThat(petAccessPolicy.requireWrite(petId).getId()).isEqualTo(petId);
        assertThat(petAccessPolicy.requireRead(petId).getId()).isEqualTo(petId);
    }

    @Test
    void owner_shouldNotNeedTheSharedAccessTable() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(ownerId);

        petAccessPolicy.requireRead(petId);

        verifyNoInteractions(petUserAccessRepositoryPort);
    }

    // ---------- EDITOR ----------

    @Test
    void editor_shouldReadAndWriteButNotOwn() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);
        givenSharedAccess(Rol.EDITOR);

        assertThat(petAccessPolicy.resolve(petId)).contains(Rol.EDITOR);
        assertThat(petAccessPolicy.requireRead(petId).getId()).isEqualTo(petId);
        assertThat(petAccessPolicy.requireWrite(petId).getId()).isEqualTo(petId);
        assertThatThrownBy(() -> petAccessPolicy.requireOwner(petId))
                .isInstanceOf(PetNotBelongsToOwnerException.class);
    }

    // ---------- VIEWER ----------

    @Test
    void viewer_shouldOnlyRead() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);
        givenSharedAccess(Rol.VIEWER);

        assertThat(petAccessPolicy.resolve(petId)).contains(Rol.VIEWER);
        assertThat(petAccessPolicy.requireRead(petId).getId()).isEqualTo(petId);
        assertThatThrownBy(() -> petAccessPolicy.requireWrite(petId))
                .isInstanceOf(UnauthorizedPetAccessException.class);
        assertThatThrownBy(() -> petAccessPolicy.requireOwner(petId))
                .isInstanceOf(PetNotBelongsToOwnerException.class);
    }

    // ---------- sin acceso ----------

    @Test
    void strangerWithoutAccess_shouldBeDeniedEverything() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);
        givenNoSharedAccess();

        assertThat(petAccessPolicy.resolve(petId)).isEmpty();
        assertThatThrownBy(() -> petAccessPolicy.requireRead(petId))
                .isInstanceOf(UnauthorizedPetAccessException.class);
        assertThatThrownBy(() -> petAccessPolicy.requireWrite(petId))
                .isInstanceOf(UnauthorizedPetAccessException.class);
        assertThatThrownBy(() -> petAccessPolicy.requireOwner(petId))
                .isInstanceOf(PetNotBelongsToOwnerException.class);
    }

    // ---------- documentos ----------

    @Test
    void requireOwnerOrUploader_shouldAllowTheOwner_evenIfSomebodyElseUploaded() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(ownerId);

        assertThat(petAccessPolicy.requireOwnerOrUploader(petId, otherUserId).getId()).isEqualTo(petId);
    }

    @Test
    void requireOwnerOrUploader_shouldAllowTheUploader() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);

        assertThat(petAccessPolicy.requireOwnerOrUploader(petId, otherUserId).getId()).isEqualTo(petId);
    }

    @Test
    void requireOwnerOrUploader_shouldRejectAnybodyElse() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);

        assertThatThrownBy(() -> petAccessPolicy.requireOwnerOrUploader(petId, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedPetAccessException.class);
    }

    // ---------- ownerId heredado (endpoints de una mascota concreta) ----------

    @Test
    void legacyOwnerId_editorWithTheRealOwnerId_shouldBeAccepted() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);
        givenSharedAccess(Rol.EDITOR);

        assertThat(petAccessPolicy.requireWrite(petId, ownerId).getId()).isEqualTo(petId);
    }

    @Test
    void legacyOwnerId_editorWithAnUnrelatedOwnerId_shouldBeRejected() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);
        givenSharedAccess(Rol.EDITOR);

        assertThatThrownBy(() -> petAccessPolicy.requireWrite(petId, UUID.randomUUID()))
                .isInstanceOf(IdentityMismatchException.class);
    }

    @Test
    void legacyOwnerId_viewerWithTheRealOwnerId_shouldStillBeDeniedWrite() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);
        givenSharedAccess(Rol.VIEWER);

        assertThatThrownBy(() -> petAccessPolicy.requireWrite(petId, ownerId))
                .isInstanceOf(UnauthorizedPetAccessException.class);
    }

    @Test
    void legacyOwnerId_matchingTheToken_shouldBeAccepted() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(ownerId);

        assertThat(petAccessPolicy.requireWrite(petId, ownerId).getId()).isEqualTo(petId);
    }

    @Test
    void legacyOwnerId_doesNotGrantPermissions_byItself() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);
        givenNoSharedAccess();

        // ownerId coincide con el dueño real, pero el usuario del token no tiene ningún acceso.
        assertThatThrownBy(() -> petAccessPolicy.requireWrite(petId, ownerId))
                .isInstanceOf(UnauthorizedPetAccessException.class);
    }

    @Test
    void legacyOwnerId_onRequireOwner_editorWithTheRealOwnerId_shouldStillBeDeniedOwnerOnlyAction() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(otherUserId);
        givenSharedAccess(Rol.EDITOR);

        assertThatThrownBy(() -> petAccessPolicy.requireOwner(petId, ownerId))
                .isInstanceOf(PetNotBelongsToOwnerException.class);
    }

    @Test
    void legacyOwnerId_null_shouldSkipTheCompatibilityCheck() {
        givenPetOwnedBy(ownerId);
        givenCurrentUser(ownerId);

        assertThat(petAccessPolicy.requireWrite(petId, null).getId()).isEqualTo(petId);
    }

    // ---------- mascota inexistente y mascota sin dueño ----------

    @Test
    void shouldThrowPetNotFound_whenThePetDoesNotExist() {
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petAccessPolicy.requireRead(petId))
                .isInstanceOf(PetNotFoundException.class);
        assertThatThrownBy(() -> petAccessPolicy.requireWrite(petId))
                .isInstanceOf(PetNotFoundException.class);
        assertThatThrownBy(() -> petAccessPolicy.requireOwner(petId))
                .isInstanceOf(PetNotFoundException.class);
    }

    /** PETS-05 hará owner_id nullable: una mascota de clínica no debe romper la política. */
    @Test
    void petWithoutOwner_shouldDenyAccessWithoutThrowingNullPointer() {
        givenPetOwnedBy(null);
        givenCurrentUser(otherUserId);
        givenNoSharedAccess();

        assertThat(petAccessPolicy.resolve(petId)).isEmpty();
        assertThatThrownBy(() -> petAccessPolicy.requireRead(petId))
                .isInstanceOf(UnauthorizedPetAccessException.class);
        assertThatThrownBy(() -> petAccessPolicy.requireOwner(petId))
                .isInstanceOf(PetNotBelongsToOwnerException.class);
    }

    @Test
    void petWithoutOwner_shouldStillAllowTheUploaderOfADocument() {
        givenPetOwnedBy(null);
        givenCurrentUser(otherUserId);

        assertThat(petAccessPolicy.requireOwnerOrUploader(petId, otherUserId).getId()).isEqualTo(petId);
    }

    // ---------- helpers ----------

    private void givenPetOwnedBy(UUID petOwnerId) {
        Pet pet = Pet.builder()
                .id(petId)
                .ownerId(petOwnerId)
                .name("Firulais")
                .species("Perro")
                .sex(Sex.MALE)
                .build();
        when(petRepositoryPort.findById(petId)).thenReturn(Optional.of(pet));
    }

    private void givenCurrentUser(UUID userId) {
        when(currentUserIdentity.userId()).thenReturn(userId);
    }

    private void givenSharedAccess(Rol role) {
        lenient().when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, otherUserId))
                .thenReturn(Optional.of(PetUserAccess.builder()
                        .id(UUID.randomUUID())
                        .petId(petId)
                        .userId(otherUserId)
                        .accessRole(role)
                        .build()));
    }

    private void givenNoSharedAccess() {
        when(petUserAccessRepositoryPort.findByPetIdAndUserId(petId, otherUserId))
                .thenReturn(Optional.empty());
    }
}
