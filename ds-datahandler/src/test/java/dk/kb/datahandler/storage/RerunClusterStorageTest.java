package dk.kb.datahandler.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.storage.model.v1.RerunClusterDto;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
public class RerunClusterStorageTest {

  @Container
  private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:13.23")
      .withDatabaseName("digisam")
      .withEnv("PGDATESTYLE", "ISO,DMY")
      .withInitScript("db.not_for_flyway/create_remote_rerun_clusters.sql");

  private static RerunClusterStorageForUnitTests rerunClusterStorage = null;

  @BeforeAll
  public static void beforeClass() throws Exception {
    ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml");
    RerunClusterStorage.initialize(postgres.getDriverClassName(), postgres.getJdbcUrl(),
        postgres.getUsername(), postgres.getPassword(), 10);
    rerunClusterStorage = new RerunClusterStorageForUnitTests();
  }

  @BeforeEach
  public void beforeEach() throws SQLException {
    rerunClusterStorage.clearTables();
  }

  @Test
  public void getRerunClusters_whenCreatedIsNullAndNoRows_thenReturnRerunClusters()
      throws Exception {
    // Act
    List<RerunClusterDto> rerunClusterDtoList = rerunClusterStorage.getRerunClusters(null);

    // Assert
    assertNotNull(rerunClusterDtoList);
    assertEquals(0, rerunClusterDtoList.size());
  }

  @Test
  public void getRerunClusters_whenCreatedIsNullAndTableIsPopulated_thenReturnRerunClusters()
      throws Exception {
    // Arrange
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute(
          """
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
          """
      );
    }
    // Act
    List<RerunClusterDto> rerunClusterDtoList = rerunClusterStorage.getRerunClusters(null);

    // Assert
    assertNotNull(rerunClusterDtoList);
    assertEquals(2, rerunClusterDtoList.size());
  }

  @Test
  public void getRerunClusters_whenCreatedIsOlderThanLatestInsertedRows_thenReturnRerunClusters()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute(
          """
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
          """
      );
    }

    // Act
    List<RerunClusterDto> rerunClusterDtoList = rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterDtoList);
    assertEquals(2, rerunClusterDtoList.size());
  }

  @Test
  public void getRerunClusters_whenMultipleRowsOfSameFileId_thenOnlyReturnTheLatestRerunCluster()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute(
          """
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'run 1');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T07:23:40.638Z', 'run 2');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-08T08:23:40.638Z', 'run 3');
          """
      );
    }

    // Act
    List<RerunClusterDto> rerunClusterDtoList = rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterDtoList);
    assertEquals(1, rerunClusterDtoList.size());
  }

  @Test
  public void getRerunClusters_whenLatestCreatedIsAfterSomeRows_thenOnlyReturnTheLatestRerunCluster()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-08T00:23:40.638Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute(
          """
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'run 1');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T07:23:40.638Z', 'run 2');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-08T08:23:40.638Z', 'run 3');
          """
      );
    }

    // Act
    List<RerunClusterDto> rerunClusterDtoList = rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterDtoList);
    assertEquals(1, rerunClusterDtoList.size());
  }

  @Test
  public void getRerunClusters_whenMultipleJobsHasInsertedRowsWhereSomeOfThemIsTheSameFileId_thenOrderIsGettingUniqueLatestFileId()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute(
          """
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-08-01T00:00:00.001Z', 'run 2');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-08-01T00:00:00.002Z', 'run 2');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('3000a00a-0aa0-000a-00a0-a0a000aa0aa0', '2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-09-06T09:23:40.638Z', 'run 3');
          INSERT INTO cluster (id, file_id, rerun_cluster_id, created, job_id) VALUES ('4000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-09-06T09:23:40.638Z', 'run 3');
          """
      );
    }

    // Act
    List<RerunClusterDto> rerunClusterDtoList = rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterDtoList);
    assertEquals(3, rerunClusterDtoList.size());

    assertEquals(UUID.fromString("4000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterDtoList.get(0).getFileId());
    assertEquals(OffsetDateTime.parse("2026-09-06T09:23:40.638Z"),
        rerunClusterDtoList.get(0).getCreated());

    assertEquals(UUID.fromString("1000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterDtoList.get(1).getFileId());
    assertEquals(OffsetDateTime.parse("2026-08-01T00:00:00.001Z"),
        rerunClusterDtoList.get(1).getCreated());

    assertEquals(UUID.fromString("3000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterDtoList.get(2).getId());
    assertEquals(UUID.fromString("2222a22a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterDtoList.get(2).getFileId());
    assertEquals(OffsetDateTime.parse("2026-09-06T09:23:40.638Z"),
        rerunClusterDtoList.get(2).getCreated());
  }
}
