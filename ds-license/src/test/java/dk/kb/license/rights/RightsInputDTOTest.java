package dk.kb.license.rights;

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
        assertEquals("2000", testInputDto.getHoldbackInput().getProductionCode());
    }

    private RightsCalculationInputDto getTestDto() {
        return new RightsCalculationInputDto("testRecord", "2015-12-17T23:41:29Z", RightsCalculationInputDto.PlatformEnum.DRARKIV,
                1000, 1200, 1000, 1000, "2000", "Der var engang en test", "123456", "ds.tv");

    }

}
