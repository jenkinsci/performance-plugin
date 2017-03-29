package hudson.plugins.performance.parsers;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;


public class ParserDetectorTest {


    @Test
    public void testFlow() throws Exception {
        File file;

        file = new File(getClass().getResource("/TaurusXMLReport.xml").toURI());
        assertEquals(ParserDetector.ParserType.TAURUS, ParserDetector.detect(file));

        file = new File(getClass().getResource("/JMeterResults.jtl").toURI());
        assertEquals(ParserDetector.ParserType.JMETER, ParserDetector.detect(file));

        file = new File(getClass().getResource("/TEST-JUnitResults.xml").toURI());
        assertEquals(ParserDetector.ParserType.JUNIT, ParserDetector.detect(file));

        file = new File(getClass().getResource("/IagoResults.log").toURI());
        assertEquals(ParserDetector.ParserType.IAGO, ParserDetector.detect(file));

        file = new File(getClass().getResource("/WrkResultsQuick.wrk").toURI());
        assertEquals(ParserDetector.ParserType.WRK, ParserDetector.detect(file));

        file = new File(getClass().getResource("/JMeterCsvResults.csv").toURI());
        assertEquals(ParserDetector.ParserType.JMETER_CSV, ParserDetector.detect(file));

        file = new File(getClass().getResource("/summary.log").toURI());
        assertEquals(ParserDetector.ParserType.JMETER_SUMMARIZER, ParserDetector.detect(file));
    }
}