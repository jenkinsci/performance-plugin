package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JUnitParserTest {

    /**
     * Test parsing a test with a long duration (ie. over 999 seconds) will work.
     * Previously this was causing a NumberFormatException since it puts commas
     * in for over 999 seconds, causing system to complain.
     */
    @Test
    public void testParseDurationLongRunningTest() {
        assertEquals("Test having a time with commas will work.", JUnitParser.parseDuration("34,953.254"), (long) 34953254);
        assertEquals("Test having a time with multiple commas will work.", JUnitParser.parseDuration("1,134,953.254"), (long) 1134953254);
        assertEquals("Test that time with no commas still work.", JUnitParser.parseDuration("999.999"), (long) 999999);
    }

    @Test
    public void testCanParseFileWithoutTimeAtrribute() throws Exception {
        final JUnitParser parser = new JUnitParser(null);
        final File reportFile = new File(getClass().getResource("/TEST-JUnitResults-noTimeAttribute.xml").toURI());

        // Execute system under test.
        final PerformanceReport result = parser.parse(reportFile);

        // Verify results.
        assertNotNull(result);
        assertEquals("The source file contains four samples. These should all have been added to the performance report.", 4, result.samplesCount());
    }

    @Test
    public void testCanParseJunitResultFileWithSuccessErrorAndFailure() throws Exception {
        final JUnitParser parser = new JUnitParser(null);
        final File reportFile = new File(getClass().getResource("/TEST-JUnitResults-success-failure-error.xml").toURI());

        // Execute system under test.
        final PerformanceReport result = parser.parse(reportFile);

        // Verify results.
        assertNotNull(result);
        assertEquals("The source file contains 3 samples. These should all have been added to the performance report.", 3, result.samplesCount());
        assertEquals("The source file contains 2 failed samples. 1 test failure and 1 runtime error sample.", 2, result.countErrors());
    }
}
