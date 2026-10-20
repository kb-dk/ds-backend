package dk.kb.datahandler.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.storage.model.v1.RerunClusterRequestDto;
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
  protected static final int CONNECTION_POOL_SIZE = 3;

  @Container
  private static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:13.23").withDatabaseName("digisam")
          .withEnv("PGDATESTYLE", "ISO,DMY")
          .withInitScript("db.not_for_flyway/create_remote_rerun_clusters.sql");

  private static RerunClusterStorageForUnitTests rerunClusterStorage = null;

  @BeforeAll
  public static void beforeClass() throws Exception {
    ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml");
    RerunClusterStorage.initialize(postgres.getDriverClassName(), postgres.getJdbcUrl(),
        postgres.getUsername(), postgres.getPassword(), CONNECTION_POOL_SIZE);
    rerunClusterStorage = new RerunClusterStorageForUnitTests();
  }

  @BeforeEach
  public void beforeEach() throws SQLException {
    rerunClusterStorage.clearTables();
  }

  /**
   * The clusters table is empty and {@code created} is null, so the time filter is skipped.
   * Verifies that an empty list (not null) is returned.
   */
  @Test
  public void getRerunClusters_whenTableIsEmptyAndCreatedIsNull_thenReturnEmptyList()
      throws Exception {
    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(null);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(0, rerunClusterRequestDtoList.size());
  }

  /**
   * Two rows with different file_ids and {@code created} is null, so no time filtering happens.
   * Verifies that both rows are returned sorted by file_id ascending, regardless of insert order.
   */
  @Test
  public void getRerunClusters_whenCreatedIsNullAndFileIdsAreUnique_thenReturnAllRows()
      throws Exception {
    // Arrange
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
                                     """);
    }
    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(null);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(2, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("1000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("run 1", rerunClusterRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("run 1", rerunClusterRequestDtoList.get(1).getJobId());
  }

  /**
   * Three rows share the same file_id (history from run 1, 2 and 3) and {@code created} is null, so
   * no time filtering happens. Verifies that DISTINCT ON keeps only the newest row (run 3) for that
   * file_id.
   */
  @Test
  public void getRerunClusters_whenCreatedIsNullAndFileIdHasHistory_thenReturnLatestFileIdRow()
      throws Exception {
    // Arrange
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T07:23:40.638Z', 'run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-08T08:23:40.638Z', 'run 3');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(null);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(1, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("2000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("run 3", rerunClusterRequestDtoList.get(0).getJobId());
  }

  /**
   * Three rows share the same file_id (history from run 1, 2 and 3). {@code created}
   * (2026-07-07T00:00:00.000Z) filters out run 1, while run 2 and run 3 remain. Verifies that only
   * the newest remaining row (run 3) is returned for that file_id.
   */
  @Test
  public void getRerunClusters_whenFileIdHasHistory_thenReturnLatestFileIdRow() throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-07T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T07:23:40.638Z', 'run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-08T08:23:40.638Z', 'run 3');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(1, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("2000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("run 3", rerunClusterRequestDtoList.get(0).getJobId());
  }

  /**
   * Two rows with different file_ids, both created after {@code created}. Verifies that both rows
   * pass the time filter and are returned sorted by file_id ascending (0000a... before 1111a...),
   * regardless of insert order.
   */
  @Test
  public void getRerunClusters_whenCreatedIsBeforeInsertedRows_thenReturnAllRowsAfterCreated()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(2, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("run 1", rerunClusterRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("1000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("run 1", rerunClusterRequestDtoList.get(1).getJobId());
  }

  /**
   * Three rows with different file_ids. Two have created exactly equal to {@code created} and one
   * is newer. Verifies that the filter is inclusive ({@code >=}), so a job that failed midway can
   * resume from the same timestamp, and that all three rows are returned sorted by file_id
   * ascending (0000a..., 0000b..., 0000c...).
   */
  @Test
  public void getRerunClusters_whenCreatedMatchInsertedRowsCreated_thenReturnAllRowsAfterOrMatchingCreated()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-06T06:23:40.638Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T08:23:40.638Z', 'run 2');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(3, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("2000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-07T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("run 2", rerunClusterRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("1000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("run 1", rerunClusterRequestDtoList.get(1).getJobId());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getFileId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"),
        rerunClusterRequestDtoList.get(2).getCreated());
    assertEquals("run 1", rerunClusterRequestDtoList.get(2).getJobId());
  }

  /**
   * Three rows with different file_ids. Two are older than {@code created}
   * (2026-07-08T00:23:40.638Z) and one is newer. Verifies that the older rows are filtered out and
   * only the newer row (run 3) is returned.
   */
  @Test
  public void getRerunClusters_whenSomeRowsAreBeforeCreated_thenExcludeOlderRows()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-08T00:23:40.638Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T07:23:40.638Z', 'run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-08T08:23:40.638Z', 'run 3');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(1, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("2000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("run 3", rerunClusterRequestDtoList.get(0).getJobId());
  }

  /**
   * Five rows across three jobs. File_id 0000a... has history in run 1, 2 and 3, while 1111a...
   * (run 3) and 2222a... (run 2) appear only once. All rows are newer than {@code created}.
   * Verifies that only the newest row per file_id is returned (run 3 for 0000a...), and that the
   * result is sorted by file_id ascending: 0000a..., 1111a..., 2222a....
   */
  @Test
  public void getRerunClusters_whenFileIdHasHistoryAndUniqueFileIdRows_thenReturnNewestRowPerFileIdOrderedByFileIdAsc()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1000a00a-0aa0-000a-00a0-a0a000aa0aa0', '2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '2026-08-01T00:00:00.001Z', 'run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-08-01T00:00:00.002Z', 'run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('3000a00a-0aa0-000a-00a0-a0a000aa0aa0', '1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '0000d00d-0aa0-000a-00a0-a0a000aa0aa0', '2026-09-06T09:23:40.638Z', 'run 3');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('4000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000d00d-0aa0-000a-00a0-a0a000aa0aa0', '2026-09-06T09:23:40.638Z', 'run 3');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(created);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(3, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("4000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000d00d-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-09-06T09:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("run 3", rerunClusterRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("3000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("0000d00d-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-09-06T09:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("run 3", rerunClusterRequestDtoList.get(1).getJobId());

    assertEquals(UUID.fromString("1000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getId());
    assertEquals(UUID.fromString("2222a22a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getFileId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-08-01T00:00:00.001Z"),
        rerunClusterRequestDtoList.get(2).getCreated());
    assertEquals("run 2", rerunClusterRequestDtoList.get(2).getJobId());
  }
}
