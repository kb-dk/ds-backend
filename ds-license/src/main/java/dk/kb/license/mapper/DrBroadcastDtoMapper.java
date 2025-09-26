package dk.kb.license.mapper;

import dk.kb.license.model.v1.DrBroadcastDto;

public class DrBroadcastDtoMapper {

    /**
     * Map data to DrBroadcastDto object
     *
     * @param drBroadcastDto
     * @param drProductionId
     * @param restrictedIdComment
     * @return DrBroadcastDto object
     */
    public DrBroadcastDto mapDrBroadcastDto(DrBroadcastDto drBroadcastDto, String drProductionId, String restrictedIdComment) {
        drBroadcastDto.setDrProductionId(drProductionId);

        if (restrictedIdComment == null) {
            drBroadcastDto.setRestricted(false);
            drBroadcastDto.setRestrictedComment(null);
        } else {
            drBroadcastDto.setRestricted(true);
            drBroadcastDto.setRestrictedComment(restrictedIdComment);
        }

        return drBroadcastDto;
    }
}
