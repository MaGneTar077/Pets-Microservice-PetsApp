package com.MyAnimaLog.Pets.application.ports.in;

import com.MyAnimaLog.Pets.application.dto.GetPendingInvitationsByPetRequest;
import com.MyAnimaLog.Pets.application.dto.PetInvitationResponse;

import java.util.List;

public interface GetPendingInvitationsByPetUseCase {
    List<PetInvitationResponse> execute(GetPendingInvitationsByPetRequest request);
}
