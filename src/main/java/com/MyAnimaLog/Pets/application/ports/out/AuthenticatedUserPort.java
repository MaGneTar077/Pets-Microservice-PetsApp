package com.MyAnimaLog.Pets.application.ports.out;

import com.MyAnimaLog.Pets.application.dto.AuthenticatedUser;

public interface AuthenticatedUserPort {

    /** @throws com.MyAnimaLog.Pets.domain.exceptions.UnauthenticatedException si no hay token. */
    AuthenticatedUser current();
}
