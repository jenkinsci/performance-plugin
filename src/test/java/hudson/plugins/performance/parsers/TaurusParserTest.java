package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TaurusParserTest {

    public static final double DELTA = 0.000001;

    @Test
    public void testReadXML() throws Exception {
        TaurusParser parser = new TaurusParser("xml report");

        PerformanceReport report = parser.parse(new File(getClass().getResource("/TaurusXMLReport.xml").toURI()));
        PerformanceReport prevReport = parser.parse(new File(getClass().getResource("/TaurusPreviousBuildReport.xml").toURI()));
        assertNotNull(report);
        checkPerformanceReport(report);

        assertNotNull(prevReport);
        report.setLastBuildReport(prevReport);
        checkReportDiff(report);

        Map<String, UriReport> uriReportMap = report.getUriReportMap();
        assertEquals(2, uriReportMap.size());

        for (String key : uriReportMap.keySet()) {
            if ("__test_url1.org_".equals(key)) {
                checkUriReport1(uriReportMap.get(key));
            } else if ("__test_url2.com_".equals(key)) {
                checkUriReport2(uriReportMap.get(key));
            } else {
                fail("There is not such key in test file: TaurusXMLReport.xml");
            }
        }

    }

    private void checkUriReport1(UriReport report) {
        // Check values for __test_url1.org_
        assertEquals("Check min", 0.15600 * 1000, report.getMin(), DELTA);
        assertEquals("Check median", 0.84300 * 1000, report.getMedian(), DELTA);
        assertEquals("Check line 90", 1.29300 * 1000, report.get90Line(), DELTA);
        assertEquals("Check max", 1.71800 * 1000, report.getMax(), DELTA);
        assertEquals("Check average", (long) (0.80638 * 1000), report.getAverage());
        assertEquals("Check samples count", 326 + 11, report.samplesCount(), DELTA);
        assertEquals("Check throughput", new Long(337), report.getThroughput());
        assertEquals("Check errors", 3.264, report.errorPercent(), DELTA);
    }

    private void checkUriReport2(UriReport report) {
        // Check values for __test_url2.com_
        assertEquals("Check min", 0.06400 * 1000, report.getMin(), DELTA);
        assertEquals("Check median", 0.07200 * 1000, report.getMedian(), DELTA);
        assertEquals("Check line 90", 0.18900 * 1000, report.get90Line(), DELTA);
        assertEquals("Check max", 0.52200 * 1000, report.getMax(), DELTA);
        assertEquals("Check average", (long) (0.11568 * 1000), report.getAverage());
        assertEquals("Check samples count", 340, report.samplesCount(), DELTA);
        assertEquals("Check throughput", new Long(340), report.getThroughput());
        assertEquals("Check errors", 0.0, report.errorPercent(), DELTA);
    }

    private void checkPerformanceReport(PerformanceReport report) {
        // Check summary values
        assertEquals("Check min", 0.06400 * 1000, report.getMin(), DELTA);
        assertEquals("Check median", 0.18700 * 1000, report.getMedian(), DELTA);
        assertEquals("Check line 90", 1.15800 * 1000, report.get90Line(), DELTA);
        assertEquals("Check max", 1.71800 * 1000, report.getMax(), DELTA);
        assertEquals("Check average", (long) (0.45950 * 1000), report.getAverage());
        assertEquals("Check samples count", 666 + 11, report.samplesCount(), DELTA);
        assertEquals("Check total KB", 6946463, report.getTotalTrafficInKb(), DELTA);
    }

    private void checkReportDiff(PerformanceReport report) {
        // Check summary values
        assertEquals("Check diff median", -14.0, report.getMedianDiff(), DELTA);
        assertEquals("Check diff average", -400, report.getAverageDiff());
        assertEquals("Check diff samples count", 232, report.getSamplesCountDiff());
    }

    @Test
    public void testGlobPattern() throws Exception {
        assertEquals("**/*.xml", new TaurusParser("").getDefaultGlobPattern());
    }
}