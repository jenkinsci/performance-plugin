package hudson.plugins.performance.parsers;

import org.junit.Test;

import static org.junit.Assert.*;


public class ParserDetectorTest {


    @Test
    public void testFlow() throws Exception {
        String filePath;

        filePath = getClass().getResource("/TaurusXMLReport.xml").toURI().getPath();
        assertEquals(TaurusParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/JMeterResults.jtl").toURI().getPath();
        assertEquals(JMeterParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/TEST-JUnitResults.xml").toURI().getPath();
        assertEquals(JUnitParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/IagoResults.log").toURI().getPath();
        assertEquals(IagoParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/WrkResultsQuick.wrk").toURI().getPath();
        assertEquals(WrkSummarizerParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/JMeterCsvResults.csv").toURI().getPath();
        assertEquals(JMeterCsvParser.class.getSimpleName(), ParserDetector.detect(filePath));

        filePath = getClass().getResource("/summary.log").toURI().getPath();
        assertEquals(JmeterSummarizerParser.class.getSimpleName(), ParserDetector.detect(filePath));
    }
}