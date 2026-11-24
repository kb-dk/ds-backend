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
  private static final int CONNECTION_POOL_SIZE = 3;
  private static final int LIMIT = 1000;

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
    rerunClusterStorage.clearTable();
  }

  /**
   * The table is empty and the cursor (created, id) is null, so the SQL defaults it to
   * ('-infinity', nil UUID). Verifies that an empty list (not null) is returned.
   */
  @Test
  public void getRerunClusters_whenTableIsEmptyAndCursorIsNull_thenReturnEmptyList()
      throws Exception {
    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(null, null, LIMIT);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(0, rerunClusterRequestDtoList.size());
  }

  /**
   * Two rows with different file_ids and the same created, and the cursor is null, so all rows are
   * after the cursor. Verifies that both rows are returned sorted by created, then id ascending,
   * regardless of insert order. Both rows share created, so the id decides the order.
   */
  @Test
  public void getRerunClusters_whenCursorIsNullAndFileIdsAreUnique_thenReturnAllRowsOrderedByCreatedAndIdAsc()
      throws Exception {
    // Arrange
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '1111c11c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '1111c11c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     """);
    }
    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(null, null, LIMIT);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(2, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("1111c11c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("test run 1", rerunClusterRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("1111c11c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("test run 1", rerunClusterRequestDtoList.get(1).getJobId());
  }

  /**
   * Three rows share the same file_id (history from test run 1, 2 and 3) and the cursor is null, so
   * all rows are after the cursor. Verifies that the NOT EXISTS check keeps only the newest row
   * (run 3) for that file_id.
   */
  @Test
  public void getRerunClusters_whenCursorIsNullAndFileIdHasHistory_thenReturnNewestRowForFileId()
      throws Exception {
    // Arrange
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T07:23:40.638Z', 'test run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-08T08:23:40.638Z', 'test run 3');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(null, null, LIMIT);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(1, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("2222b22b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("test run 3", rerunClusterRequestDtoList.get(0).getJobId());
  }

  /**
   * Three rows share the same file_id (history from test run 1, 2 and 3). The cursor excludes test
   * run 1, while run 2 and run 3 remain. Verifies that only the newest row (run 3) is returned for
   * that file_id.
   */
  @Test
  public void getRerunClusters_whenFileIdHasHistoryAfterCursor_thenReturnNewestRowForFileId()
      throws Exception {
    // Arrange
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
    OffsetDateTime created = OffsetDateTime.parse("2026-07-07T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T07:23:40.638Z', 'test run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-08T08:23:40.638Z', 'test run 3');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(id, created, LIMIT);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(1, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("test run 3", rerunClusterRequestDtoList.get(0).getJobId());
  }

  /**
   * Two rows with different file_ids and the same created, both after the cursor. Verifies that
   * both rows are returned sorted by created, then id ascending, regardless of insert order. Both
   * rows share created, so the id decides the order.
   */
  @Test
  public void getRerunClusters_whenCursorIsBeforeAllRows_thenReturnAllRowsOrderedByCreatedAndIdAsc()
      throws Exception {
    // Arrange
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '1111c11c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '1111c11c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(id, created, LIMIT);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(2, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("1111c11c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("test run 1", rerunClusterRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("1111c11c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("test run 1", rerunClusterRequestDtoList.get(1).getJobId());
  }

  /**
   * Three rows with different file_ids. Two have created exactly equal to the cursor's created and
   * one is newer. The cursor id is the nil UUID, the lowest possible UUID, so the filter acts as
   * created >= cursor created. Verifies that rows matching the cursor's created are included, so a
   * job that failed midway can resume from the same timestamp, and that all three rows are returned
   * sorted by created, then id ascending.
   */
  @Test
  public void getRerunClusters_whenCursorCreatedMatchesRowsAndCursorIdIsNil_thenIncludeMatchingRows()
      throws Exception {
    // Arrange
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
    OffsetDateTime created = OffsetDateTime.parse("2026-07-06T06:23:40.638Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T08:23:40.638Z', 'test run 2');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(id, created, LIMIT);

    // Assert
    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(3, rerunClusterRequestDtoList.size());

    // Matching created, lowest id first
    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("2222c22c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("test run 1", rerunClusterRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("2222a22a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("2222b22b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("2222c22c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("test run 1", rerunClusterRequestDtoList.get(1).getJobId());

    // Later created last
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-07T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(2).getCreated());
    assertEquals("test run 2", rerunClusterRequestDtoList.get(2).getJobId());
  }

  /**
   * Three rows with different file_ids. Two are older than the cursor and one is newer. Verifies
   * that the older rows are excluded and only the newer row (run 3) is returned.
   */
  @Test
  public void getRerunClusters_whenSomeRowsAreBeforeCursor_thenExcludeOlderRows() throws Exception {
    // Arrange
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
    OffsetDateTime created = OffsetDateTime.parse("2026-07-08T00:23:40.638Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T07:23:40.638Z', 'test run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-08T08:23:40.638Z', 'test run 3');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(id, created, LIMIT);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(1, rerunClusterRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("test run 3", rerunClusterRequestDtoList.get(0).getJobId());
  }

  /**
   * Five rows across three jobs. File_id 4444b44b... has history in test run 1, 2 and 3, while
   * 3333b33b... and 1111b11b... appear only once. All rows are after the cursor. Verifies that only
   * the newest row per file_id is returned (run 3 for 4444b44b...), and that the result is sorted
   * by created, then id ascending. Two of the returned rows share created, so the id decides their
   * order.
   */
  @Test
  public void getRerunClusters_whenFileIdHasHistoryAndUniqueFileIdRows_thenReturnNewestRowPerFileIdOrderedByCreatedAndIdAsc()
      throws Exception {
    // Arrange
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000000");
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('4444a44a-0aa0-000a-00a0-a0a000aa0aa0', '4444b44b-0aa0-000a-00a0-a0a000aa0aa0', '4444c44c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('3333a33a-0aa0-000a-00a0-a0a000aa0aa0', '3333b33b-0aa0-000a-00a0-a0a000aa0aa0', '4444c44c-0aa0-000a-00a0-a0a000aa0aa0', '2026-08-01T00:00:00.001Z', 'test run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '4444b44b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-08-01T00:00:00.002Z', 'test run 2');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '1111c11c-0aa0-000a-00a0-a0a000aa0aa0', '2026-09-06T09:23:40.638Z', 'test run 3');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '4444b44b-0aa0-000a-00a0-a0a000aa0aa0', '1111c11c-0aa0-000a-00a0-a0a000aa0aa0', '2026-09-06T09:23:40.638Z', 'test run 3');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(id, created, LIMIT);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(3, rerunClusterRequestDtoList.size());

    // Earliest created first
    assertEquals(UUID.fromString("3333a33a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("3333b33b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("4444c44c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-08-01T00:00:00.001Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("test run 2", rerunClusterRequestDtoList.get(0).getJobId());

    // Same created as the next row, so the lower id comes first
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("4444b44b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("1111c11c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-09-06T09:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("test run 3", rerunClusterRequestDtoList.get(1).getJobId());

    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getFileId());
    assertEquals(UUID.fromString("1111c11c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(2).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-09-06T09:23:40.638Z"),
        rerunClusterRequestDtoList.get(2).getCreated());
    assertEquals("test run 3", rerunClusterRequestDtoList.get(2).getJobId());
  }

  /**
   * Two rows share created with the cursor and one is newer. The cursor points at the row with the
   * lower id, as it does after a page or a restart. Verifies that the cursor row is excluded, while
   * the row with the same created but a higher id and the newer row are returned.
   */
  @Test
  public void getRerunClusters_whenCursorPointsAtExistingRow_thenReturnOnlyRowsAfterCursor()
      throws Exception {
    // Arrange
    UUID id = UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0");
    OffsetDateTime created = OffsetDateTime.parse("2026-07-06T06:23:40.638Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '1111c11c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T08:23:40.638Z', 'test run 2');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> rerunClusterRequestDtoList =
        rerunClusterStorage.getRerunClusters(id, created, LIMIT);

    // Assert
    assertNotNull(rerunClusterRequestDtoList);
    assertEquals(2, rerunClusterRequestDtoList.size());

    // Same created as the cursor, but a higher id
    assertEquals(UUID.fromString("2222a22a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("2222b22b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getFileId());
    assertEquals(UUID.fromString("2222c22c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"),
        rerunClusterRequestDtoList.get(0).getCreated());
    assertEquals("test run 1", rerunClusterRequestDtoList.get(0).getJobId());

    // Later created
    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        rerunClusterRequestDtoList.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-07T08:23:40.638Z"),
        rerunClusterRequestDtoList.get(1).getCreated());
    assertEquals("test run 2", rerunClusterRequestDtoList.get(1).getJobId());
  }

  /**
   * Four rows, three of them sharing the same created. Pages through them with a limit of 2, using
   * the last row of each page as the next cursor, as the facade does. Verifies that a created tie
   * across a page boundary neither skips nor repeats rows, and that paging ends with an empty
   * page.
   */
  @Test
  public void getRerunClusters_whenPagingWithLastRowAsCursor_thenReturnEachRowExactlyOnce()
      throws Exception {
    // Arrange
    int pageSize = 2;
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('3333a33a-0aa0-000a-00a0-a0a000aa0aa0', '3333b33b-0aa0-000a-00a0-a0a000aa0aa0', '3333c33c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', '2222c22c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', '1111c11c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO clusters (id, file_id, rerun_cluster_id, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', '0000c00c-0aa0-000a-00a0-a0a000aa0aa0', '2026-07-07T08:23:40.638Z', 'test run 2');
                                     """);
    }

    // Act
    List<RerunClusterRequestDto> firstPage =
        rerunClusterStorage.getRerunClusters(null, null, pageSize);
    RerunClusterRequestDto firstPageLast = firstPage.get(firstPage.size() - 1);

    List<RerunClusterRequestDto> secondPage =
        rerunClusterStorage.getRerunClusters(firstPageLast.getId(), firstPageLast.getCreated(),
            pageSize);
    RerunClusterRequestDto secondPageLast = secondPage.get(secondPage.size() - 1);

    List<RerunClusterRequestDto> thirdPage =
        rerunClusterStorage.getRerunClusters(secondPageLast.getId(), secondPageLast.getCreated(),
            pageSize);

    // Assert first page
    assertNotNull(firstPage);
    assertEquals(2, firstPage.size());

    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"), firstPage.get(0).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        firstPage.get(0).getFileId());
    assertEquals(UUID.fromString("1111c11c-0aa0-000a-00a0-a0a000aa0aa0"),
        firstPage.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"), firstPage.get(0).getCreated());
    assertEquals("test run 1", firstPage.get(0).getJobId());

    assertEquals(UUID.fromString("2222a22a-0aa0-000a-00a0-a0a000aa0aa0"), firstPage.get(1).getId());
    assertEquals(UUID.fromString("2222b22b-0aa0-000a-00a0-a0a000aa0aa0"),
        firstPage.get(1).getFileId());
    assertEquals(UUID.fromString("2222c22c-0aa0-000a-00a0-a0a000aa0aa0"),
        firstPage.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"), firstPage.get(1).getCreated());
    assertEquals("test run 1", firstPage.get(1).getJobId());

    // Assert second page
    assertNotNull(secondPage);
    assertEquals(2, secondPage.size());

    // Same created as the first page's last row, so it must not be skipped
    assertEquals(UUID.fromString("3333a33a-0aa0-000a-00a0-a0a000aa0aa0"),
        secondPage.get(0).getId());
    assertEquals(UUID.fromString("3333b33b-0aa0-000a-00a0-a0a000aa0aa0"),
        secondPage.get(0).getFileId());
    assertEquals(UUID.fromString("3333c33c-0aa0-000a-00a0-a0a000aa0aa0"),
        secondPage.get(0).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"), secondPage.get(0).getCreated());
    assertEquals("test run 1", secondPage.get(0).getJobId());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        secondPage.get(1).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        secondPage.get(1).getFileId());
    assertEquals(UUID.fromString("0000c00c-0aa0-000a-00a0-a0a000aa0aa0"),
        secondPage.get(1).getRerunClusterId());
    assertEquals(OffsetDateTime.parse("2026-07-07T08:23:40.638Z"), secondPage.get(1).getCreated());
    assertEquals("test run 2", secondPage.get(1).getJobId());

    // Assert third page
    assertNotNull(thirdPage);
    assertEquals(0, thirdPage.size());
  }
}
