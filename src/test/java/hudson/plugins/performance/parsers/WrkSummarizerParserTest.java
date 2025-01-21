package hudson.plugins.performance.parsers;

import hudson.model.TaskListener;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import hudson.util.StreamTaskListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrkSummarizerParserTest {

    private WrkSummarizerParser parser;
    private TaskListener listener;

    @BeforeEach
    void before() {
        parser = new WrkSummarizerParser(null, PerformanceReportTest.DEFAULT_PERCENTILES);
        listener = new StreamTaskListener((java.io.OutputStream) System.out, StandardCharsets.UTF_8);
    }

    @Test
    void testParseResultsWithMilliSecondResponseTimes() {
        List<File> files = new ArrayList<File>(1);
        files.add(new File(getClass().getResource("/WrkResultsQuick.wrk").getFile()));

        assertDoesNotThrow(() -> {
            Collection<PerformanceReport> reports = parser.parse(null, files,
                    listener);
            assertFalse(reports.isEmpty());
            for (PerformanceReport report : reports) {
                // should not have average time >= 1s
                assertTrue(report.getAverage() < 1000);
            }
        });
    }

    @Test
    void testParseResultsWithSecondResponseTimes() {
        List<File> files = new ArrayList<File>(1);
        files.add(new File(getClass().getResource("/WrkResultsLong.wrk").getFile()));

        assertDoesNotThrow(() -> {
            Collection<PerformanceReport> reports = parser.parse(null, files, listener);
            assertFalse(reports.isEmpty());
            for (PerformanceReport report : reports) {
                // should have average time >= 1s
                assertTrue(report.getAverage() >= 1000);
            }
        });
    }

    @Test
    void testParseWithLatencyDistributionBuckets() {
        List<File> files = new ArrayList<File>(1);
        files.add(new File(getClass().getResource("/WrkResultsWithLatencyFlag.wrk").getFile()));

        assertDoesNotThrow(() -> {
            Collection<PerformanceReport> reports = parser.parse(null, files, listener);
            assertFalse(reports.isEmpty());
        });
    }

    @Test
    void testParseWithErrors() {
        List<File> files = new ArrayList<File>(1);
        files.add(new File(getClass().getResource("/WrkResultsWithErrors.wrk").getFile()));

        assertDoesNotThrow(() -> {
            Collection<PerformanceReport> reports = parser.parse(null, files, listener);
            assertFalse(reports.isEmpty());

            // NOTE: uncomment once this is intentionally supported. Currently some
            //       refactoring is needed with summarized reports.
            // for(PerformanceReport report: reports) {
            //   assertTrue(report.countErrors() > 0);
            // }
        });
    }

    @Test
    void testParseTimeMeasurements() {
        // milliseconds
        assertEquals(5, parser.getTime("5ms", WrkSummarizerParser.TimeUnit.MILLISECOND));
        assertEquals(5000, parser.getTime("5s", WrkSummarizerParser.TimeUnit.MILLISECOND));
        assertEquals(5000 * 60, parser.getTime("5m", WrkSummarizerParser.TimeUnit.MILLISECOND));
        assertEquals(1000 * 60 * 60, parser.getTime("1h", WrkSummarizerParser.TimeUnit.MILLISECOND));

        // seconds
        assertEquals(1, parser.getTime("1005ms", WrkSummarizerParser.TimeUnit.SECOND));
        assertEquals(5, parser.getTime("5s", WrkSummarizerParser.TimeUnit.SECOND));
        assertEquals(5 * 60, parser.getTime("5m", WrkSummarizerParser.TimeUnit.SECOND));
        assertEquals(60 * 60, parser.getTime("1h", WrkSummarizerParser.TimeUnit.SECOND));

        // minute
        assertEquals(0, parser.getTime("5ms", WrkSummarizerParser.TimeUnit.MINUTE));
        assertEquals((int) Math.floor(5 / 60.0), parser.getTime("5s", WrkSummarizerParser.TimeUnit.MINUTE));
        assertEquals((int) Math.floor((5 * 60) / 60.0), parser.getTime("5m", WrkSummarizerParser.TimeUnit.MINUTE));
        assertEquals((int) Math.floor((60 * 60) / 60.0), parser.getTime("1h", WrkSummarizerParser.TimeUnit.MINUTE));

        // hour
        assertEquals(0, parser.getTime("5ms", WrkSummarizerParser.TimeUnit.HOUR));
        assertEquals((int) Math.floor(5 / (60.0 * 60.0)), parser.getTime("5s", WrkSummarizerParser.TimeUnit.HOUR));
        assertEquals((int) Math.floor((5 * 60) / (60.0 * 60.0)), parser.getTime("5m", WrkSummarizerParser.TimeUnit.HOUR));
        assertEquals((int) Math.floor((60 * 60) / (60.0 * 60.0)), parser.getTime("1h", WrkSummarizerParser.TimeUnit.HOUR));
    }
}
