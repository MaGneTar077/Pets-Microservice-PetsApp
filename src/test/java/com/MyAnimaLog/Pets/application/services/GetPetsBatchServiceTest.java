package com.MyAnimaLog.Pets.application.services;

import com.MyAnimaLog.Pets.application.dto.GetPetsBatchRequest;
import com.MyAnimaLog.Pets.application.dto.InternalPetResponse;
import com.MyAnimaLog.Pets.application.ports.out.PetRepositoryPort;
import com.MyAnimaLog.Pets.domain.enums.Sex;
import com.MyAnimaLog.Pets.domain.exceptions.InvalidPetDataException;
import com.MyAnimaLog.Pets.domain.model.Pet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetPetsBatchServiceTest {

    @Mock
    private PetRepositoryPort petRepositoryPort;

    @InjectMocks
    private GetPetsBatchService getPetsBatchService;

    @Test
    void execute_shouldReturnOnlyExistingPets_whenSomeIdsDoNotExist() {
        UUID existingId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        List<UUID> ids = List.of(existingId, missingId);

        when(petRepositoryPort.findAllByIds(ids)).thenReturn(List.of(petWithId(existingId)));

        List<InternalPetResponse> result = getPetsBatchService.execute(requestFor(ids));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(existingId);
    }

    @Test
    void execute_shouldMapEveryFieldOfTheContract() {
        UUID petId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Pet pet = Pet.builder()
                .id(petId)
                .ownerId(ownerId)
                .name("Max")
                .species("Perro")
                .breed("Criollo")
                .sex(Sex.MALE)
                .birthDate(LocalDate.of(2022, 1, 15))
                .photoUrl("https://cdn/pets/max.png")
                .build();

        when(petRepositoryPort.findAllByIds(List.of(petId))).thenReturn(List.of(pet));

        InternalPetResponse response = getPetsBatchService.execute(requestFor(List.of(petId))).get(0);

        assertThat(response.id()).isEqualTo(petId);
        assertThat(response.ownerId()).isEqualTo(ownerId);
        assertThat(response.name()).isEqualTo("Max");
        assertThat(response.species()).isEqualTo("Perro");
        assertThat(response.breed()).isEqualTo("Criollo");
        assertThat(response.sex()).isEqualTo(Sex.MALE);
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(2022, 1, 15));
        assertThat(response.photoUrl()).isEqualTo("https://cdn/pets/max.png");
        // PETS-05 todavía no está en la tabla.
        assertThat(response.createdByVeterinaryId()).isNull();
        assertThat(response.claimStatus()).isEqualTo("NONE");
        assertThat(response.ownerContact()).isNull();
    }

    @Test
    void execute_shouldThrowInvalidPetData_whenMoreThan100Ids() {
        List<UUID> ids = Stream.generate(UUID::randomUUID).limit(101).toList();

        assertThatThrownBy(() -> getPetsBatchService.execute(requestFor(ids)))
                .isInstanceOf(InvalidPetDataException.class);

        verifyNoInteractions(petRepositoryPort);
    }

    @Test
    void execute_shouldAccept100Ids() {
        List<UUID> ids = Stream.generate(UUID::randomUUID).limit(100).toList();
        when(petRepositoryPort.findAllByIds(ids)).thenReturn(List.of());

        assertThat(getPetsBatchService.execute(requestFor(ids))).isEmpty();
    }

    @Test
    void execute_shouldReturnEmptyList_whenIdsIsEmpty() {
        assertThat(getPetsBatchService.execute(requestFor(List.of()))).isEmpty();

        verifyNoInteractions(petRepositoryPort);
    }

    @Test
    void execute_shouldReturnEmptyList_whenIdsIsNull() {
        assertThat(getPetsBatchService.execute(requestFor(null))).isEmpty();

        verifyNoInteractions(petRepositoryPort);
    }

    @Test
    void execute_shouldQueryTheRepositoryOnlyOnce() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(petRepositoryPort.findAllByIds(ids)).thenReturn(List.of());

        getPetsBatchService.execute(requestFor(ids));

        verify(petRepositoryPort, times(1)).findAllByIds(ids);
        verifyNoMoreInteractions(petRepositoryPort);
    }

    private GetPetsBatchRequest requestFor(List<UUID> ids) {
        return GetPetsBatchRequest.builder().ids(ids).build();
    }

    private Pet petWithId(UUID id) {
        return Pet.builder()
                .id(id)
                .ownerId(UUID.randomUUID())
                .name("Luna")
                .species("Gato")
                .sex(Sex.FEMALE)
                .build();
    }
}
