package com.MyAnimaLog.Pets.application.dto;

/**
 * Contrato con Medical: el campo se llama exactamente {@code isOwner}
 * (PETS_REQUERIMIENTOS_DESDE_MEDICAL.md §3). No se reutiliza {@code IsOwnerResponse} porque Lombok
 * lo serializa como {@code owner}.
 */
public record InternalIsOwnerResponse(boolean isOwner) {
}
