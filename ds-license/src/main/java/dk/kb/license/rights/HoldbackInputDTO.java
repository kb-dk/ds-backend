package dk.kb.license.rights;

/**
 * This object contains all values that are needed for holdback calculation. It should always be instantiated and used from a {@link RightsInputDTO}. An overview of the values
 * follows:
 * <ul>
 *     <li>{@link #hensigt}: If the value is equal to 6000, then no lookups in the formaalsid-tabel is needed as the formaal_name in the holdback_from_formaalsid-tabel is then
 *     'Undervisning'.</li>
 *     <li>{@link #form}: A metadata value used to extract formaalsid when doing lookups in the formaalsid-tabel.</li>
 *     <li>{@link #indhold}: A metadata value used to extract formaalsid when doing lookups in the formaalsid-tabel.</li></li>
 *     <li>{@link #productionCountry}: A metadata value used to validate formaalsid values from the formaalsid-tabel. If formaalsid is '2.05' then this value is used to check if
 *     the formaalsid should be either '2.05.01' or '2.05.02'. If productionCountry is 1000 then the value of formaalsid gets set to '2.05.01' otherwise it gets set as '2.05.02'
 *     . In words this means that all records with formaalsid '2.05.01' are produced in Denmark, while the others aren't.</li>
 *     <li>{@link #productionCode}: A metadata value used to define if the record is produced by DR themselves. Values between 1000 and 3300 (both included) can be shown in the
 *     current setup. More information on productionCodes <a href="https://kb-dk.atlassian.net/wiki/spaces/DRAR/pages/40632339/Metadata">here</a>.</li>
 * </ul>
 */
public class HoldbackInputDTO {
    /**
     * If the value is equal to 6000, then no lookups in the formaalsid-tabel is needed as the formaal_name in the
     * holdback_from_formaalsid-tabel is then 'Undervisning'.
     */
    public int hensigt;

    /**
     * A metadata value used to extract formaalsid when doing lookups in the formaalsid-tabel.
     */
    public int form;

    /**
     * A metadata value used to extract formaalsid when doing lookups in the formaalsid-tabel.
     */
    public int indhold;

    /**
     * A metadata value used to validate formaalsid values from the formaalsid-tabel. If formaalsid is '2.05' then this value is used to check if
     * the formaalsid should be either '2.05.01' or '2.05.02'. If productionCountry is 1000 then the value of formaalsid gets set to '2.05.01' otherwise it gets set as '2.05.02'
     * . In words this means that all records with formaalsid '2.05.01' are produced in Denmark, while the others aren't.
     */
    public int productionCountry;

    /**
     * A metadata value used to define if the record is produced by DR themselves. Values between 1000 and 3300 (both included) can be shown in the
     * current setup. More information on productionCodes <a href="https://kb-dk.atlassian.net/wiki/spaces/DRAR/pages/40632339/Metadata">here</a>.
     */
    public String productionCode;

    public HoldbackInputDTO(int hensigt, int form, int indhold, int productionCountry, String productionCode) {
        this.hensigt = hensigt;
        this.form = form;
        this.indhold = indhold;
        this.productionCountry = productionCountry;
        this.productionCode = productionCode;
    }

    public int getHensigt() {
        return hensigt;
    }

    public void setHensigt(int hensigt) {
        this.hensigt = hensigt;
    }
    public void setHensigtFromString(String hensigt) {
        this.hensigt = Integer.parseInt(hensigt);
    }

    public int getForm() {
        return form;
    }

    public void setForm(int form) {
        this.form = form;
    }
    public void setFormFromString(String form) {
        this.form = Integer.parseInt(form);
    }

    public int getIndhold() {
        return indhold;
    }

    public void setIndhold(int indhold) {
        this.indhold = indhold;
    }
    public void setIndholdFromString(String indhold) {
        this.indhold = Integer.parseInt(indhold);
    }

    public int getProductionCountry() {
        return productionCountry;
    }

    public void setProductionCountry(int productionCountry) {
        this.productionCountry = productionCountry;
    }

    public void setProductionCountryFromString(String productionCountry) {
        this.productionCountry = Integer.parseInt(productionCountry);
    }

    public String getProductionCode() {
        return productionCode;
    }

    public void setProductionCode(String productionCode) {
        this.productionCode = productionCode;
    }

}
