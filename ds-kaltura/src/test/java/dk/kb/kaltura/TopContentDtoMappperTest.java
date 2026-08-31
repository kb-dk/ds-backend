package dk.kb.kaltura;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import dk.kb.kaltura.domain.ReportTableDto;
import dk.kb.kaltura.domain.TopContentDto;
import dk.kb.kaltura.mapper.TopContentDtoMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TopContentDtoMappperTest {

    private static final Logger log = LoggerFactory.getLogger(KalturaApiIntegrationTest.class);

    // Column order matches the CSV header used by every test below.
    private static final String HEADER =
            "object_id,entry_name,count_plays,sum_time_viewed,avg_time_viewed,count_loads," +
                    "load_play_ratio,avg_view_drop_off,unique_known_users,sum_view_period," +
                    "avg_view_period_time,avg_completion_rate";

    /**
     * Builds a single semicolon-terminated CSV data row using named parameters,
     * so each test can express exactly which field it's exercising instead of
     * relying on a long, unlabeled comma-separated string.
     */
    private static String row(String objectId, String entryName, String countPlays, String sumTimeViewed,
                              String avgTimeViewed, String countLoads, String loadPlayRatio,
                              String avgViewDropOff, String uniqueKnownUsers, String sumViewPeriod,
                              String avgViewPeriodTime, String avgCompletionRate) {
        return String.join(",",
                objectId, entryName, countPlays, sumTimeViewed, avgTimeViewed, countLoads,
                loadPlayRatio, avgViewDropOff, uniqueKnownUsers, sumViewPeriod,
                avgViewPeriodTime, avgCompletionRate) + ";";
    }

    /**
     * Convenience overload for the common case of a fully valid, realistic row.
     */
    private static String validRow(String objectId, String entryName) {
        return row(objectId, entryName,
                /* count_plays          */ "100",
                /* sum_time_viewed      */ "5.5",
                /* avg_time_viewed      */ "4.4",
                /* count_loads          */ "200",
                /* load_play_ratio      */ "0.8",
                /* avg_view_drop_off    */ "0.2",
                /* unique_known_users   */ "2",
                /* sum_view_period      */ "1400.1",
                /* avg_view_period_time */ "6.1",
                /* avg_completion_rate  */ "22.1");
    }

    @Test
    public void map_whenValidReportTableDto_thenReturnsMappedList() throws Exception {
        String data = validRow("0_testId", "Test Title");
        int totalCount = 1;
        ReportTableDto reportTableDto = new ReportTableDto(HEADER, data, totalCount);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        List<TopContentDto> topContentDtoList = topContentDtoMapper.map(reportTableDto);

        assertEquals(totalCount, topContentDtoList.size());
    }

    @Test
    public void map_whenHeaderAndDataAreNull_thenReturnsEmptyList() throws Exception {
        ReportTableDto reportTableDto = new ReportTableDto(null, null, 0);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        List<TopContentDto> topContentDtoList = topContentDtoMapper.map(reportTableDto);

        assertTrue(topContentDtoList.isEmpty());
    }

    @Test
    public void map_whenHeaderHasUnexpectedExtraColumn_thenReturnsMappedList() throws Exception {
        String header = "UNEXPECTED_EXTRA_COL," + HEADER;
        String data = "UNEXPECTED_EXTRA_COL_DATA," + validRow("0_testId", "Test Title");
        int totalCount = 1;
        ReportTableDto reportTableDto = new ReportTableDto(header, data, totalCount);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        List<TopContentDto> topContentDtoList = topContentDtoMapper.map(reportTableDto);

        assertEquals(totalCount, topContentDtoList.size());
        assertEquals("0_testId", topContentDtoList.get(0).getObject_id());
        assertEquals("Test Title", topContentDtoList.get(0).getEntry_name());
    }

    @Test
    public void map_whenMultipleDataRows_thenReturnsAllMappedEntries() throws Exception {
        String data = validRow("0_testId", "Test Title")
                + validRow("0_testId2", "Test Title 2");
        int totalCount = 2;
        ReportTableDto reportTableDto = new ReportTableDto(HEADER, data, totalCount);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        List<TopContentDto> topContentDtoList = topContentDtoMapper.map(reportTableDto);

        assertEquals(totalCount, topContentDtoList.size());
    }

    @Test
    public void map_whenHeaderIsEmptyString_thenReturnsEmptyList() throws Exception {
        ReportTableDto reportTableDto = new ReportTableDto(null, null, 0);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        List<TopContentDto> topContentDtoList = topContentDtoMapper.map(reportTableDto);

        assertTrue(topContentDtoList.isEmpty());
    }

    @Test
    public void map_whenReportTableDtoIsNull_thenThrowsException() {
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        assertThrows(NullPointerException.class, () -> topContentDtoMapper.map(null));
    }

    @Test
    public void map_whenDataRowHasFewerColumnsThanHeader_thenThrowsException() {
        // Only object_id, entry_name and count_plays are present; the remaining
        // nine columns declared in HEADER are missing from this row.
        String data = "0_testId,Test Title,2000;";
        int totalCount = 1;
        ReportTableDto reportTableDto = new ReportTableDto(HEADER, data, totalCount);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        assertThrows(Exception.class, () -> topContentDtoMapper.map(reportTableDto));
    }

    @Test
    public void map_whenDataRowHasEmptyValueInStringColumn_thenReturnsDtoWithEmptyString() throws Exception {
        // entry_name left blank on purpose to test empty-string handling.
        String data = validRow("0_testId", "");
        int totalCount = 1;
        ReportTableDto reportTableDto = new ReportTableDto(HEADER, data, totalCount);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        List<TopContentDto> result = topContentDtoMapper.map(reportTableDto);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getEntry_name());
    }

    @Test
    public void map_whenNumericFieldIsNonNumeric_thenThrowsException() {
        // count_plays set to a non-numeric value to trigger a type-coercion failure.
        String data = row("0_testId", "Test Title", "NOT_A_NUMBER", "17652.233333333",
                "7.7797414426326", "3892", "0.58299075025694", "0.26906126046717",
                "2", "14957.166666667", "6.5919641545468", "22.929925077126");
        int totalCount = 1;
        ReportTableDto reportTableDto = new ReportTableDto(HEADER, data, totalCount);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        assertThrows(InvalidFormatException.class, () -> topContentDtoMapper.map(reportTableDto));
    }

    @Test
    public void map_whenTotalCountDoesNotMatchActualRowCount_thenReturnsActualParsedRows() throws Exception {
        String data = validRow("0_testId", "Test Title");
        int totalCount = 5; // deliberately wrong; only one row is actually present
        ReportTableDto reportTableDto = new ReportTableDto(HEADER, data, totalCount);
        TopContentDtoMapper topContentDtoMapper = new TopContentDtoMapper();

        List<TopContentDto> topContentDtoList = topContentDtoMapper.map(reportTableDto);

        assertEquals(1, topContentDtoList.size());
    }
}