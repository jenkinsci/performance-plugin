package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocustParserTest {
    static final String FILE_NAME = "test_results_stats.csv";
    File requestReportFile;
    LocustParser locustParser;
    PerformanceReport report;

    @BeforeEach
    void setUp() throws Exception {
        requestReportFile = new File(getClass().getResource(String.format("/%s", FILE_NAME)).toURI());
        locustParser = new LocustParser(null, PerformanceReportTest.DEFAULT_PERCENTILES);
        report = locustParser.parse(requestReportFile);
    }

    @Test
    void shouldCreateParser() throws Exception {
        assertNotNull(locustParser);
    }

    @Test
    void parserShouldReturnGlobPattern() throws Exception {
        assertEquals("**/*_stats.csv", locustParser.getDefaultGlobPattern());
    }

    @Test
    void reportShouldContainSummarizedValues() throws Exception {
        assertEquals(4, report.getSummarizerSize());
        assertEquals(1993, report.getSummarizerMax());
        assertEquals(196, report.getSummarizerMin());
        assertEquals(223, report.getSummarizerAvg());
    }

    @Test
    void reportShouldContainAllReports() throws Exception {
        String[] reportUris = new String[]{"big", "huge", "medium", "small"};
        assertArrayEquals(reportUris, report.getUriReportMap().keySet().toArray(new String[0]));

        for (String uri : reportUris) {
            assertTrue(report.getUriReportMap().get(uri).hasSamples());
        }
    }

    @Test
    void reportHasValuesInUriReport() {
        assertFalse(report.getUriReportMap().get("big").isExcludeResponseTime());
        assertEquals(370, report.getUriReportMap().get("big").getAverage());
        assertEquals(1, report.getUriReportMap().get("big").samplesCount());
        assertEquals(638.0, report.getUriReportMap().get("big").getAverageSizeInKb(), 0.001);
    }

    @Test
    void reportShouldContainTrafficSize() {
        assertEquals(71464.0, report.getTotalTrafficInKb(), 0.001);
    }

    @Test
    void reportShouldReturnProperFileName() throws Exception {
        assertEquals(FILE_NAME, report.getReportFileName());
    }
}
