package hudson.plugins.performance.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import hudson.plugins.performance.reports.UriReport;

public class LoadRunnerParserTest {
    private static final String NO_GLOB = null;

    /* Simple test scenario  with 2 VUsers x 1 iteration (until completion), 
     * no rampup, 2 transactions as below:
        Action()
        {	int i;
            for (i = 0; i < 10; i++) {
                lr_set_transaction("transaction1", 1, LR_PASS); // 1 second
                lr_think_time(1); // 1 second
                lr_set_transaction("transaction2", i%2+1, i%5? LR_PASS: LR_FAIL); 1 or 2 seconds, fail every 5th
            }
            return 0;
        }
    */
    @Test
    public void test() throws Exception {
        final LoadRunnerParser parser = new LoadRunnerParser(NO_GLOB, PerformanceReportTest.DEFAULT_PERCENTILES);
        final PerformanceReport result = parser.parse(new File(getClass().getResource("/lr-session.mdb").toURI()));
        assertNotNull(result);

        Map<String, UriReport> uriReportMap = result.getUriReportMap();
        UriReport report = uriReportMap.get("transaction1");
        assertNotNull(report);
        assertEquals("transaction1", report.getDisplayName());
        assertEquals(20, report.samplesCount());
        assertEquals(1000, report.getAverage());
        assertEquals(0, report.countErrors());
        assertEquals(10043, report.getEnd().getTime()-report.getStart().getTime());

        report = uriReportMap.get("transaction2");
        assertNotNull(report);
        assertEquals("transaction2", report.getDisplayName());
        assertEquals(20, report.samplesCount());
        assertEquals(1500, report.getAverage());
        assertEquals(4, report.countErrors());
        assertEquals(10043, report.getEnd().getTime()-report.getStart().getTime());
    }
}