package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class JmeterSummarizerParserTest {

    @Test
    public void testParse() throws Exception {
        JmeterSummarizerParser jmeterSummarizerParser = new JmeterSummarizerParser(null, PerformanceReportTest.DEFAULT_PERCENTILES);
        File summaryLogFile = new File(getClass().getResource("/summary.log").toURI());
        PerformanceReport performanceReport = jmeterSummarizerParser.parse(summaryLogFile);

        assertEquals(1257, performanceReport.getSummarizerSize());
        assertEquals(333, performanceReport.getSummarizerAvg());
        assertEquals(3, performanceReport.getSummarizerMin());
        assertEquals(5630, performanceReport.getSummarizerMax());
        assertEquals("4.56", performanceReport.getSummarizerErrors());
    }


    @Test
    public void testParseNewLog() throws Exception {

        String path = getClass().getResource("/jmeter.log").getPath();
        String parser = ParserDetector.detect(path);
        assertEquals(JmeterSummarizerParser.class.getSimpleName(), parser);

        JmeterSummarizerParser jmeterSummarizerParser = new JmeterSummarizerParser(path, PerformanceReportTest.DEFAULT_PERCENTILES);
        PerformanceReport performanceReport = jmeterSummarizerParser.parse(new File(path));

        assertEquals(1000, performanceReport.getSummarizerSize());
        assertEquals(276, performanceReport.getSummarizerAvg());
        assertEquals(50, performanceReport.getSummarizerMin());
        assertEquals(500, performanceReport.getSummarizerMax());
        assertEquals("0.00", performanceReport.getSummarizerErrors());
    }
}
