package com.MyAnimaLog.Pets.application.security;

import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.application.ports.out.PetUserAccessRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Rol;
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
        Pet pet = loadPet(petId);
        if (!isOwner(pet)) {
            throw new PetNotBelongsToOwnerException();
        }
        return pet;
    }

    /** Dueño o {@code EDITOR}. Para editar la mascota, la foto y subir documentos. */
    public Pet requireWrite(UUID petId) {
        Pet pet = loadPet(petId);
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
        Pet pet = loadPet(petId);
        if (resolve(pet).isEmpty()) {
            throw new UnauthorizedPetAccessException("You do not have access to this pet");
        }
        return pet;
    }

    /** Dueño o quien subió el documento. Para editar metadatos y borrar un documento. */
    public Pet requireOwnerOrUploader(UUID petId, UUID uploadedBy) {
        Pet pet = loadPet(petId);
        if (!isOwner(pet) && !Objects.equals(uploadedBy, currentUserIdentity.userId())) {
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

    private Pet loadPet(UUID petId) {
        return petRepositoryPort.findById(petId)
                .orElseThrow(() -> new PetNotFoundException(petId));
    }
}
