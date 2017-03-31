package hudson.plugins.performance.parsers;

import org.junit.Test;

import static org.junit.Assert.*;

public class ParserFactoryTest {

    @Test
    public void testFlow() throws Exception {
        String filePath;

        filePath = getClass().getResource("/TaurusXMLReport.xml").toURI().getPath();
        assertTrue(ParserFactory.getParser(filePath) instanceof TaurusParser);

        filePath = getClass().getResource("/JMeterResults.jtl").toURI().getPath();
        assertTrue(ParserFactory.getParser(filePath) instanceof JMeterParser);

        filePath = getClass().getResource("/TEST-JUnitResults.xml").toURI().getPath();
        assertTrue(ParserFactory.getParser(filePath) instanceof JUnitParser);

        filePath = getClass().getResource("/IagoResults.log").toURI().getPath();
        assertTrue(ParserFactory.getParser(filePath) instanceof IagoParser);

        filePath = getClass().getResource("/WrkResultsQuick.wrk").toURI().getPath();
        assertTrue(ParserFactory.getParser(filePath) instanceof WrkSummarizerParser);

        filePath = getClass().getResource("/JMeterCsvResults.csv").toURI().getPath();
        assertTrue(ParserFactory.getParser(filePath) instanceof JMeterCsvParser);

        filePath = getClass().getResource("/summary.log").toURI().getPath();
        assertTrue(ParserFactory.getParser(filePath) instanceof JmeterSummarizerParser);
    }
}