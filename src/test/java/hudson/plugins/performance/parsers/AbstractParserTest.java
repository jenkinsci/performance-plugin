package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class AbstractParserTest {

    @Test
    public void testDeserialized() throws Exception {
        File serializedFile = new File(getClass().getResource("/results.v.2.0.jtl.serialized").toURI());
        String reportFilePath = serializedFile.getAbsolutePath().replace(".serialized", "");
        PerformanceReport report = AbstractParser.loadSerializedReport(new File(reportFilePath));
        assertNotNull(report);
        assertEquals("totalDuration", 2030.47, report.getTotalTrafficInKb(), 0.001);
        assertEquals("samples count", 200, report.samplesCount());
    }
}