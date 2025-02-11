package dk.kb.license.rights;

/**
 * This object contains values that are to be looked up in the klausuleringer-table. This object should always be instantiated as part of a {@link RightsInputDTO}.
 * The object contains the following values:
 * <ul>
 *     <li>{@link #recordId}: The ID of the record this metadata is from.</li>
 *     <li>{@link #drProductionId}: For DR records this is an internal DR ID, which might have been restricted by DR. This is checked in the klausuleringer-tabel.</li>
 *     <li>{@link #ownProductionCode}: A metadata value representing if the record is produced by DR.</li>
 *     <li>{@link #title}: Includes the title of the record, which can also be in the klausuleringer-tabel.</li>
 * </ul>
 */
public class RestrictionsInputDTO {
    /**
     * The ID of the record this metadata is from.
     */
    public String recordId;

    /**
     * For DR records this is an internal DR ID, which might have been restricted by DR. This is checked in the klausuleringer-tabel.
     */
    public String drProductionId;

    /**
     * A metadata value representing if the record is produced by DR.
     */
    public String ownProductionCode;

    /**
     * Includes the title of the record, which can also be in the klausuleringer-tabel.
     */
    public String title;

    public RestrictionsInputDTO(String recordId, String drProductionId, String ownProductionCode, String title) {
        this.recordId = recordId;
        this.drProductionId = drProductionId;
        this.ownProductionCode = ownProductionCode;
        this.title = title;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getDrProductionId() {
        return drProductionId;
    }

    public void setDrProductionId(String drProductionId) {
        this.drProductionId = drProductionId;
    }

    public String getOwnProductionCode() {
        return ownProductionCode;
    }

    public void setOwnProductionCode(String ownProductionCode) {
        this.ownProductionCode = ownProductionCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
