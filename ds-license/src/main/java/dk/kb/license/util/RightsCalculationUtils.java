package dk.kb.license.util;

import dk.kb.license.model.v1.HoldbackCalculationInputDto;
import dk.kb.license.model.v1.RestrictionsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationInputDto;

public class RightsCalculationUtils {

    /**
     * Create a {@link RightsCalculationInputDto} from all needed values
     * @param id
     * @param startTime
     * @param platform
     * @param form
     * @param hensigt
     * @param indhold
     * @param productionCountry
     * @param productionCode
     * @param title
     * @param drProductionId
     * @return
     */
    public static RightsCalculationInputDto createRightsCalculationInputDto(String id, String startTime, RightsCalculationInputDto.PlatformEnum platform,
                                                                     int form, int hensigt, int indhold, int productionCountry, String productionCode,
                                                                     String title, String drProductionId) {
        RightsCalculationInputDto dto = new RightsCalculationInputDto();
        HoldbackCalculationInputDto holdbackDto = new HoldbackCalculationInputDto();
        RestrictionsCalculationInputDto restrictionsDto = new RestrictionsCalculationInputDto();

        dto.setRecordId(id);
        dto.setStartTime(startTime);
        dto.setPlatform(platform);

        holdbackDto.setForm(form);
        holdbackDto.setHensigt(hensigt);
        holdbackDto.setIndhold(indhold);
        holdbackDto.setProductionCode(productionCode);
        holdbackDto.setProductionCountry(productionCountry);

        restrictionsDto.setRecordId(id);
        restrictionsDto.setTitle(title);
        restrictionsDto.setProductionCode(productionCode);
        restrictionsDto.setDrProductionId(drProductionId);

        dto.setHoldbackInput(holdbackDto);
        dto.setRestrictionsInput(restrictionsDto);

        return dto;
    }
}
