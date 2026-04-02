package com.MyAnimaLog.Pets.domain.model;

import com.MyAnimaLog.Pets.domain.enums.Sex;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Pet {

    private UUID id;
    private UUID ownerId;
    private String name;
    private String species;
    private String breed;
    private Sex sex;
    private LocalDate birthDate;
    private Double height;
    private Double weight;
    private String photoUrl;
    private LocalDateTime createdAt;

}
