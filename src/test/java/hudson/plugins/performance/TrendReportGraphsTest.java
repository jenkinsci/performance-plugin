package hudson.plugins.performance;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.performance.reports.PerformanceReport;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class TrendReportGraphsTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @After
    public void shutdown() throws Exception {
        j.after();
    }

    @Test
    public void test() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        FreeStyleBuild build = project.createExecutable();
        PerformanceReport report = new PerformanceReport();

        TrendReportGraphs graphs = new TrendReportGraphs(project, build, null, "simpleFilename", report);

        assertEquals(0, graphs.getUris().size());
        assertEquals("simpleFilename", graphs.getFilename());
        assertEquals("Trend report", graphs.getDisplayName());
        assertEquals(project, graphs.getProject());
        assertEquals(build, graphs.getBuild());
        assertEquals(report, graphs.getPerformanceReport());
        assertEquals(null, graphs.getUriReport("null"));
    }
}