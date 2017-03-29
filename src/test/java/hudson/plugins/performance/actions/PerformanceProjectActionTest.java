package hudson.plugins.performance.actions;

import hudson.model.FreeStyleProject;
import hudson.plugins.performance.details.GraphConfigurationDetail;
import hudson.plugins.performance.details.TestSuiteReportDetail;
import hudson.plugins.performance.details.TrendReportDetail;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class PerformanceProjectActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Mock
    private StaplerRequest staplerRequest;

    @After
    public void shutdown() throws Exception {
        j.after();
    }


    @Test
    public void testDynamic() throws Exception {
        FreeStyleProject freeStyleProject = j.createFreeStyleProject("testProject");
        PerformanceProjectAction performanceProjectAction = new PerformanceProjectAction(freeStyleProject);

        Object nullObj = performanceProjectAction.getDynamic("testNull", null, null);
        assertEquals(null, nullObj);

        Object graphConfigurationDetail = performanceProjectAction.getDynamic("configure", staplerRequest, null);
        assertTrue(graphConfigurationDetail instanceof GraphConfigurationDetail);

        Object trendReportDetail = performanceProjectAction.getDynamic("trendReport", staplerRequest, null);
        assertTrue(trendReportDetail instanceof TrendReportDetail);

        Object testSuiteReportDetail = performanceProjectAction.getDynamic("testsuiteReport", staplerRequest, null);
        assertTrue(testSuiteReportDetail instanceof TestSuiteReportDetail);
    }

}