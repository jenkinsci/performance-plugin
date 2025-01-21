package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import hudson.plugins.performance.reports.UriReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class TaurusParserTest {

    public static final double DELTA = 0.000001;

    @Test
    void testReadXML() throws Exception {
        TaurusParser parser = new TaurusParser("xml report", PerformanceReportTest.DEFAULT_PERCENTILES);

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
        assertEquals(0.15600 * 1000, report.getMin(), DELTA, "Check min");
        assertEquals(0.84300 * 1000, report.getMedian(), DELTA, "Check median");
        assertEquals(1.29300 * 1000, report.get90Line(), DELTA, "Check line 90");
        assertEquals(1.42000 * 1000, report.get95Line(), DELTA, "Check line 95");
        assertEquals(1.71800 * 1000, report.getMax(), DELTA, "Check max");
        assertEquals((long) (0.80638 * 1000), report.getAverage(), "Check average");
        assertEquals(326 + 11, report.samplesCount(), DELTA, "Check samples count");
        assertEquals(3.264, report.errorPercent(), DELTA, "Check errors");
    }

    private void checkUriReport2(UriReport report) {
        // Check values for __test_url2.com_
        assertEquals(0.06400 * 1000, report.getMin(), DELTA, "Check min");
        assertEquals(0.07200 * 1000, report.getMedian(), DELTA, "Check median");
        assertEquals(0.18900 * 1000, report.get90Line(), DELTA, "Check line 90");
        assertEquals(0.20400 * 1000, report.get95Line(), DELTA, "Check line 95");
        assertEquals(0.52200 * 1000, report.getMax(), DELTA, "Check max");
        assertEquals((long) (0.11568 * 1000), report.getAverage(), "Check average");
        assertEquals(340, report.samplesCount(), DELTA, "Check samples count");
        assertEquals(0.0, report.errorPercent(), DELTA, "Check errors");
    }

    private void checkPerformanceReport(PerformanceReport report) {
        // Check summary values
        assertEquals(0.06400 * 1000, report.getMin(), DELTA, "Check min");
        assertEquals(0.18700 * 1000, report.getMedian(), DELTA, "Check median");
        assertEquals(1.15800 * 1000, report.get90Line(), DELTA, "Check line 90");
        assertEquals(1.29300 * 1000, report.get95Line(), DELTA, "Check line 95");
        assertEquals(1.71800 * 1000, report.getMax(), DELTA, "Check max");
        assertEquals((long) (0.45950 * 1000), report.getAverage(), "Check average");
        assertEquals(666 + 11, report.samplesCount(), DELTA, "Check samples count");
        assertEquals(6946463, report.getTotalTrafficInKb(), DELTA, "Check total KB");
    }

    private void checkReportDiff(PerformanceReport report) {
        // Check summary values
        assertEquals(-14.0, report.getMedianDiff(), DELTA, "Check diff median");
        assertEquals(-400, report.getAverageDiff(), "Check diff average");
        assertEquals(213, report.get90LineDiff(), "Check diff line 90");
        assertEquals(348, report.get95LineDiff(), "Check diff line 95");
        assertEquals(232, report.getSamplesCountDiff(), "Check diff samples count");
    }

    @Test
    void testGlobPattern() throws Exception {
        assertEquals("**/*.xml", new TaurusParser("", PerformanceReportTest.DEFAULT_PERCENTILES).getDefaultGlobPattern());
    }
}