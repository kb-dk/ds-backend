package dk.kb.license.rights;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RightsInputDTOTest {
    private final RightsInputDTO testInputDto = new RightsInputDTO("testRecord", "test-system", "2015-12-17T23:41:29Z", 1000, 1200, 1200, 1000, "2000", "123456", "Der var engang en test");

    @Test
    public void testRecordId(){
        assertEquals("testRecord", testInputDto.getRecordId());
        assertEquals("testRecord", testInputDto.getRestrictions().getRecordId());
    }

    @Test
    public void testOwnProductionCode() {
        assertEquals("2000", testInputDto.getRestrictions().getOwnProductionCode());
        assertEquals("2000", testInputDto.getHoldbackValues().getProductionCode());
    }

}
