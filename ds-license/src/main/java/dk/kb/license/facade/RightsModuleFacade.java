package dk.kb.license.facade;

import dk.kb.license.RightsCalculation;
import dk.kb.license.model.v1.RightsCalculationInputDto;
import dk.kb.license.model.v1.RightsCalculationOutputDrDto;
import dk.kb.license.model.v1.RightsCalculationOutputDto;

public class RightsModuleFacade {

    public RightsCalculationOutputDto calculateRightsForRecord(RightsCalculationInputDto rightsCalculationInputDto) {
        RightsCalculationOutputDto output = new RightsCalculationOutputDto();

        RightsCalculationOutputDrDto drOutput = RightsCalculation.calculateDrRights(rightsCalculationInputDto);

        output.setDr(drOutput);

        return output;
    }

}
