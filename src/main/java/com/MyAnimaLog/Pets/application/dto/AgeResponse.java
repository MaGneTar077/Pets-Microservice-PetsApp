package com.MyAnimaLog.Pets.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgeResponse {
    private int years;
    private int months;
    private int days;
}
