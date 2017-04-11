package hudson.plugins.performance.parsers;

import hudson.EnvVars;
import hudson.FilePath;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import java.io.File;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ParserFactoryTest {

    @Test
    public void testFlow() throws Exception {
        EnvVars envVars = new EnvVars(new HashMap<String, String>());
        FilePath path = new FilePath(new File("/"));
        String filePath;

        filePath = getClass().getResource("/TaurusXMLReport.xml").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath, envVars) instanceof TaurusParser);

        filePath = getClass().getResource("/JMeterResults.jtl").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath, envVars) instanceof JMeterParser);

        filePath = getClass().getResource("/TEST-JUnitResults.xml").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath, envVars) instanceof JUnitParser);

        filePath = getClass().getResource("/IagoResults.log").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath, envVars) instanceof IagoParser);

        filePath = getClass().getResource("/WrkResultsQuick.wrk").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath, envVars) instanceof WrkSummarizerParser);

        filePath = getClass().getResource("/JMeterCsvResults.csv").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath, envVars) instanceof JMeterCsvParser);

        filePath = getClass().getResource("/summary.log").toURI().getPath();
        assertTrue(ParserFactory.getParser(path, filePath, envVars) instanceof JmeterSummarizerParser);
    }

    @Test
    public void testFlowWithGlob() throws Exception {
        EnvVars envVars = new EnvVars(new HashMap<String, String>());
        assertTrue(ParserFactory.getParser(null, "**/*.xml", envVars) instanceof TaurusParser);
        assertTrue(ParserFactory.getParser(null, "**/*.jtl", envVars) instanceof JMeterParser);
        assertTrue(ParserFactory.getParser(null, "**/TEST-*.xml", envVars) instanceof JUnitParser);
        assertTrue(ParserFactory.getParser(null, "parrot-server-stats.log", envVars) instanceof IagoParser);
        assertTrue(ParserFactory.getParser(null, "**/*.wrk", envVars) instanceof WrkSummarizerParser);
        assertTrue(ParserFactory.getParser(null, "**/*.csv", envVars) instanceof JMeterCsvParser);
        assertTrue(ParserFactory.getParser(null, "**/*.log", envVars) instanceof JmeterSummarizerParser);
    }

    @Test
    @Issue("43503")
    public void test43503() throws Exception {
        EnvVars envVars = new EnvVars(new HashMap<String, String>());
        File report = new File(getClass().getResource("/WrkResultsQuick.wrk").getPath());
        FilePath workspace = new FilePath(new File(report.getParentFile().getParent()));
        String glob = "**/test-classes/*.wrk";
        assertTrue(ParserFactory.getParser(workspace, glob, envVars) instanceof WrkSummarizerParser);
    }
}