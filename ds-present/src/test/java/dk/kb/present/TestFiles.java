package dk.kb.present;

/**
 * This class contains references to test files on the classpath.
 * Records have the format COLLECTIONPREFIX_RECORD_IDENTIFIER, where COLLECTIONPREFIX specifies, where the collection
 * comes from and IDENTIFIER is the first part of the id of the record (Typically the first 8 characters).
 * <br/>
 * To fetch the content of a specific testfile resolve it through {@link dk.kb.util.Resolver#resolveUTF8String(String)}
 */
public class TestFiles {

    public static final String PVICA_RECORD_3006e2f8 = "internal_test_files/preservica7/3006e2f8-3f73-477a-a504-4d7cb1ae1e1c.xml";

    public static final String PVICA_RECORD_74e22fd8 = "internal_test_files/preservica7/74e22fd8-1268-4bcf-8a9f-22ca25379ea4.xml";
    public static final String PVICA_RECORD_a8afb121 = "internal_test_files/preservica7/a8afb121-e8b8-467a-8704-10dc42356ac4.xml";
    public static final String PVICA_RECORD_3945e2d1 = "internal_test_files/preservica7/3945e2d1-83a2-40d8-af1c-30f7b3b94390.xml";
    public static final String PVICA_RECORD_9d9785a8 = "internal_test_files/preservica7/9d9785a8-71f4-4b34-9a0e-1c99c13b001b.xml";
    public static final String PVICA_RECORD_c6fde2f4 = "internal_test_files/preservica7/c6fde2f4-036a-4e04-b83a-39a92021460b.xml";

    public static final String PVICA_RECORD_df3dc9cf = "internal_test_files/preservica7/df3dc9cf-43f6-4a8a-8909-de8b0fb7bd00.xml";
    public static final String PVICA_RECORD_a8aafb121 = "internal_test_files/preservica7/a8afb121-e8b8-467a-8704-10dc42356ac4.xml";
    public static final String PVICA_RECORD_4b18d02d = "internal_test_files/preservica7/4b18d02d-a421-4026-b522-66436a56bc0a.xml";

    // Preservica 5 manifestation
    public static final String PVICA_RECORD_33e30aa9 = "internal_test_files/preservica7/33e30aa9-d216-4216-aabf-b28d2b465215.xml";
    public static final String PVICA_RECORD_b346acc8 = "internal_test_files/preservica7//b346acc8-bcb2-41cd-bab1-be58bb0665e0.xml";
    public static final String PVICA_RECORD_e683b0b8 = "internal_test_files/preservica7/e683b0b8-425b-45aa-be86-78ac2b4ef0ca.xml";
    // White program radio metadata from 1966
    public static final String PVICA_RECORD_c295ae6c = "internal_test_files/preservica7/c295ae6c-fd34-4694-a9cc-4204b4a9f3a0.xml";

    public static final String PVICA_RECORD_0b3f6a54 = "internal_test_files/preservica7/0b3f6a54-befa-4471-95c0-78bcb1de6300.xml";

    // Empty record
    public static final String PVICA_EMPTY_RECORD = "internal_test_files/preservica7/emptyRecord.xml";
    public static final String PVICA_RECORD_4f706cda = "internal_test_files/preservica7/4f706cda-f474-46c8-824b-5a62ed5a8bee.xml";
    public static final String PVICA_RECORD_2973e7fa = "internal_test_files/preservica7/2973e7fa-0531-4c37-8b8d-5726c553b30b.xml";
    public static final String PVICA_RECORD_53ce4817 = "internal_test_files/preservica7/53ce4817-56ce-4e41-bac4-ba2dda938199.xml";

    // From preservica 6, not in preservica 7 stage
    public static final String PVICA6_RECORD_00a9e71c = "internal_test_files/preservica6/00a9e71c-1264-4e57-9238-b38ec5672fb2.xml";

