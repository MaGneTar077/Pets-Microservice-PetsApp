package com.MyAnimaLog.Pets.application.dto;

import java.util.UUID;

/** Identidad tomada de los claims del token (CONTRATOS_COMPARTIDOS.md §1.2). */
public record AuthenticatedUser(UUID userId, String email, boolean emailVerified) {
}
