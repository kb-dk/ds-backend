package dk.kb.present.dr.restrictions;

import dk.kb.present.config.ServiceConfig;
import dk.kb.present.dr.holdback.HoldbackDatePicker;
import dk.kb.util.Resolver;
import dk.kb.util.webservice.exception.InternalServiceException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * DR has provided restrictions for the DR Archive. This includes a restricted list of production IDs. If a record has a production ID, which is present in the backing list for
 * this class, the record can not be shown in the DR Archive.
 */
public class ProductionIdLookup {
    private static final Logger log = LoggerFactory.getLogger(ProductionIdLookup.class);

    private static ProductionIdLookup idLookup = new ProductionIdLookup();


    /**
     * Set containing production IDs that cannot be shown to users.
     */
    private static Set<String> restrictedProductionIds = new HashSet<>();

    ProductionIdLookup() {}

    public static void init() {
        loadRestrictedIdsFromFile();
    }
    public static synchronized ProductionIdLookup getInstance(){
        return idLookup;
    }


    /**
     * Check if an ID is present in {@link #restrictedProductionIds} and return either true or false based on the result.
     * If the method returns true, the production ID cannot be shown in the DR Archive.
     * @param id to perform lookup for.
     * @return either true or false based on the id being in the restrictedProductionIDs set.
     */
    public boolean doLookup(String id){
        return restrictedProductionIds.contains(id);
    }

    public long getAmountOfRestrictedIds() {
        return restrictedProductionIds.size();
    }


    /**
     * Load restricted production IDs from an Excel sheet defined in the configuration for ds-present.
     */
    private static void loadRestrictedIdsFromFile() {
        String restrictionsSheetPath = ServiceConfig.getConfig().getString("dr.restrictionSheet");
        Set<String> restrictedIds = new HashSet<>();

        // Read Excel sheet specified in configuration.
        try (FileInputStream fis = new FileInputStream(Resolver.resolveURL(restrictionsSheetPath).getPath());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            // TODO: Read the specific column of cells.
            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                if (cell != null) {
                    if (Objects.requireNonNull(cell.getCellType()) == CellType.STRING) {
                        restrictedProductionIds.add(reformatProductionId(cell.getStringCellValue()));
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        // The new entries in the document are formatted differently than the old ones
                        String formattedId = reformatProductionId(Integer.toString((int) cell.getNumericCellValue()));
                        restrictedProductionIds.add(formattedId);
                    }
                }
            }
        } catch (IOException e) {
            log.error("The excel sheet, which should be present at path '{}', could not be loaded.", restrictionsSheetPath);
            throw new InternalServiceException(e);
        }

        if (restrictedProductionIds.isEmpty()){
            log.error("No restricted production IDs were loaded. Keep in mind that this sounds like a good thing, however it should not happen. " +
                    "Please check that a list of actual IDs are provided in the configuration at YAML key: '{}'.", restrictionsSheetPath);
        }

        log.info("Loaded '{}' restricted production IDs from file specified at YAML path: '{}'", restrictedProductionIds.size(), restrictionsSheetPath);
    }


    /**
     * DR production IDs are different in nielsen/tvmeter data than in DRs own system. We need to reformat the values received from DR to match our metadata.
     * To do so we must remove prefixed zeros and prefix another zero.
     * @param productionId retrieved from DR provided Excel sheet.
     * @return productionId matching values in tvmeter and nielsen metadata.
     */
    private static String reformatProductionId(String productionId) {
        while (productionId.startsWith("0")) { //remove prefix zeroes
            productionId = productionId.substring(1);
        }

        // Some production IDs are on the correct formula already, as they are derived by hand in our system. therefore,
        // if an ID is 10 digits long an ends with two zeros, they are already correct.
        if (productionId.endsWith("00") && productionId.length() == 10){
            return productionId;
        }

        //add another zero
        return productionId + "0" ;
    }


}
