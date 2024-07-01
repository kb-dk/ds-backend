package dk.kb.present;

/**
 * Data transfer object which contains holdback information for a preservica record.
 * The object contains the following values:
 * <ul>
 *     <li>holdbackDate: Until this date, the record is not available.</li>
 *     <li>holdbackPurposeName: The name of the purpose. Purpose is a term from DR, in danish 'Formål' used to specify
 *                              the kind of material.</li>
 * </ul>
 */
public class HoldbackDTO {
    private String holdbackDate;
    private String holdbackPurposeName;

    public String getHoldbackDate() {
        return holdbackDate;
    }

    public void setHoldbackDate(String holdbackDate) {
        this.holdbackDate = holdbackDate;
    }

    public String getHoldbackPurposeName() {
        return holdbackPurposeName;
    }

    public void setHoldbackPurposeName(String holdbackPurposeName) {
        this.holdbackPurposeName = holdbackPurposeName;
    }
}
