package dk.kb.license.util;

import dk.kb.license.model.v1.DrHoldbackRangeDto;
import dk.kb.license.model.v1.DrHoldbackRangeOutputDto;
import dk.kb.license.model.v1.DrHoldbackRuleOutputDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;

/**
 * Generates a change text when saving or updating a Rights object (restrictedID or DR holdback rules)
 */
public class RightsChangelogGenerator {

    public static ChangeDifferenceText createRestrictedIdChanges(RestrictedIdOutputDto restrictedIdOutputDto) {
        return new ChangeDifferenceText(null, restrictedIdOutputDto.toString());
    }

    public static ChangeDifferenceText deleteRestrictedIdChanges(RestrictedIdOutputDto restrictedIdOutputDto) {
        return new ChangeDifferenceText(restrictedIdOutputDto.toString(), null);
    }

    public static ChangeDifferenceText updateRestrictedIdChanges(RestrictedIdOutputDto oldVersion, RestrictedIdOutputDto newVersion) {
        return new ChangeDifferenceText(oldVersion.toString(), newVersion.toString());
    }

    public static ChangeDifferenceText createDrHoldbackRuleChanges(DrHoldbackRuleOutputDto drHoldbackRuleOutputDto) {
        return new ChangeDifferenceText(null, drHoldbackRuleOutputDto.toString());
    }

    public static ChangeDifferenceText updateDrHoldbackRuleChanges(DrHoldbackRuleOutputDto oldVersion, DrHoldbackRuleOutputDto newVersion) {
        return new ChangeDifferenceText(oldVersion.toString(), newVersion.toString());
    }

    public static ChangeDifferenceText deleteDrHoldbackRuleChanges(DrHoldbackRuleOutputDto drHoldbackRuleOutputDto) {
        return new ChangeDifferenceText(drHoldbackRuleOutputDto.toString(), null);
    }

    public static ChangeDifferenceText createDrHoldbackRangeChanges(DrHoldbackRangeDto drHoldbackRangeDto) {
        return new ChangeDifferenceText(null, drHoldbackRangeDto.toString());
    }

    public static ChangeDifferenceText deleteDrHoldbackRangeChanges(DrHoldbackRangeOutputDto drHoldbackRangeDto) {
        return new ChangeDifferenceText(drHoldbackRangeDto.toString(), null);
    }
}
