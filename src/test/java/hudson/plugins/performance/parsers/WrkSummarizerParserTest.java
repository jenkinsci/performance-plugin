package hudson.plugins.performance.parsers;

import hudson.model.TaskListener;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.util.StreamTaskListener;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class WrkSummarizerParserTest {

    private WrkSummarizerParser parser;
    private TaskListener listener;

    @Before
    public void before() {
        parser = new WrkSummarizerParser(null);
        listener = new StreamTaskListener((java.io.OutputStream) System.out);
    }

    @Test
    public void testParseResultsWithMilliSecondResponseTimes() {
        List<File> files = new ArrayList<File>(1);
        files.add(new File(getClass().getResource("/WrkResultsQuick.wrk").getFile()));

        try {
            Collection<PerformanceReport> reports = parser.parse(null, files,
                    listener);
            assertFalse(reports.isEmpty());
            for (PerformanceReport report : reports) {
                // should not have average time >= 1s
                assertTrue(report.getAverage() < 1000);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParseResultsWithSecondResponseTimes() {
        List<File> files = new ArrayList<File>(1);
        files.add(new File(getClass().getResource("/WrkResultsLong.wrk").getFile()));

        try {
            Collection<PerformanceReport> reports = parser.parse(null, files, listener);
            assertFalse(reports.isEmpty());
            for (PerformanceReport report : reports) {
                // should have average time >= 1s
                assertTrue(report.getAverage() >= 1000);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParseWithLatencyDistributionBuckets() {
        List<File> files = new ArrayList<File>(1);
        files.add(new File(getClass().getResource("/WrkResultsWithLatencyFlag.wrk").getFile()));

        try {
            Collection<PerformanceReport> reports = parser.parse(null, files, listener);
            assertFalse(reports.isEmpty());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParseWithErrors() {
        List<File> files = new ArrayList<File>(1);
        files.add(new File(getClass().getResource("/WrkResultsWithErrors.wrk").getFile()));

        try {
            Collection<PerformanceReport> reports = parser.parse(null, files, listener);
            assertFalse(reports.isEmpty());

            // NOTE: uncomment once this is intentionally supported. Currently some
            //       refactoring is needed with summarized reports.
            // for(PerformanceReport report: reports) {
            //   assertTrue(report.countErrors() > 0);
            // }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParseTimeMeasurements() {
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
