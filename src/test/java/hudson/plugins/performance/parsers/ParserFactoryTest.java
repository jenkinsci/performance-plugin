package hudson.plugins.performance.parsers;

import hudson.FilePath;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class ParserFactoryTest {

    @Test
    public void testFlow() throws Exception {
        FilePath path = new FilePath(new File("/"));
        String filePath;

        filePath = getClass().getResource("/TaurusXMLReport.xml").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath) instanceof TaurusParser);

        filePath = getClass().getResource("/JMeterResults.jtl").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath) instanceof JMeterParser);

        filePath = getClass().getResource("/TEST-JUnitResults.xml").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath) instanceof JUnitParser);

        filePath = getClass().getResource("/IagoResults.log").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath) instanceof IagoParser);

        filePath = getClass().getResource("/WrkResultsQuick.wrk").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath) instanceof WrkSummarizerParser);

        filePath = getClass().getResource("/JMeterCsvResults.csv").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath) instanceof JMeterCsvParser);

        filePath = getClass().getResource("/summary.log").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath) instanceof JmeterSummarizerParser);
    }

    @Test
    public void testFlowWithGlob() throws Exception {
        assertTrue(ParserFactory.getParser(null, "**/*.xml") instanceof TaurusParser);
        assertTrue(ParserFactory.getParser(null, "**/*.jtl") instanceof JMeterParser);
        assertTrue(ParserFactory.getParser(null, "**/TEST-*.xml") instanceof JUnitParser);
        assertTrue(ParserFactory.getParser(null, "parrot-server-stats.log") instanceof IagoParser);
        assertTrue(ParserFactory.getParser(null, "**/*.wrk") instanceof WrkSummarizerParser);
        assertTrue(ParserFactory.getParser(null, "**/*.csv") instanceof JMeterCsvParser);
        assertTrue(ParserFactory.getParser(null, "**/*.log") instanceof JmeterSummarizerParser);
    }
}