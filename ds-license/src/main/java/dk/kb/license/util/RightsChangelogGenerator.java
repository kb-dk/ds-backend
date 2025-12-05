package dk.kb.license.util;

import dk.kb.license.model.v1.DrHoldbackRangeDto;
import dk.kb.license.model.v1.DrHoldbackRangeOutputDto;
import dk.kb.license.model.v1.DrHoldbackCategoryOutputDto;
import dk.kb.license.model.v1.RestrictedIdOutputDto;

/**
 * Generates a change text when saving or updating a Rights object (restrictedID or DR holdback categories)
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

    public static ChangeDifferenceText createDrHoldbackCategoryChanges(DrHoldbackCategoryOutputDto drHoldbackCategoryOutputDto) {
        return new ChangeDifferenceText(null, drHoldbackCategoryOutputDto.toString());
    }

    public static ChangeDifferenceText updateDrHoldbackCategoryChanges(DrHoldbackCategoryOutputDto oldVersion, DrHoldbackCategoryOutputDto newVersion) {
        return new ChangeDifferenceText(oldVersion.toString(), newVersion.toString());
    }

    public static ChangeDifferenceText deleteDrHoldbackCategoryChanges(DrHoldbackCategoryOutputDto drHoldbackCategoryOutputDto) {
        return new ChangeDifferenceText(drHoldbackCategoryOutputDto.toString(), null);
    }

    public static ChangeDifferenceText createDrHoldbackRangeChanges(DrHoldbackRangeDto drHoldbackRangeDto) {
        return new ChangeDifferenceText(null, drHoldbackRangeDto.toString());
    }

    public static ChangeDifferenceText deleteDrHoldbackRangeChanges(DrHoldbackRangeOutputDto drHoldbackRangeDto) {
        return new ChangeDifferenceText(drHoldbackRangeDto.toString(), null);
    }
}