    // not in stage
    public static final String PVICA7_RECORD_8946d31d = "internal_test_files/preservica6/8946d31d-a81c-447f-b84d-ff80644353d2.xml";
    public static final String CUMULUS_RECORD_05fea810 = "xml/copyright_extraction/05fea810-7181-11e0-82d7-002185371280.xml";
    public static final String CUMULUS_RECORD_3956d820 = "xml/copyright_extraction/3956d820-7b7d-11e6-b2b3-0016357f605f.xml";
    public static final String CUMULUS_RECORD_096c9090 = "xml/copyright_extraction/096c9090-717f-11e0-82d7-002185371280.xml";
    public static final String CUMULUS_RECORD_54b34b50 = "xml/copyright_extraction/54b34b50-2ce6-11ed-81b4-005056882ec3.xml";
    public static final String CUMULUS_RECORD_45dd4830 = "xml/copyright_extraction/45dd4830-717f-11e0-82d7-002185371280.xml";
    public static final String CUMULUS_RECORD_e3fcf020 = "xml/copyright_extraction/e3fcf020-85cb-11e8-8398-00505688346e.xml";
    public static final String CUMULUS_RECORD_e91341d0 = "xml/copyright_extraction/e91341d0-7184-11e0-82d7-002185371280.xml";
    public static final String CUMULUS_RECORD_DNF = "xml/copyright_extraction/DNF_1951-00352_00052.tif.xml";
    public static final String CUMULUS_RECORD_ANSK = "xml/copyright_extraction/ANSK_11614.tif.xml";
    public static final String CUMULUS_RECORD_FM = "xml/copyright_extraction/FM103703H.tif.xml";
    public static final String CUMULUS_RECORD_5cc1bea0 = "xml/copyright_extraction/5cc1bea0-71fa-11e2-b31c-0016357f605f.xml";
    public static final String CUMULUS_RECORD_09222b40 = "xml/copyright_extraction/09222b40-dba1-11e5-9785-0016357f605f.xml";
    public static final String CUMULUS_RECORD_e2519ce0 = "xml/copyright_extraction/e2519ce0-9fb0-11e8-8891-00505688346e.xml";
    public static final String CUMULUS_RECORD_26d4dd60 ="xml/copyright_extraction/26d4dd60-6708-11e2-b40f-0016357f605f.xml";
    public static final String CUMULUS_RECORD_9c17a440 = "xml/copyright_extraction/9c17a440-fe1a-11e8-9044-00505688346e.xml";
    public static final String CUMULUS_RECORD_3b03aa00 = "xml/copyright_extraction/3b03aa00-fee2-11e8-ab76-00505688346e.xml";
    public static final String CUMULUS_RECORD_25461fb0 = "xml/copyright_extraction/25461fb0-f664-11e0-9d29-0016357f605f.xml";
    public static final String CUMULUS_RECORD_770379f0 = "xml/copyright_extraction/770379f0-8a0d-11e1-805f-0016357f605f.xml";
    public static final String CUMULUS_RECORD_40221e30 = "xml/copyright_extraction/40221e30-1414-11e9-8fb8-00505688346e.xml";
    public static final String CUMULUS_RECORD_e5a0e980 = "xml/copyright_extraction/e5a0e980-d6cb-11e3-8d2e-0016357f605f.xml";
    public static final String CUMULUS_RECORD_f4668ad0 = "xml/copyright_extraction/f4668ad0-f334-11e8-b74f-00505688346e.xml";
    public static final String CUMULUS_RECORD_aaf3b130 = "xml/copyright_extraction/aaf3b130-e6e7-11e6-bdbe-00505688346e.xml";
    public static final String CUMULUS_RECORD_8e608940 = "xml/copyright_extraction/8e608940-d6db-11e3-8d2e-0016357f605f.xml";
    public static final String CUMULUS_RECORD_0c02aa10 = "xml/copyright_extraction/0c02aa10-b657-11e6-aedf-00505688346e.xml";
    public static final String CUMULUS_RECORD_226d41a0 = "xml/copyright_extraction/226d41a0-5a83-11e6-8b8d-0016357f605f.xml";

}
