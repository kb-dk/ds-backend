package dk.kb.datahandler.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dk.kb.datahandler.config.ServiceConfig;
import dk.kb.storage.model.v1.SummaryRequestDto;
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
public class SummaryStorageTest {
  protected static final int CONNECTION_POOL_SIZE = 3;

  @Container
  private static final PostgreSQLContainer postgres =
      new PostgreSQLContainer("postgres:13.23").withDatabaseName("digisam")
          .withEnv("PGDATESTYLE", "ISO,DMY")
          .withInitScript("db.not_for_flyway/create_remote_summaries.sql");

  private static SummaryStorageForUnitTests summaryStorage = null;

  @BeforeAll
  public static void beforeClass() throws Exception {
    ServiceConfig.initialize("conf/ds-datahandler-behaviour.yaml");
    SummaryStorage.initialize(postgres.getDriverClassName(), postgres.getJdbcUrl(),
        postgres.getUsername(), postgres.getPassword(), CONNECTION_POOL_SIZE);
    summaryStorage = new SummaryStorageForUnitTests();
  }

  @BeforeEach
  public void beforeEach() throws SQLException {
    summaryStorage.clearTable();
  }

  /**
   * The summaries table is empty and {@code created} is null, so the time filter is skipped.
   * Verifies that an empty list (not null) is returned.
   */
  @Test
  public void getSummaries_whenTableIsEmptyAndCreatedIsNull_thenReturnEmptyList() throws Exception {
    // Act
    List<SummaryRequestDto> summaryRequestDtoList = summaryStorage.getSummaries(null);

    // Assert
    assertNotNull(summaryRequestDtoList);
    assertEquals(0, summaryRequestDtoList.size());
  }

