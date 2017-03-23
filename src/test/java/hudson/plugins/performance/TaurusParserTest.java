package hudson.plugins.performance;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 *
 */
public class TaurusParserTest {

    public static final double DELTA = 0.000001;

    @Test
    public void testReadCSV() throws Exception {
        TaurusParser parser = new TaurusParser("csv report");

        PerformanceReport report = parser.parse(new File(getClass().getResource("/TaurusCSVReport.csv").toURI()));
        checkPerformanceReport(report);

    }

    @Test
    public void testReadXML() throws Exception {
        TaurusParser parser = new TaurusParser("xml report");

        PerformanceReport report = parser.parse(new File(getClass().getResource("/TaurusXMLReport.xml").toURI()));
        checkPerformanceReport(report);
    }

    private void checkPerformanceReport(PerformanceReport report) {
        assertEquals("Check min", 0.06400 * 1000, report.getMin(), DELTA);
        assertEquals("Check median", 0.18700 * 1000, report.getMedian(), DELTA);
        assertEquals("Check line 90", 1.15800 * 1000, report.get90Line(), DELTA);
        assertEquals("Check max", 1.71800 * 1000, report.getMax(), DELTA);
        assertEquals("Check average", (long) (0.45950 * 1000), report.getAverage());
        assertEquals("Check samples count", 666 + 11, report.samplesCount(), DELTA);

    }
}