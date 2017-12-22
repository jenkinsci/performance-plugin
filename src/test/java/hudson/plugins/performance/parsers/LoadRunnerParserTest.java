package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import org.junit.Test;
import java.io.File;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        final LoadRunnerParser parser = new LoadRunnerParser(NO_GLOB);
        final PerformanceReport result = parser.parse(new File(getClass().getResource("/lr-session.mdb").toURI()));
        assertNotNull(result);

        Map<String, UriReport> uriReportMap = result.getUriReportMap();
        UriReport report = uriReportMap.get("transaction1");
        assertNotNull(report);
        assertEquals("transaction1", report.getDisplayName());
        assertEquals(20, report.samplesCount());
        assertEquals(1000, report.getAverage());
        assertEquals(0, report.countErrors());
        assertEquals(new Date(1504081650990L), report.getStart());
        assertEquals(new Date(1504081660033L+1000), report.getEnd());

        report = uriReportMap.get("transaction2");
        assertNotNull(report);
        assertEquals("transaction2", report.getDisplayName());
        assertEquals(20, report.samplesCount());
        assertEquals(1500, report.getAverage());
        assertEquals(4, report.countErrors());
        assertEquals(new Date(1504081651990L), report.getStart());
        assertEquals(new Date(1504081660033L+2000), report.getEnd());
    }
}