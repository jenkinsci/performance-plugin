package hudson.plugins.performance.actions;

import hudson.model.FreeStyleProject;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.details.GraphConfigurationDetail;
import hudson.plugins.performance.details.TestSuiteReportDetail;
import hudson.plugins.performance.details.TrendReportDetail;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
@ExtendWith(MockitoExtension.class)
class PerformanceProjectActionTest {

    @Mock
    private StaplerRequest staplerRequest;

    @Test
    void testDynamic(JenkinsRule j) throws Exception {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject("testProject");
        PerformanceProjectAction performanceProjectAction = new PerformanceProjectAction(freeStyleProject);

        Object nullObj = performanceProjectAction.getDynamic("testNull", null, null);
        assertNull(nullObj);

        Object graphConfigurationDetail = performanceProjectAction.getDynamic("configure", staplerRequest, null);
        assertInstanceOf(GraphConfigurationDetail.class, graphConfigurationDetail);

        Object trendReportDetail = performanceProjectAction.getDynamic("trendReport", staplerRequest, null);
        assertInstanceOf(TrendReportDetail.class, trendReportDetail);

        Object testSuiteReportDetail = performanceProjectAction.getDynamic("testsuiteReport", staplerRequest, null);
        assertInstanceOf(TestSuiteReportDetail.class, testSuiteReportDetail);

        assertFalse(performanceProjectAction.ifModePerformancePerTestCaseUsed());
        assertTrue(performanceProjectAction.ifModeThroughputUsed());

        freeStyleProject.getPublishersList().add(new PerformancePublisher("", 0, 0, "", 0.0, 0.0, 0.0, 0.0, 0, false, "", true, false, false, true,false, null));
        assertFalse(performanceProjectAction.ifModePerformancePerTestCaseUsed());
        assertTrue(performanceProjectAction.ifModeThroughputUsed());
    }
}