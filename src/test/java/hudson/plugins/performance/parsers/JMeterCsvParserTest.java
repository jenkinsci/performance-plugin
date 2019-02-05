package hudson.plugins.performance.parsers;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import hudson.plugins.performance.reports.UriReport;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
        final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB, PerformanceReportTest.DEFAULT_PERCENTILES);
        parseAndVerifyResult(parser, reportFile);
    }

    @Test
    public void canParseCsvFileWhenSkipFirstLineIsNotSpecifiedAndFirstLineHasHeader() throws Exception {
        final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB, PerformanceReportTest.DEFAULT_PERCENTILES);
        parseAndVerifyResult(parser, reportFile);
    }

    @Test
    public void testDateDateFormats() throws Exception {
        final JMeterCsvParser parser = new JMeterCsvParser(NO_GLOB, PerformanceReportTest.DEFAULT_PERCENTILES);
        PerformanceReport performanceReport = parseAndVerifyResult(parser, reportFile);
        assertEquals(41.9, performanceReport.getTotalTrafficInKb(), 0.01);
        performanceReport = parseAndVerifyResult(parser, reportFile2);
        assertEquals(41.9, performanceReport.getTotalTrafficInKb(), 0.01);
        performanceReport = parseAndVerifyResult(parser, reportFile3);
        assertEquals(41.9, performanceReport.getTotalTrafficInKb(), 0.01);
    }

    private PerformanceReport parseAndVerifyResult(JMeterCsvParser parser, File file) throws Exception {
        final PerformanceReport result = parser.parse(file);
        // Verify results.
        assertNotNull(result);
        assertEquals("The source file contains three samples. These should all have been added to the performance report.", 3, result.samplesCount());
        return result;
    }

    @Test
    public void testLookingForDelimeter() throws Exception {
        assertEquals(',', JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc,adzxcAZZAAZ"));
        assertEquals('\t', JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc\tadzxcAZZAAZ"));
        assertEquals(';', JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc;adzxcAZZAAZ"));
        assertEquals('^', JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc^adzxcAZZAAZ"));
        assertEquals(':', JMeterCsvParser.lookingForDelimiter("acaaAZSZAzzafergc:tadzxcAZZAAZ"));

        try {
            JMeterCsvParser.lookingForDelimiter("asdadadadasd");
            fail("Can not find delimiter in this string");
        } catch (Exception ex) {
            assertEquals("Cannot find delimiter in header asdadadadasd", ex.getMessage());
        }
    }
    @Test
    public void testMultiLineCSV() throws Exception {

        // Setup fixture.
        final JMeterCsvParser parser = new JMeterCsvParser(null, PerformanceReportTest.DEFAULT_PERCENTILES);
        final File reportFile = new File(getClass().getResource("/multiLineCSV.jtl").toURI());

        // Execute system under test.
        PerformanceReport performanceReport = parser.parse(reportFile);

        Map<String, UriReport> reportMap = performanceReport.getUriReportMap();

        assertEquals(2, reportMap.size());
        for (UriReport report : reportMap.values()) {
            if (report.getHttpCode().equals("200")) {
                assertEquals("Preparation: Login", report.getUri());
            } else if (report.getHttpCode().equals("500")) {
                assertEquals("no such element: Unable to locate element: {\"method\":\"id\",\"selector\":\"reviewBAD\"}\n" +
                        "                  Session ID: 89a1a36b52e184afb01963257d8739e8\n" +
                        "                  *** Element info: {Using=id, value=reviewBAD}", report.getUri());
            } else {
                fail("Wrong uri sampler");
            }
        }
    }
    
    @Test
    public void testCSVWithRegex() throws Exception {
        final JMeterCsvParser parser = new JMeterCsvParser(
                null, PerformanceReportTest.DEFAULT_PERCENTILES, "^(HP|Scenario|Search)(-success|-failure)?$");
        final File reportFile = new File(getClass().getResource("/filewithtransactions.csv").toURI());

        // Execute system under test.
        PerformanceReport performanceReport = parser.parse(reportFile);

        Map<String, UriReport> reportMap = performanceReport.getUriReportMap();

        assertEquals(3, reportMap.size());
        assertEquals("Search", reportMap.get("Search").getUri());
        assertEquals("Scenario", reportMap.get("Scenario").getUri());
        assertEquals("HP", reportMap.get("HP").getUri());
        
        assertEquals(37, reportMap.get("HP").samplesCount());
        assertEquals(33, reportMap.get("Search").samplesCount());
        assertEquals(33, reportMap.get("Scenario").samplesCount());
    }
}