  /**
   * Two rows with different file_ids and {@code created} is null, so no time filtering happens.
   * Verifies that both rows are returned sorted by file_id ascending, regardless of insert order.
   */
  @Test
  public void getSummaries_whenCreatedIsNullAndFileIdsAreUnique_thenReturnAllRows()
      throws Exception {
    // Arrange
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 1.', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 0.', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     """);
    }
    // Act
    List<SummaryRequestDto> summaryRequestDtoList = summaryStorage.getSummaries(null);

    // Assert
    assertNotNull(summaryRequestDtoList);
    assertEquals(2, summaryRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getFileId());
    assertEquals("Fin beskrivelse 0.", summaryRequestDtoList.get(0).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        summaryRequestDtoList.get(0).getCreated());
    assertEquals("test run 1", summaryRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(1).getFileId());
    assertEquals("Fin beskrivelse 1.", summaryRequestDtoList.get(1).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        summaryRequestDtoList.get(1).getCreated());
    assertEquals("test run 1", summaryRequestDtoList.get(1).getJobId());
  }

  /**
   * Three rows share the same file_id (history from test run 1, 2 and 3) and {@code created} is null, so
   * no time filtering happens. Verifies that DISTINCT ON keeps only the newest row (run 3) for that
   * file_id.
   */
  @Test
  public void getSummaries_whenCreatedIsNullAndFileIdHasHistory_thenReturnLatestFileIdRow()
      throws Exception {
    // Arrange
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 2.', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 1.', '2026-07-07T07:23:40.638Z', 'test run 2');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 0.', '2026-07-08T08:23:40.638Z', 'test run 3');
                                     """);
    }

    // Act
    List<SummaryRequestDto> summaryRequestDtoList = summaryStorage.getSummaries(null);

    // Assert
    assertNotNull(summaryRequestDtoList);
    assertEquals(1, summaryRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("2222b22b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getFileId());
    assertEquals("Fin beskrivelse 0.", summaryRequestDtoList.get(0).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        summaryRequestDtoList.get(0).getCreated());
    assertEquals("test run 3", summaryRequestDtoList.get(0).getJobId());
  }

  /**
   * Three rows share the same file_id (history from test run 1, 2 and 3). {@code created}
   * (2026-07-07T00:00:00.000Z) filters out test run 1, while run 2 and run 3 remain. Verifies that only
   * the newest remaining row (run 3) is returned for that file_id.
   */
  @Test
  public void getSummaries_whenFileIdHasHistory_thenReturnLatestFileIdRow() throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-07T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 2.', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 1.', '2026-07-07T07:23:40.638Z', 'test run 2');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 0.', '2026-07-08T08:23:40.638Z', 'test run 3');
                                     """);
    }

    // Act
    List<SummaryRequestDto> summaryRequestDtoList = summaryStorage.getSummaries(created);

    // Assert
    assertNotNull(summaryRequestDtoList);
    assertEquals(1, summaryRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getFileId());
    assertEquals("Fin beskrivelse 0.", summaryRequestDtoList.get(0).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        summaryRequestDtoList.get(0).getCreated());
    assertEquals("test run 3", summaryRequestDtoList.get(0).getJobId());
  }

  /**
   * Two rows with different file_ids, both created after {@code created}. Verifies that both rows
   * pass the time filter and are returned sorted by file_id ascending, regardless of insert order.
   */
  @Test
  public void getSummaries_whenCreatedIsBeforeInsertedRows_thenReturnAllRowsAfterCreated()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 1.', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 0.', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     """);
    }

    // Act
    List<SummaryRequestDto> summaryRequestDtoList = summaryStorage.getSummaries(created);

    // Assert
    assertNotNull(summaryRequestDtoList);
    assertEquals(2, summaryRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getFileId());
    assertEquals("Fin beskrivelse 0.", summaryRequestDtoList.get(0).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        summaryRequestDtoList.get(0).getCreated());
    assertEquals("test run 1", summaryRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(1).getFileId());
    assertEquals("Fin beskrivelse 1.", summaryRequestDtoList.get(1).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-06T07:23:40.638Z"),
        summaryRequestDtoList.get(1).getCreated());
    assertEquals("test run 1", summaryRequestDtoList.get(1).getJobId());
  }

  /**
   * Three rows with different file_ids. Two have created exactly equal to {@code created} and one
   * is newer. Verifies that the filter is inclusive ({@code >=}), so a job that failed midway can
   * resume from the same timestamp, and that all three rows are returned sorted by file_id
   * ascending.
   */
  @Test
  public void getSummaries_whenCreatedMatchInsertedRowsCreated_thenReturnAllRowsAfterOrMatchingCreated()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-06T06:23:40.638Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 2.', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 1.', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 0.', '2026-07-07T08:23:40.638Z', 'test run 2');
                                     """);
    }

    // Act
    List<SummaryRequestDto> summaryRequestDtoList = summaryStorage.getSummaries(created);

    // Assert
    assertNotNull(summaryRequestDtoList);
    assertEquals(3, summaryRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getFileId());
    assertEquals("Fin beskrivelse 0.", summaryRequestDtoList.get(0).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-07T08:23:40.638Z"),
        summaryRequestDtoList.get(0).getCreated());
    assertEquals("test run 2", summaryRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(1).getFileId());
    assertEquals("Fin beskrivelse 1.", summaryRequestDtoList.get(1).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"),
        summaryRequestDtoList.get(1).getCreated());
    assertEquals("test run 1", summaryRequestDtoList.get(1).getJobId());

    assertEquals(UUID.fromString("2222a22a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(2).getId());
    assertEquals(UUID.fromString("2222b22b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(2).getFileId());
    assertEquals("Fin beskrivelse 2.", summaryRequestDtoList.get(2).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-06T06:23:40.638Z"),
        summaryRequestDtoList.get(2).getCreated());
    assertEquals("test run 1", summaryRequestDtoList.get(2).getJobId());
  }

  /**
   * Three rows with different file_ids. Two are older than {@code created}
   * (2026-07-08T00:23:40.638Z) and one is newer. Verifies that the older rows are filtered out and
   * only the newer row (run 3) is returned.
   */
  @Test
  public void getSummaries_whenSomeRowsAreBeforeCreated_thenExcludeOlderRows() throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-08T00:23:40.638Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '2222b22b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 2.', '2026-07-06T06:23:40.638Z', 'test run 1');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 1.', '2026-07-07T07:23:40.638Z', 'test run 2');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '0000b00b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 0.', '2026-07-08T08:23:40.638Z', 'test run 3');
                                     """);
    }

    // Act
    List<SummaryRequestDto> summaryRequestDtoList = summaryStorage.getSummaries(created);

    // Assert
    assertNotNull(summaryRequestDtoList);
    assertEquals(1, summaryRequestDtoList.size());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("0000b00b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getFileId());
    assertEquals("Fin beskrivelse 0.", summaryRequestDtoList.get(0).getSummary());
    assertEquals(OffsetDateTime.parse("2026-07-08T08:23:40.638Z"),
        summaryRequestDtoList.get(0).getCreated());
    assertEquals("test run 3", summaryRequestDtoList.get(0).getJobId());
  }

  /**
   * Five rows across three jobs. File_id 4444b44b... has history in test run 1, 2 and 3, while
   * 3333b33b... and 1111b11b... appear only once. All rows are newer than {@code created}. Verifies
   * that only the newest row per file_id is returned (run 3 for 4444b44b...), and that the result
   * is sorted by file_id ascending.
   */
  @Test
  public void getSummaries_whenFileIdHasHistoryAndUniqueFileIdRows_thenReturnNewestRowPerFileIdOrderedByFileIdAsc()
      throws Exception {
    // Arrange
    OffsetDateTime created = OffsetDateTime.parse("2026-07-01T00:00:00.000Z");
    try (Connection conn = postgres.createConnection("")) {
      conn.createStatement().execute("""
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('4444a44a-0aa0-000a-00a0-a0a000aa0aa0', '4444b44b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 4.', '2026-07-06T07:23:40.638Z', 'test run 1');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('3333a33a-0aa0-000a-00a0-a0a000aa0aa0', '3333b33b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 3.', '2026-08-01T00:00:00.001Z', 'test run 2');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('2222a22a-0aa0-000a-00a0-a0a000aa0aa0', '4444b44b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 2.', '2026-08-01T00:00:00.002Z', 'test run 2');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('1111a11a-0aa0-000a-00a0-a0a000aa0aa0', '1111b11b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 1.', '2026-09-06T09:23:40.638Z', 'test run 3');
                                     INSERT INTO summaries (id, file_id, summary, created, job_id) VALUES ('0000a00a-0aa0-000a-00a0-a0a000aa0aa0', '4444b44b-0aa0-000a-00a0-a0a000aa0aa0', 'Fin beskrivelse 0.', '2026-09-06T09:23:40.638Z', 'test run 3');
                                     """);
    }

    // Act
    List<SummaryRequestDto> summaryRequestDtoList = summaryStorage.getSummaries(created);

    // Assert
    assertNotNull(summaryRequestDtoList);
    assertEquals(3, summaryRequestDtoList.size());

    assertEquals(UUID.fromString("1111a11a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getId());
    assertEquals(UUID.fromString("1111b11b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(0).getFileId());
    assertEquals("Fin beskrivelse 1.", summaryRequestDtoList.get(0).getSummary());
    assertEquals(OffsetDateTime.parse("2026-09-06T09:23:40.638Z"),
        summaryRequestDtoList.get(0).getCreated());
    assertEquals("test run 3", summaryRequestDtoList.get(0).getJobId());

    assertEquals(UUID.fromString("3333a33a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(1).getId());
    assertEquals(UUID.fromString("3333b33b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(1).getFileId());
    assertEquals("Fin beskrivelse 3.", summaryRequestDtoList.get(1).getSummary());
    assertEquals(OffsetDateTime.parse("2026-08-01T00:00:00.001Z"),
        summaryRequestDtoList.get(1).getCreated());
    assertEquals("test run 2", summaryRequestDtoList.get(1).getJobId());

    assertEquals(UUID.fromString("0000a00a-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(2).getId());
    assertEquals(UUID.fromString("4444b44b-0aa0-000a-00a0-a0a000aa0aa0"),
        summaryRequestDtoList.get(2).getFileId());
    assertEquals("Fin beskrivelse 0.", summaryRequestDtoList.get(2).getSummary());
    assertEquals(OffsetDateTime.parse("2026-09-06T09:23:40.638Z"),
        summaryRequestDtoList.get(2).getCreated());
    assertEquals("test run 3", summaryRequestDtoList.get(2).getJobId());
  }
}
