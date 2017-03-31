package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class JmeterSummarizerParserTest {

    @Test
    public void testParse() throws Exception {
        JmeterSummarizerParser jmeterSummarizerParser = new JmeterSummarizerParser(null);
        File summaryLogFile = new File(getClass().getResource("/summary.log").toURI());
        PerformanceReport performanceReport = jmeterSummarizerParser.parse(summaryLogFile);

        assertEquals(1257, performanceReport.getSummarizerSize());
        assertEquals(333, performanceReport.getSummarizerAvg());
        assertEquals(3, performanceReport.getSummarizerMin());
        assertEquals(5630, performanceReport.getSummarizerMax());
        assertEquals("4.56", performanceReport.getSummarizerErrors());
    }
}
