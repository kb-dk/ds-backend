package dk.kb.license.rights;

/**
 * Object containing all values that are to be used as input for all types of rights calculation in DS-license.
 */
public class RightsInputDTO {

    public String recordId;
    public String system;
    public String startTime;

    public HoldbackInputDTO holdbackValues;

    public RestrictionsInputDTO restrictions;

    /**
     * Construct a DTO from all needed values.
     * @param recordId of the record which this metadata comes from.
     * @param system that rights are calculated from.
     * @param startTime of the record this object relates to.
     * @param hensigt a metadata value used for holdback calculation.
     * @param form a metadata value used for holdback calculation.
     * @param indhold a metadata value used for holdback calculation.
     * @param productionCountry a metadata value used for holdback calculation.
     * @param productionCode a metadata value used for holdback and own production calculation.
     * @param drProductionId an internal ID from DR which could be restricted.
     * @param title of the record in hand. There is a chance that it is restricted.
     */
    public RightsInputDTO(String recordId, String system, String startTime,
                          int hensigt, int form, int indhold, int productionCountry, String productionCode,
                          String drProductionId, String title) {
        this.recordId = recordId;
        this.system = system;
        this.startTime = startTime;
        this.holdbackValues = new HoldbackInputDTO(hensigt, form, indhold, productionCountry, productionCode);
        this.restrictions = new RestrictionsInputDTO(recordId, drProductionId, productionCode, title);
    }

    public String getRecordId() {
        return recordId;
    }

    public String getSystem() {
        return system;
    }

    public String getStartTime() {
        return startTime;
    }

    public HoldbackInputDTO getHoldbackValues() {
        return holdbackValues;
    }

    public RestrictionsInputDTO getRestrictions() {
        return restrictions;
    }
}
