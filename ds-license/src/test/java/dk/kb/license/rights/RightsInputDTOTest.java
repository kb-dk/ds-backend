package dk.kb.license.rights;

import dk.kb.license.model.v1.HoldbackCalculationInputDto;
import dk.kb.license.model.v1.PlatformEnumDto;
import dk.kb.license.model.v1.RestrictionsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RightsInputDTOTest {

    @Test
    public void testRecordId(){
        RightsCalculationInputDto testInputDto = getTestDto();
        assertEquals("testRecord", testInputDto.getRecordId());
        assertEquals("testRecord", testInputDto.getRestrictionsInput().getRecordId());
    }

    @Test
    public void testOwnProductionCode() {
        RightsCalculationInputDto testInputDto = getTestDto();
        assertEquals("2000", testInputDto.getRestrictionsInput().getProductionCode());
    }

    private RightsCalculationInputDto getTestDto() {
        RightsCalculationInputDto rightsCalculationInputDto = new RightsCalculationInputDto();

        rightsCalculationInputDto.setRecordId("testRecord");
        rightsCalculationInputDto.setPlatform(PlatformEnumDto.DRARKIV);
        rightsCalculationInputDto.setStartTime("2015-12-17T23:41:29Z");

        HoldbackCalculationInputDto holdbackCalculationInputDto = new HoldbackCalculationInputDto();
        holdbackCalculationInputDto.setHensigt(1000);
        holdbackCalculationInputDto.setForm(1200);
        holdbackCalculationInputDto.setIndhold(1000);
        holdbackCalculationInputDto.setHoldbackCategory(null);
        holdbackCalculationInputDto.setProductionCountry(1000);
        holdbackCalculationInputDto.setOrigin("ds.tv");

        rightsCalculationInputDto.setHoldbackInput(holdbackCalculationInputDto);

        RestrictionsCalculationInputDto restrictionsCalculationInputDto = new RestrictionsCalculationInputDto();
        restrictionsCalculationInputDto.setRecordId("testRecord");
        restrictionsCalculationInputDto.setProductionCode("2000");
        restrictionsCalculationInputDto.setDrProductionId("12345678");
        restrictionsCalculationInputDto.setTitle("Der var engang en test");

        rightsCalculationInputDto.setRestrictionsInput(restrictionsCalculationInputDto);

        return rightsCalculationInputDto;
    }

}
