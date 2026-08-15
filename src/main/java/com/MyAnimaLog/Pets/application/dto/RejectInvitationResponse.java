package com.MyAnimaLog.Pets.application.dto;

import com.MyAnimaLog.Pets.domain.enums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RejectInvitationResponse {
    private UUID invitationId;
    private UUID petId;
    private InvitationStatus status;
}
