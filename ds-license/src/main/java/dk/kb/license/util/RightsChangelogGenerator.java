package dk.kb.license.util;

import dk.kb.license.model.v1.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates a change text when saving or updating a Rights object (restrictedID or holdbackrules)
 *
 */
public class RightsChangelogGenerator {

    public static ChangeDifferenceText createRestrictedIdChanges(RestrictedIdInputDto restrictedIdInputDto) {
        return new ChangeDifferenceText(null,restrictedIdInputDto.toString());
    }

    public static ChangeDifferenceText deleteRestrictedIdChanges(String id, String idType, String platform) {
        return new ChangeDifferenceText(restrictedIdText(id, idType, platform),null);
    }

    public static ChangeDifferenceText updateRestrictedIdChanges(RestrictedIdOutputDto oldVersion, RestrictedIdOutputDto newVersion) {
        return new ChangeDifferenceText(oldVersion.toString(),newVersion.toString());
    }

    public static ChangeDifferenceText createDrHoldbackRuleInputDtoChanges(DrHoldbackRuleInputDto drHoldbackRuleInputDto) {
        return new ChangeDifferenceText(null,drHoldbackRuleInputDto.toString());
    }

    public static ChangeDifferenceText createDrHoldbackRuleOutputDtoChanges(DrHoldbackRuleOutputDto drHoldbackRuleOutputDto) {
        return new ChangeDifferenceText(null,drHoldbackRuleOutputDto.toString());
    }

    public static ChangeDifferenceText createHoldbackRangesChanges(List<DrHoldbackRangeInputDto> ranges) {
        String rangesString = ranges.stream().map(DrHoldbackRangeInputDto::toString)
                .collect(Collectors.joining(", "));
        return new ChangeDifferenceText(null,"["+rangesString+"]");
    }

    public static ChangeDifferenceText deleteHoldbackRangesChanges(List<DrHoldbackRangeOutputDto> oldRanges) {
        String rangesString = oldRanges.stream().map(DrHoldbackRangeOutputDto::toString)
                .collect(Collectors.joining(", "));
        return new ChangeDifferenceText(null,"["+rangesString+"]");
    }

    private static String restrictedIdText(String id, String idType, String platform) {
        StringBuilder builder = new StringBuilder();
        builder.append("Restricted ID - value: ");
        builder.append(id);
        builder.append(" type: ");
        builder.append(idType);
        builder.append(" platform: ");
        builder.append(platform);
        return builder.toString();
    }
}
