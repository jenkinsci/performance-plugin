package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class LocustParserTest {
    final static String FILE_NAME = "test_results_requests.csv";
    File requestReportFile;
    LocustParser locustParser;
    PerformanceReport report;

    @Before
    public void setUp() throws Exception {
        requestReportFile = new File(getClass().getResource(String.format("/%s", FILE_NAME)).toURI());
        locustParser = new LocustParser(null, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL);
        report = locustParser.parse(requestReportFile);
    }

    @Test
    public void shouldCreateParser() throws Exception {
        assertNotNull(locustParser);
    }

    @Test
    public void parserShouldReturnGlobPattern() throws Exception {
        assertEquals("**/*.csv", locustParser.getDefaultGlobPattern());
    }

    @Test
    public void reportShouldContainSummarizedValues() throws Exception {
        assertEquals(4, report.getSummarizerSize());
        assertEquals(1993, report.getSummarizerMax());
        assertEquals(196, report.getSummarizerMin());
        assertEquals(223, report.getSummarizerAvg());
    }

    @Test
    public void reportShouldContainAllReports() throws Exception {
        String[] reportUris = new String[]{"big", "huge", "medium", "small"};
        assertArrayEquals(reportUris, report.getUriReportMap().keySet().toArray(new String[0]));

        for (String uri : reportUris) {
            assertEquals(true, report.getUriReportMap().get(uri).hasSamples());
        }
    }

    @Test
    public void reportHasValuesInUriReport() {
        assertEquals(false, report.getUriReportMap().get("big").isExcludeResponseTime());
        assertEquals(370, report.getUriReportMap().get("big").getAverage());
        assertEquals(1, report.getUriReportMap().get("big").samplesCount());
        assertEquals(638.0, report.getUriReportMap().get("big").getAverageSizeInKb(), 0.001);
    }

    @Test
    public void reportShouldContainTrafficSize() {
        assertEquals(71464.0, report.getTotalTrafficInKb(), 0.001);
    }

    @Test
    public void reportShouldReturnProperFileName() throws Exception {
        assertEquals(FILE_NAME, report.getReportFileName());
    }
}
