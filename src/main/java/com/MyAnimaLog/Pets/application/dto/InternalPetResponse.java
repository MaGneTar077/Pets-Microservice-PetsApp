package com.MyAnimaLog.Pets.application.dto;

import com.MyAnimaLog.Pets.domain.enums.Sex;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos de una mascota para servicios internos (PETS_REQUERIMIENTOS_DESDE_MEDICAL.md §4).
 *
 * <p>PETS-05 todavía no está aplicado: la tabla {@code pets} no tiene las columnas
 * {@code created_by_veterinary_id}, {@code claim_status} ni {@code owner_contact_*}, así que
 * {@code createdByVeterinaryId} y {@code ownerContact} van siempre en {@code null} y
 * {@code claimStatus} siempre en {@code "NONE"}.
 */
public record InternalPetResponse(
        UUID id,
        UUID ownerId,
        String name,
        String species,
        String breed,
        Sex sex,
        LocalDate birthDate,
        String photoUrl,
        UUID createdByVeterinaryId,
        String claimStatus,
        OwnerContact ownerContact) {

    /** Solo se envía cuando {@code claimStatus = UNCLAIMED}. */
    public record OwnerContact(String name, String email, String phone) {
    }
}
