package hudson.plugins.performance.parsers;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@WithJenkins
class ParserFactoryTest {

    @Test
    void testFlow(JenkinsRule j) throws Exception {
        EnvVars envVars = new EnvVars(new HashMap<String, String>());
        FreeStyleProject project = j.createFreeStyleProject();
        FreeStyleBuildExt build = new FreeStyleBuildExt(project);

        FilePath workspace = new FilePath(Files.createTempDirectory(null).toFile());
        build.setWorkspace(workspace);
        String filePath;

        filePath = getClass().getResource("/TaurusXMLReport.xml").toURI().getPath();
        assertInstanceOf(TaurusParser.class, ParserFactory.getParser(build, workspace, null, filePath, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        filePath = getClass().getResource("/JMeterResults.jtl").toURI().getPath();
        assertInstanceOf(JMeterParser.class, ParserFactory.getParser(build, workspace, null, filePath, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        filePath = getClass().getResource("/TEST-JUnitResults.xml").toURI().getPath();
        assertInstanceOf(JUnitParser.class, ParserFactory.getParser(build, workspace, null, filePath, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        filePath = getClass().getResource("/IagoResults.log").toURI().getPath();
        assertInstanceOf(IagoParser.class, ParserFactory.getParser(build, workspace, null, filePath, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        filePath = getClass().getResource("/WrkResultsQuick.wrk").toURI().getPath();
        assertInstanceOf(WrkSummarizerParser.class, ParserFactory.getParser(build, workspace, null, filePath, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        filePath = getClass().getResource("/JMeterCsvResults.csv").toURI().getPath();
        assertInstanceOf(JMeterCsvParser.class, ParserFactory.getParser(build, workspace, null, filePath, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        filePath = getClass().getResource("/summary.log").toURI().getPath();
        assertInstanceOf(JmeterSummarizerParser.class, ParserFactory.getParser(build, workspace, null, filePath, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        filePath = getClass().getResource("/test_results_stats.csv").toURI().getPath();
        assertInstanceOf(LocustParser.class, ParserFactory.getParser(build, workspace, null, filePath, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
    }

    @Test
    void testFlowWithGlob(JenkinsRule j) throws Exception {
        EnvVars envVars = new EnvVars(new HashMap<String, String>());
        assertInstanceOf(TaurusParser.class, ParserFactory.getParser(null, null, null, "**/*.xml", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
        assertInstanceOf(JMeterParser.class, ParserFactory.getParser(null, null, null, "**/*.jtl", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
        assertInstanceOf(JUnitParser.class, ParserFactory.getParser(null, null, null, "**/TEST-*.xml", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
        assertInstanceOf(IagoParser.class, ParserFactory.getParser(null, null, null, "parrot-server-stats.log", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
        assertInstanceOf(WrkSummarizerParser.class, ParserFactory.getParser(null, null, null, "**/*.wrk", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
        assertInstanceOf(JMeterCsvParser.class, ParserFactory.getParser(null, null, null, "**/*.csv", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
        assertInstanceOf(JmeterSummarizerParser.class, ParserFactory.getParser(null, null, null, "**/*.log", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
        assertInstanceOf(LocustParser.class, ParserFactory.getParser(null, null, null, "**/*_stats.csv", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
    }

    @Test
    @Issue("JENKINS-43503")
    void test43503(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        FreeStyleBuildExt build = new FreeStyleBuildExt(project);
        EnvVars envVars = new EnvVars(new HashMap<String, String>());

        FilePath workspace = new FilePath(Files.createTempDirectory(null).toFile());
        build.setWorkspace(workspace);

        FilePath results = workspace.child("results");
        results.mkdirs();

        FilePath child = results.child("result.wrk");
        child.copyFrom(getClass().getResourceAsStream("/WrkResultsQuick.wrk"));
        String glob = "**/results/*.wrk";
        assertInstanceOf(WrkSummarizerParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, glob, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        FilePath child2 = results.child("result.jtl");
        child2.copyFrom(getClass().getResourceAsStream("/JMeterResults.jtl"));
        String glob2 = "**/results/*.jtl";
        assertInstanceOf(JMeterParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, glob2, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
    }


    @Test
    @Issue("JENKINS-45119")
    void test45119(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        FreeStyleBuildExt build = new FreeStyleBuildExt(project);
        EnvVars envVars = new EnvVars(new HashMap<String, String>());

        FilePath workspace = new FilePath(Files.createTempDirectory(null).toFile());
        build.setWorkspace(workspace);

        String absPath1 = getClass().getResource("/WrkResultsQuick.wrk").getPath();

        assertInstanceOf(WrkSummarizerParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, absPath1, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        String absPath2 = getClass().getResource("/JMeterResults.jtl").getPath();
        assertInstanceOf(JMeterParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, absPath2, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        FilePath results = workspace.child("results");
        results.mkdirs();

        FilePath child = results.child("result.wrk");
        child.copyFrom(getClass().getResourceAsStream("/WrkResultsQuick.wrk"));
        assertInstanceOf(WrkSummarizerParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, "results/result.wrk", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        FilePath child2 = results.child("result.jtl");
        child2.copyFrom(getClass().getResourceAsStream("/JMeterResults.jtl"));
        assertInstanceOf(JMeterParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, "results/result.jtl", envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
    }

    @Test
    @Issue("JENKINS-45119")
    void testAntAbsolutePath(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        FreeStyleBuildExt build = new FreeStyleBuildExt(project);
        EnvVars envVars = new EnvVars(new HashMap<String, String>());

        FilePath workspace = new FilePath(Files.createTempDirectory(null).toFile());
        build.setWorkspace(workspace);

        String path = getClass().getResource("/single_result/res.csv").getPath();
        path = path.replace("res.", "*.");
        assertInstanceOf(JMeterCsvParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, path, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        String path2 = getClass().getResource("/single_result/nested/res.jtl").getPath();
        path2 = path2.replace("nested/res", "**/*");
        assertInstanceOf(JMeterParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, path2, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));

        String path3 = getClass().getResource("/single_result/nested/res.jtl").getPath();
        path3 = path3.replace("single_result", "**");
        path3 = path3.replace("res.", "*.");
        assertInstanceOf(JMeterParser.class, ParserFactory.getParser(build, build.getWorkspace(), null, path3, envVars, PerformanceReportTest.DEFAULT_PERCENTILES, PerformanceReport.INCLUDE_ALL).get(0));
    }


    public static class FreeStyleBuildExt extends FreeStyleBuild {

        public FreeStyleBuildExt(FreeStyleProject project) throws IOException {
            super(project);
        }

        @Override
        protected void setWorkspace(@NonNull FilePath ws) {
            super.setWorkspace(ws);
        }
    }
}