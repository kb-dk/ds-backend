package dk.kb.datahandler.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.kb.datahandler.model.v1.OaiTargetDto;
import dk.kb.util.Resolver;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceConfigOaiTargetSearchTest {

    @Test
    void searchBlockIsLoadedWithLists() throws IOException {
        Path knownFile = Path.of(Resolver.resolveURL("logback-test.xml").getPath());
        String projectRoot = knownFile.getParent().getParent().getParent().toString();
        String yamlFile = projectRoot + "/conf/ds-datahandler-behaviour.yaml";

        String yaml = Files.readString(Path.of(yamlFile), StandardCharsets.UTF_8).replace(
                "    filter: dr\n",
                """
                    filter: dr
                    parallel: 3
                    search:
                      apiUrl: https://example.com
                      user: api-user
                      query: >-
                        {"fields": [
                          {"name": "pbcore.PBchannel", "values": ["dr*"]},
                          {"name": "pbcore.PBformatmedia", "values": ["Moving Image", "Sound"]}
                        ]}
                """);
        Path searchYaml = Files.createTempFile("ds-datahandler-search-", ".yaml");
        try {
            Files.writeString(searchYaml, yaml, StandardCharsets.UTF_8);
            ServiceConfig.initialize(searchYaml.toString());

            OaiTargetDto target = ServiceConfig.getOaiTargets().get("ds.radiotv");
            assertEquals(3, target.getParallel());
            assertEquals("https://example.com", target.getSearch().getApiUrl());
            assertEquals("api-user", target.getSearch().getUsername());
            assertNull(target.getSearch().getPassword());
            JsonNode query = new ObjectMapper().readTree(target.getSearch().getQuery());
            assertEquals("Sound", query.path("fields").get(1).path("values").get(1).asText());
            assertEquals(1000, target.getSearch().getPageSize());
        } finally {
            Files.deleteIfExists(searchYaml);
            ServiceConfig.initialize(yamlFile);
        }
        assertNull(ServiceConfig.getOaiTargets().get("ds.radiotv").getSearch());
        assertEquals(8, ServiceConfig.getOaiTargets().get("ds.radiotv").getParallel());
    }
}
