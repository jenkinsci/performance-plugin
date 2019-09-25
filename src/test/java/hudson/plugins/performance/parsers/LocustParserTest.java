package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class LocustParserTest
{
    final static String FILE_NAME = "test_results_requests.csv";
    File requestReportFile;
    LocustParser locustParser;
    PerformanceReport report;

    @Before
    public void setUp() throws Exception
    {
        requestReportFile = new File(getClass().getResource(String.format("/%s", FILE_NAME)).toURI());
        locustParser = new LocustParser(null, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL);
        report = locustParser.parse(requestReportFile);
    }

    @Test
    public void shouldCreateParser() throws Exception
    {
        Assert.assertNotNull(locustParser);
    }

    @Test
    public void parserShouldReturnGlobPattern() throws Exception
    {
        Assert.assertEquals("**/*.csv", locustParser.getDefaultGlobPattern());
    }

    @Test
    public void reportShouldContainSummarizedValues() throws Exception
    {
        Assert.assertEquals(4, report.getSummarizerSize());
        Assert.assertEquals(1993, report.getSummarizerMax());
        Assert.assertEquals(196, report.getSummarizerMin());
        Assert.assertEquals(223, report.getSummarizerAvg());
    }

    @Test
    public void reportShouldContainAllReports() throws Exception
    {
        String[] reportUris = new String[]{"big", "huge", "medium", "small"};
        Assert.assertArrayEquals(reportUris, report.getUriReportMap().keySet().toArray(new String[0]));

        for (String uri : reportUris) {
            Assert.assertEquals(true, report.getUriReportMap().get(uri).hasSamples());
        }
    }

    @Test
    public void reportHasValuesInUriReport()
    {
        Assert.assertEquals(false, report.getUriReportMap().get("big").isExcludeResponseTime());
        Assert.assertEquals(370, report.getUriReportMap().get("big").getAverage());
        Assert.assertEquals(1, report.getUriReportMap().get("big").samplesCount());
        Assert.assertEquals(638.0, report.getUriReportMap().get("big").getAverageSizeInKb(), 0.001);
    }

    @Test
    public void reportShouldContainTrafficSize()
    {
        Assert.assertEquals(71464.0, report.getTotalTrafficInKb(), 0.001);
    }

    @Test
    public void reportShouldReturnProperFileName() throws Exception
    {
        Assert.assertEquals(FILE_NAME, report.getReportFileName());
    }
}
