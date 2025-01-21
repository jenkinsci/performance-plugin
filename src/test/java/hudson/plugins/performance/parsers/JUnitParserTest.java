package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JUnitParserTest {

    /**
     * Test parsing a test with a long duration (ie. over 999 seconds) will work.
     * Previously this was causing a NumberFormatException since it puts commas
     * in for over 999 seconds, causing system to complain.
     */
    @Test
    void testParseDurationLongRunningTest() {
        assertEquals((long) 34953254, JUnitParser.parseDuration("34,953.254"), "Test having a time with commas will work.");
        assertEquals((long) 1134953254, JUnitParser.parseDuration("1,134,953.254"), "Test having a time with multiple commas will work.");
        assertEquals((long) 999999, JUnitParser.parseDuration("999.999"), "Test that time with no commas still work.");
    }

    @Test
    void testCanParseFileWithoutTimeAtrribute() throws Exception {
        final JUnitParser parser = new JUnitParser(null, PerformanceReportTest.DEFAULT_PERCENTILES);
        final File reportFile = new File(getClass().getResource("/TEST-JUnitResults-noTimeAttribute.xml").toURI());

        // Execute system under test.
        final PerformanceReport result = parser.parse(reportFile);

        // Verify results.
        assertNotNull(result);
        assertEquals(4, result.samplesCount(), "The source file contains four samples. These should all have been added to the performance report.");
    }

    @Test
    void testCanParseJunitResultFileWithSuccessErrorAndFailure() throws Exception {
        final JUnitParser parser = new JUnitParser(null, PerformanceReportTest.DEFAULT_PERCENTILES);
        final File reportFile = new File(getClass().getResource("/TEST-JUnitResults-success-failure-error.xml").toURI());

        // Execute system under test.
        final PerformanceReport result = parser.parse(reportFile);

        // Verify results.
        assertNotNull(result);
        assertEquals(3, result.samplesCount(), "The source file contains 3 samples. These should all have been added to the performance report.");
        assertEquals(2, result.countErrors(), "The source file contains 2 failed samples. 1 test failure and 1 runtime error sample.");
    }
}
