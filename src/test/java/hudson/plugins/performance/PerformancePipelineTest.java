package hudson.plugins.performance;

import hudson.model.Result;
import hudson.slaves.DumbSlave;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.net.URL;

public class PerformancePipelineTest  {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();

    @Test
    public void bztSmokeTests() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();
        final String bztParams =  path + ' ' + "-o modules.jmeter.plugins=[] -o services=[]";

        story.addStep(new Statement() {
            public void evaluate() throws Throwable {
                DumbSlave s = story.j.createOnlineSlave();
                s.setLabelString("test performance test ");
                WorkflowJob p = story.j.createProject(WorkflowJob.class, "demo");
                p.setDefinition(new CpsFlowDefinition(
                        "node{ bzt '" + bztParams + "' }", true));
                WorkflowRun r = p.scheduleBuild2(0).waitForStart();
                story.j.assertBuildStatusSuccess(story.j.waitForCompletion(r));
                story.j.assertLogContains("File aggregate-results.xml reported 0.0% of errors [SUCCESS].", r);
            }
        });
    }

    @Test
    public void perfReportSmokeTests() throws Exception {
        String fileContents = null;

        URL url = getClass().getResource("/TaurusXMLReport.xml");
        if (url != null) {
            fileContents = IOUtils.toString(url);
        }

        final String report = fileContents;

        story.addStep(new Statement() {
            public void evaluate() throws Throwable {
                DumbSlave s = story.j.createOnlineSlave();
                s.setLabelString("test performance report DSL function");
                WorkflowJob p = story.j.createProject(WorkflowJob.class, "demo");
                p.setDefinition(new CpsFlowDefinition(
                        "node{ writeFile file: 'test.xml', text: '''" + report + "'''; perfReport 'test.xml' }", true));
                WorkflowRun r = p.scheduleBuild2(0).waitForStart();
                story.j.assertBuildStatus(Result.FAILURE, story.j.waitForCompletion(r));
                story.j.assertLogContains("File test.xml reported 1.625% of errors [FAILURE]. Build status is: FAILURE", r);
            }
        });
    }

}
