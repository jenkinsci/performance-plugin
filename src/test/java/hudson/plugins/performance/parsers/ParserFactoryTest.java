package hudson.plugins.performance.parsers;

import com.google.common.io.Files;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.*;

public class ParserFactoryTest {


    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testFlow() throws Exception {
        EnvVars envVars = new EnvVars(new HashMap<String, String>());
        FilePath path = new FilePath(new File("/"));
        String filePath;

        filePath = getClass().getResource("/TaurusXMLReport.xml").toURI().getPath();
        assertTrue(ParserFactory.getParser(null, path, null, filePath, envVars) instanceof TaurusParser);

        filePath = getClass().getResource("/JMeterResults.jtl").toURI().getPath();
        assertTrue(ParserFactory.getParser(null, path, null, filePath, envVars) instanceof JMeterParser);

        filePath = getClass().getResource("/TEST-JUnitResults.xml").toURI().getPath();
        assertTrue(ParserFactory.getParser(null, path, null, filePath, envVars) instanceof JUnitParser);

        filePath = getClass().getResource("/IagoResults.log").toURI().getPath();
        assertTrue(ParserFactory.getParser(null, path, null, filePath, envVars) instanceof IagoParser);

        filePath = getClass().getResource("/WrkResultsQuick.wrk").toURI().getPath();
        assertTrue(ParserFactory.getParser(null, path, null, filePath, envVars) instanceof WrkSummarizerParser);

        filePath = getClass().getResource("/JMeterCsvResults.csv").toURI().getPath();
        assertTrue(ParserFactory.getParser(null, path, null, filePath, envVars) instanceof JMeterCsvParser);

        filePath = getClass().getResource("/summary.log").toURI().getPath();
        assertTrue(ParserFactory.getParser(null, path, null, filePath, envVars) instanceof JmeterSummarizerParser);
    }

    @Test
    public void testFlowWithGlob() throws Exception {
        EnvVars envVars = new EnvVars(new HashMap<String, String>());
        assertTrue(ParserFactory.getParser(null, null, null, "**/*.xml", envVars) instanceof TaurusParser);
        assertTrue(ParserFactory.getParser(null, null, null, "**/*.jtl", envVars) instanceof JMeterParser);
        assertTrue(ParserFactory.getParser(null, null, null, "**/TEST-*.xml", envVars) instanceof JUnitParser);
        assertTrue(ParserFactory.getParser(null, null, null, "parrot-server-stats.log", envVars) instanceof IagoParser);
        assertTrue(ParserFactory.getParser(null, null, null, "**/*.wrk", envVars) instanceof WrkSummarizerParser);
        assertTrue(ParserFactory.getParser(null, null, null, "**/*.csv", envVars) instanceof JMeterCsvParser);
        assertTrue(ParserFactory.getParser(null, null, null, "**/*.log", envVars) instanceof JmeterSummarizerParser);
    }

    @Test
    @Issue("43503")
    public void test43503() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        FreeStyleBuildExt build = new FreeStyleBuildExt(project);
        EnvVars envVars = new EnvVars(new HashMap<String, String>());

        FilePath workspace = new FilePath(Files.createTempDir());
        build.setWorkspace(workspace);

        FilePath results = workspace.child("results");
        results.mkdirs();

        FilePath child = results.child("result.wrk");
        child.copyFrom(getClass().getResourceAsStream("/WrkResultsQuick.wrk"));
        String glob = "**/results/*.wrk";
        assertTrue(ParserFactory.getParser(build, build.getWorkspace(), null, glob, envVars) instanceof WrkSummarizerParser);

        FilePath child2 = results.child("result.jtl");
        child2.copyFrom(getClass().getResourceAsStream("/JMeterResults.jtl"));
        String glob2 = "**/results/*.jtl";
        assertTrue(ParserFactory.getParser(build, build.getWorkspace(), null, glob2, envVars) instanceof JMeterParser);
    }

    public static class FreeStyleBuildExt extends FreeStyleBuild {

        public FreeStyleBuildExt(FreeStyleProject project) throws IOException {
            super(project);
        }

        @Override
        protected void setWorkspace(@Nonnull FilePath ws) {
            super.setWorkspace(ws);
        }
    }
}