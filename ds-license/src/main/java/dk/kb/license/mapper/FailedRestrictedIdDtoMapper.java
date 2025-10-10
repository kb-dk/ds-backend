package dk.kb.license.mapper;

import dk.kb.license.model.v1.FailedRestrictedIdDto;
import dk.kb.license.model.v1.RestrictedIdInputDto;

public class FailedRestrictedIdDtoMapper {

    /**
     * Map RestrictedIdInputDto and Exception to FailedRestrictedIdDto
     *
     * @param restrictedIdInputDto
     * @param exception
     * @return FailedRestrictedIdDto
     */
    public FailedRestrictedIdDto mapFailedRestrictedIdDto(RestrictedIdInputDto restrictedIdInputDto, Exception exception) {
        FailedRestrictedIdDto failedRestrictedIdDto = new FailedRestrictedIdDto();

        failedRestrictedIdDto.setIdValue(restrictedIdInputDto.getIdValue());
        failedRestrictedIdDto.setIdType(restrictedIdInputDto.getIdType());
        failedRestrictedIdDto.setPlatform(restrictedIdInputDto.getPlatform());
        failedRestrictedIdDto.setComment(restrictedIdInputDto.getComment());
        failedRestrictedIdDto.setException(exception.getClass().getSimpleName());
        failedRestrictedIdDto.setErrorMessage(exception.getMessage());

        return failedRestrictedIdDto;
    }
}
