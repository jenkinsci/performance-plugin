package hudson.plugins.performance;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * This class contains basic tests that verify the parsing behavior of
 * {@link JMeterParser}.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class JMeterParserTest {
    /**
     * Verifies that {@link JMeterParser#isXmlFile(File)} correctly identifies an
     * XML file.
     */
    @Test
    public void testIsXml() throws Exception {
        // Setup fixture.
        final File xmlFile = new File(getClass().getResource("/JMeterResults.jtl").toURI());

        // Execute system under test.
        final boolean result = JMeterParser.isXmlFile(xmlFile);

        // Verify results.
        assertTrue(result);
    }

    /**
     * Verifies that {@link JMeterParser#isXmlFile(File)} returns false when the
     * provided data is CSV.
     */
    @Test
    public void testIsCsv() throws Exception {
        // Setup fixture.
        final File csvFile = new File(getClass().getResource("/JENKINS-16627_CSV_instead_of_XML.jtl").toURI());

        // Execute system under test.
        final boolean result = JMeterParser.isXmlFile(csvFile);

        // Verify results.
        assertFalse(result);
    }

    /**
     * Verifies that {@link JMeterParser#isXmlFile(File)} returns false when the
     * provided data is CSV.
     */
    @Test
    public void testIsEmpty() throws Exception {
        // Setup fixture.
        final File emptyFile = new File(getClass().getResource("/emptyfile.jtl").toURI());

        // Execute system under test.
        final boolean result = JMeterParser.isXmlFile(emptyFile);

        // Verify results.
        assertFalse(result);
    }

    /**
     * Verifies that {@link JMeterParser#isXmlFile(File)} returns true when the
     * XML data is preceded by whitespace.
     */
    @Test
    public void testIsWhitespaceXml() throws Exception {
        // Setup fixture.
        final File xml = new File(getClass().getResource("/whitespace-followed-by-xml.jtl").toURI());

        // Execute system under test.
        final boolean result = JMeterParser.isXmlFile(xml);

        // Verify results.
        assertTrue(result);
    }

    /**
     * JMeter can generate JTL files that contain XML data. This test verifies
     * that such a file can be parsed by {@link JMeterParser#parse(File)} without
     * incident.
     * <p>
     * Note that this tests verifies that the file can be parsed. It does not
     * verify the correctness of the parsed data.
     */
    @Test
    public void testParseXmlJtlFile() throws Exception {
        // Setup fixture.
        final JMeterParser parser = new JMeterParser(null);
        final File reportFile = new File(getClass().getResource("/JMeterResults.jtl").toURI());

        // Execute system under test.
        final PerformanceReport result = parser.parse(reportFile);

        // Verify results.
        assertNotNull(result);
        assertEquals("The source file contains eight samples. These should all have been added to the performance report.",
                8, result.size());
    }

    /**
     * JMeter can generate JTL files that contain XML data. This test verifies
     * that such a file can be parsed by {@link JMeterParser#parse(File)} without
     * incident.
     * <p>
     * Note that this tests verifies that the file can be parsed. It does not
     * verify the correctness of the parsed data.
     */
    @Test
    public void testParseCsvJtlFile() throws Exception {
        // Setup fixture.
        final JMeterParser parser = new JMeterParser(null);
        final File reportFile = new File(getClass().getResource("/JENKINS-16627_CSV_instead_of_XML.jtl").toURI());

        // Execute system under test.
        final PerformanceReport result = parser.parse(reportFile);

        // Verify results.
        assertNotNull(result);
        assertEquals("The source file contains three samples. These should all have been added to the performance report.",
                3, result.size());

        UriReport uriReport = result.getUriReportMap().get("GET _ordermgmt_inventory");
        assertEquals(0, uriReport.countErrors());
        assertEquals("200", uriReport.getHttpCode());
        assertEquals("GET /ordermgmt/inventory", uriReport.getUri());
    }


  /*
  @Test
  public void parseXmlTest() throws Exception 
  {
    // Setup fixture.
    
    // Execute system under test.
    
    // Verify results.
  }
   
   */
}
