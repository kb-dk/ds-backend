package dk.kb.license.mapper;

import dk.kb.license.model.v1.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProcessedRestrictedIdsOutputDtoMapperTest {

    @Test
    public void map_whenOnlySuccessfullyRestrictedId_thenProcessStatusIsSuccess() {
        // Arrange
        int processedSuccessfully = 1;

        List<FailedRestrictedIdDto> failedRestrictedIdDtoList = new ArrayList<>();

        ProcessedRestrictedIdsOutputDtoMapper processedRestrictedIdsOutputDtoMapper = new ProcessedRestrictedIdsOutputDtoMapper();

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = processedRestrictedIdsOutputDtoMapper.map(processedSuccessfully, failedRestrictedIdDtoList);

        // Assert
        assertEquals(ProcessStatusDto.SUCCESS, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(processedSuccessfully, processedRestrictedIdsOutputDto.getProcessedSuccessfully());

        assertEquals(0, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
    }

    @Test
    public void map_whenOneSuccessfullyAndOneFailedRestrictedId_thenProcessStatusIsPartialProcessed() {
        // Arrange
        int processedSuccessfully = 1;

        FailedRestrictedIdDto failedRestrictedIdDto = new FailedRestrictedIdDto();
        failedRestrictedIdDto.setIdValue("12345678");
        failedRestrictedIdDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        failedRestrictedIdDto.setPlatform(PlatformEnumDto.DRARKIV);
        failedRestrictedIdDto.setTitle("TV Avisen");
        failedRestrictedIdDto.setComment("Brugeren har trukket deres samtykke tilbage");
        failedRestrictedIdDto.setException("Exception");
        failedRestrictedIdDto.setErrorMessage("Test exception");

        List<FailedRestrictedIdDto> failedRestrictedIdDtoList = List.of(failedRestrictedIdDto);

        ProcessedRestrictedIdsOutputDtoMapper processedRestrictedIdsOutputDtoMapper = new ProcessedRestrictedIdsOutputDtoMapper();

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = processedRestrictedIdsOutputDtoMapper.map(processedSuccessfully, failedRestrictedIdDtoList);

        // Assert
        assertEquals(ProcessStatusDto.PARTIAL_PROCESSED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(processedSuccessfully, processedRestrictedIdsOutputDto.getProcessedSuccessfully());

        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(failedRestrictedIdDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(failedRestrictedIdDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(failedRestrictedIdDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(failedRestrictedIdDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(failedRestrictedIdDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals(failedRestrictedIdDto.getException(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals(failedRestrictedIdDto.getErrorMessage(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());
    }

    @Test
    public void map_whenOnlyFailedRestrictedId_thenProcessStatusIsFailed() {
        // Arrange
        int processedSuccessfully = 0;

        FailedRestrictedIdDto failedRestrictedIdDto = new FailedRestrictedIdDto();
        failedRestrictedIdDto.setIdValue("12345678");
        failedRestrictedIdDto.setIdType(IdTypeEnumDto.DR_PRODUCTION_ID);
        failedRestrictedIdDto.setPlatform(PlatformEnumDto.DRARKIV);
        failedRestrictedIdDto.setTitle("TV Avisen");
        failedRestrictedIdDto.setComment("Brugeren har trukket deres samtykke tilbage");
        failedRestrictedIdDto.setException("Exception");
        failedRestrictedIdDto.setErrorMessage("Test exception");

        List<FailedRestrictedIdDto> failedRestrictedIdDtoList = List.of(failedRestrictedIdDto);

        ProcessedRestrictedIdsOutputDtoMapper processedRestrictedIdsOutputDtoMapper = new ProcessedRestrictedIdsOutputDtoMapper();

        // Act
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = processedRestrictedIdsOutputDtoMapper.map(processedSuccessfully, failedRestrictedIdDtoList);

        // Assert
        assertEquals(ProcessStatusDto.FAILED, processedRestrictedIdsOutputDto.getProcessStatus());
        assertEquals(processedSuccessfully, processedRestrictedIdsOutputDto.getProcessedSuccessfully());

        assertEquals(1, processedRestrictedIdsOutputDto.getFailedRestrictedIds().size());
        assertEquals(failedRestrictedIdDto.getIdValue(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdValue());
        assertEquals(failedRestrictedIdDto.getIdType(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getIdType());
        assertEquals(failedRestrictedIdDto.getPlatform(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getPlatform());
        assertEquals(failedRestrictedIdDto.getTitle(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getTitle());
        assertEquals(failedRestrictedIdDto.getComment(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getComment());
        assertEquals(failedRestrictedIdDto.getException(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getException());
        assertEquals(failedRestrictedIdDto.getErrorMessage(), processedRestrictedIdsOutputDto.getFailedRestrictedIds().get(0).getErrorMessage());
    }
}
