package com.MyAnimaLog.Pets.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EditPetRequest {
    private String name;
    private String species;
    private String breed;
    private String sex;
    private LocalDate birthDate;
    private Double height;
    private Double weight;
    private String photoUrl;
}
