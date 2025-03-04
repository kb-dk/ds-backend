package dk.kb.present.dr.restrictions;

import dk.kb.present.config.ServiceConfig;
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

public class DsIdLookup {
    private static final Logger log = LoggerFactory.getLogger(DsIdLookup.class);

    private static DsIdLookup idLookup = new DsIdLookup();


    /**
     * Set containing production IDs that cannot be shown to users.
     */
    private static Set<String> restrictedDsIds = new HashSet<>();

    DsIdLookup() {}

    public static void init() {
        loadRestrictedIdsFromFile();
    }
    public static synchronized DsIdLookup getInstance(){
        return idLookup;
    }


    /**
     * Check if an ID is present in {@link #restrictedDsIds} and return either true or false based on the result.
     * If the method returns true, the DS ID cannot be shown in the DR Archive.
     * @param id to perform lookup for.
     * @return either true or false based on the id being in the restrictedProductionIDs set.
     */
    public boolean doLookup(String id){
        return restrictedDsIds.contains(id);
    }


    /**
     * Load restricted production IDs from an Excel sheet defined in the configuration for ds-present.
     */
    private static void loadRestrictedIdsFromFile() {
        String restrictionsSheetPath = ServiceConfig.getConfig().getString("dr.restrictedDsIdsSheet");
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
                        restrictedDsIds.add(cell.getStringCellValue());
                    }
                }
            }
        } catch (IOException e) {
            log.error("The excel sheet, which should be present at path '{}', could not be loaded.", restrictionsSheetPath);
            throw new InternalServiceException(e);
        }

        if (restrictedDsIds.isEmpty()){
            log.error("No restricted DS IDs were loaded. This sounds like a good thing, however it should not happen. " +
                    "Please check that a list of actual IDs are provided in the configuration at YAML key: '{}'.", restrictionsSheetPath);
        }

        log.info("Loaded '{}' restricted DS IDs from file specified at YAML path: '{}'", restrictedDsIds.size(), restrictionsSheetPath);
    }



}
