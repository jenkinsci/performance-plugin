package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AbstractParserTest {

    @Test
    public void testDeserialized() throws Exception {
        File serializedFile = new File(getClass().getResource("/results.v.2.0.jtl.serialized").toURI());
        String reportFilePath = serializedFile.getAbsolutePath().replace(".serialized", "");
        File reportFile = new File(reportFilePath);
        PerformanceReport report = AbstractParser.loadSerializedReport(reportFile);
        assertNotNull(report);
        report = AbstractParser.loadSerializedReport(reportFile); // subsequent calls should be cached
        assertNotNull(report);
        assertEquals("totalDuration", 2030.47, report.getTotalTrafficInKb(), 0.001);
        assertEquals("samples count", 200, report.samplesCount());
    }

    @Test
    public void testUploadOldReport() throws Exception {
        File serializedFile = new File(getClass().getResource("/result.csv.serialized").toURI());
        String reportFilePath = serializedFile.getAbsolutePath().replace(".serialized", "");

        PerformanceReport report = AbstractParser.loadSerializedReport(new File(reportFilePath));
        assertNotNull(report);
        Map<Double, Long> percentilesValues = report.getPercentilesValues();
        assertEquals(5, percentilesValues.size());
        assertEquals(Long.valueOf(320), percentilesValues.get(50d));
        assertEquals(Long.valueOf(449), percentilesValues.get(90d));
        assertEquals(Long.valueOf(455), percentilesValues.get(95d));
        assertEquals(Long.valueOf(100), percentilesValues.get(0d));
        assertEquals(Long.valueOf(468), percentilesValues.get(100d));

        assertEquals(320, report.getMedian());
        assertEquals(449, report.get90Line());
        assertEquals(455, report.get95Line());
        assertEquals(100, report.getMin());
        assertEquals(468, report.getMax());

        System.out.println();
    }
}