package hudson.plugins.performance.parsers;

import hudson.plugins.performance.Messages;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.util.FormValidation;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class JMeterCsvParserTest {
    private static final String NO_GLOB = null;
    private File reportFile;
    private File reportFile2;
    private File reportFile3;

    @Before
    public void beforeMethod() throws Exception {
        reportFile = new File(getClass().getResource("/JMeterCsvResults.csv").toURI());
        reportFile2 = new File(getClass().getResource("/JMeterCsvResults2.csv").toURI());
        reportFile3 = new File(getClass().getResource("/JMeterCsvResults3.csv").toURI());
    }


    @Test
    public void canParseCsvFile() throws Exception {
        final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB);
        parseAndVerifyResult(parser, reportFile);
    }

    @Test
    public void canParseCsvFileWhenSkipFirstLineIsNotSpecifiedAndFirstLineHasHeader() throws Exception {
        final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB);
        parseAndVerifyResult(parser, reportFile);
    }

    @Test
    public void testDateDateFormats() throws Exception {
        final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB);
        parseAndVerifyResult(parser, reportFile);
        parseAndVerifyResult(parser, reportFile2);
        parseAndVerifyResult(parser, reportFile3);
    }

    private void parseAndVerifyResult(JMeterCsvParser parser, File file) throws Exception {
        final PerformanceReport result = parser.parse(file);
        // Verify results.
        assertNotNull(result);
        assertEquals("The source file contains three samples. These should all have been added to the performance report.", 3, result.samplesCount());
    }

    @Test
    public void testLookingForDelimeter() throws Exception {
        assertEquals(",", JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc,adzxcAZZAAZ"));
        assertEquals("\t", JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc\tadzxcAZZAAZ"));
        assertEquals(";", JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc;adzxcAZZAAZ"));
        assertEquals("^", JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc^adzxcAZZAAZ"));
        assertEquals(":", JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc:tadzxcAZZAAZ"));

        try {
            JMeterCsvParser.lookingForDelimiter("asdadadadasd");
            fail("Can not find delimiter in this string");
        } catch (Exception ex) {
            assertEquals("Cannot find delimiter in header asdadadadasd", ex.getMessage());
        }
    }
}