package com.MyAnimaLog.Pets.application.security;

import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Rol;
import com.MyAnimaLog.Pets.domain.exceptions.IdentityMismatchException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotBelongsToOwnerException;
import com.MyAnimaLog.Pets.domain.exceptions.PetNotFoundException;
import com.MyAnimaLog.Pets.domain.exceptions.UnauthorizedPetAccessException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import com.MyAnimaLog.Pets.domain.model.PetUserAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Autorización sobre una mascota para el usuario del token. Ningún servicio compara {@code ownerId}
 * por su cuenta.
 *
 * <p>Matriz de permisos:
 * <table>
 *   <tr><th>Operación</th><th>Dueño</th><th>EDITOR</th><th>VIEWER</th></tr>
 *   <tr><td>Leer mascota, edad, documentos, URL de descarga</td><td>sí</td><td>sí</td><td>sí</td></tr>
 *   <tr><td>Editar datos y foto, subir documentos</td><td>sí</td><td>sí</td><td>no</td></tr>
 *   <tr><td>Editar metadatos y borrar un documento</td><td>sí</td><td>solo los suyos</td><td>no</td></tr>
 *   <tr><td>Borrar mascota, compartir, cambiar roles, invitar, ver compartidos</td><td>sí</td><td>no</td><td>no</td></tr>
 * </table>
 *
 * <p>Si la mascota no existe se lanza 404; si existe y no hay acceso, 403. El dueño se determina
 * siempre por {@code pets.owner_id}, no por una fila de {@code pet_user_access}.
 */
@Component
@RequiredArgsConstructor
public class PetAccessPolicy {

    private final PetRepositoryPort petRepositoryPort;
    private final PetUserAccessRepositoryPort petUserAccessRepositoryPort;
    private final CurrentUserIdentity currentUserIdentity;

    /** @return el rol efectivo, o vacío si el usuario del token no tiene ningún acceso. */
    public Optional<Rol> resolve(UUID petId) {
        return resolve(loadPet(petId));
    }

    /** Solo el dueño. Para borrar la mascota, compartir, cambiar roles, invitar y ver compartidos. */
    public Pet requireOwner(UUID petId) {
        return requireOwner(petId, null);
    }

    /**
     * Igual que {@link #requireOwner(UUID)}, validando además el {@code ownerId} heredado que
     * algunos endpoints todavía reciben por body/query (PETS-02): se acepta si coincide con el
     * token o con el dueño real de la mascota (la app vieja mandaba este segundo valor); si no
     * coincide con ninguno de los dos, 403 {@code IDENTITY_MISMATCH}. No otorga permisos por sí
     * solo — sigue exigiendo ser dueño.
     */
    public Pet requireOwner(UUID petId, UUID legacyOwnerId) {
        Pet pet = loadPet(petId);
        validateLegacyOwnerId(pet, legacyOwnerId);
        if (!isOwner(pet)) {
            throw new PetNotBelongsToOwnerException();
        }
        return pet;
    }

    /** Dueño o {@code EDITOR}. Para editar la mascota, la foto y subir documentos. */
    public Pet requireWrite(UUID petId) {
        return requireWrite(petId, null);
    }

    /** Igual que {@link #requireWrite(UUID)}, validando el {@code ownerId} heredado como en {@link #requireOwner(UUID, UUID)}. */
    public Pet requireWrite(UUID petId, UUID legacyOwnerId) {
        Pet pet = loadPet(petId);
        validateLegacyOwnerId(pet, legacyOwnerId);
        boolean canWrite = resolve(pet)
                .filter(role -> role == Rol.OWNER || role == Rol.EDITOR)
                .isPresent();
        if (!canWrite) {
            throw new UnauthorizedPetAccessException("You do not have write access to this pet");
        }
        return pet;
    }

    /** Dueño, {@code EDITOR} o {@code VIEWER}. Para cualquier lectura. */
    public Pet requireRead(UUID petId) {
        return requireRead(petId, null);
    }

    /** Igual que {@link #requireRead(UUID)}, validando el {@code ownerId} heredado como en {@link #requireOwner(UUID, UUID)}. */
    public Pet requireRead(UUID petId, UUID legacyOwnerId) {
        Pet pet = loadPet(petId);
        validateLegacyOwnerId(pet, legacyOwnerId);
        if (resolve(pet).isEmpty()) {
            throw new UnauthorizedPetAccessException("You do not have access to this pet");
        }
        return pet;
    }

    /**
     * Dueño, o {@code EDITOR} que además subió el documento. Para editar metadatos y borrar un
     * documento. Un {@code VIEWER} nunca pasa, aunque coincida con {@code uploadedBy} (por
     * ejemplo si lo subió siendo {@code EDITOR} y luego lo bajaron de rol): lo que importa es el
     * rol actual, no quién lo subió en su momento.
     */
    public Pet requireOwnerOrUploader(UUID petId, UUID uploadedBy) {
        Pet pet = loadPet(petId);
        boolean isUploaderWithEditAccess = Objects.equals(uploadedBy, currentUserIdentity.userId())
                && resolve(pet).filter(role -> role == Rol.EDITOR).isPresent();
        if (!isOwner(pet) && !isUploaderWithEditAccess) {
            throw new UnauthorizedPetAccessException("Only the owner or the uploader can modify this document");
        }
        return pet;
    }

    private Optional<Rol> resolve(Pet pet) {
        if (isOwner(pet)) {
            return Optional.of(Rol.OWNER);
        }
        return petUserAccessRepositoryPort
                .findByPetIdAndUserId(pet.getId(), currentUserIdentity.userId())
                .map(PetUserAccess::getAccessRole);
    }

    /** {@code Objects.equals} porque PETS-05 hará {@code owner_id} nullable (mascotas de clínica). */
    private boolean isOwner(Pet pet) {
        return Objects.equals(pet.getOwnerId(), currentUserIdentity.userId());
    }

    /**
     * Regla de compatibilidad para endpoints de UNA mascota concreta (PETS-02): {@code legacyOwnerId}
     * ausente → nada que validar; igual al token o igual al dueño real de la mascota → se acepta
     * (ninguno de los dos casos otorga permisos, {@link #resolve(Pet)} sigue decidiendo eso);
     * cualquier otro valor → 403.
     */
    private void validateLegacyOwnerId(Pet pet, UUID legacyOwnerId) {
        if (legacyOwnerId == null) {
            return;
        }
        if (legacyOwnerId.equals(currentUserIdentity.userId())) {
            return;
        }
        if (Objects.equals(legacyOwnerId, pet.getOwnerId())) {
            return;
        }
        throw new IdentityMismatchException();
    }

    private Pet loadPet(UUID petId) {
        return petRepositoryPort.findById(petId)
                .orElseThrow(() -> new PetNotFoundException(petId));
    }
}
