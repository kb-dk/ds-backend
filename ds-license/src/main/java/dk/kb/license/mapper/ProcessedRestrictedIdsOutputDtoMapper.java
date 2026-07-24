package dk.kb.license.mapper;

import dk.kb.license.model.v1.FailedRestrictedIdDto;
import dk.kb.license.model.v1.ProcessStatusDto;
import dk.kb.license.model.v1.ProcessedRestrictedIdsOutputDto;

import java.util.List;

public class ProcessedRestrictedIdsOutputDtoMapper {

    /**
     * Create a {@link ProcessedRestrictedIdsOutputDto} from processedSuccessfully and failedRestrictedIdDtoList
     *
     * @param processedSuccessfully
     * @param failedRestrictedIdDtoList
     * @return {@link ProcessedRestrictedIdsOutputDto} object
     */
    public ProcessedRestrictedIdsOutputDto map(int processedSuccessfully, List<FailedRestrictedIdDto> failedRestrictedIdDtoList) {
        ProcessedRestrictedIdsOutputDto processedRestrictedIdsOutputDto = new ProcessedRestrictedIdsOutputDto();

        // Need to start with checking processedSuccessfully, for not getting wrongly PARTIAL_PROCESSED
        if (processedSuccessfully == 0) {
            processedRestrictedIdsOutputDto.setProcessStatus(ProcessStatusDto.FAILED);
        } else if (failedRestrictedIdDtoList.isEmpty()) {
            processedRestrictedIdsOutputDto.setProcessStatus(ProcessStatusDto.SUCCESS);
        } else if (!failedRestrictedIdDtoList.isEmpty()) {
            processedRestrictedIdsOutputDto.setProcessStatus(ProcessStatusDto.PARTIAL_PROCESSED);
        }

        processedRestrictedIdsOutputDto.setProcessedSuccessfully(processedSuccessfully);
        processedRestrictedIdsOutputDto.setFailedRestrictedIds(failedRestrictedIdDtoList);

        return processedRestrictedIdsOutputDto;
    }
}
