package hudson.plugins.performance;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@WithJenkins
class TrendReportGraphsTest {

    @Test
    void test(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        FreeStyleBuild build = project.createExecutable();
        PerformanceReport report = new PerformanceReport(PerformanceReportTest.DEFAULT_PERCENTILES);

        TrendReportGraphs graphs = new TrendReportGraphs(project, build, null, "simpleFilename", report);

        assertEquals(0, graphs.getUris().size());
        assertFalse(graphs.hasSamples(""));
        assertEquals("simpleFilename", graphs.getFilename());
        assertEquals("Trend report", graphs.getDisplayName());
        assertEquals(project, graphs.getProject());
        assertEquals(build, graphs.getBuild());
        assertEquals(report, graphs.getPerformanceReport());
        assertNull(graphs.getUriReport("null"));
    }
}