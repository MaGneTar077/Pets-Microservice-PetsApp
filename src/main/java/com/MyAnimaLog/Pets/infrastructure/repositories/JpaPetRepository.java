package com.MyAnimaLog.Pets.infrastructure.repositories;

import com.MyAnimaLog.Pets.infrastructure.entity.PetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPetRepository extends JpaRepository<PetEntity, UUID> {
}
