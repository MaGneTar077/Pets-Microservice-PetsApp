package com.MyAnimaLog.Pets.application.dto;

import com.MyAnimaLog.Pets.domain.enums.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddPetRequest {

    @NotNull(message = "Owner ID is required")
    private UUID ownerId;

    @NotBlank(message = "Pet name is required")
    private String name;

    @NotBlank(message = "Pet species is required")
    private String species;

    private String breed;

    @NotNull(message = "Sex is required")
    private Sex sex;

    private LocalDate birthDate;
    private Double height;
    private Double weight;
}
