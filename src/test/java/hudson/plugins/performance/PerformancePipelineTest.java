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
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.net.URL;

public class PerformancePipelineTest  {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();

    @Test
    public void bztSmokeTests() throws Exception {
        final String path = getClass().getResource("/performanceTest.yml").getPath();

        story.addStep(new Statement() {
            public void evaluate() throws Throwable {
                DumbSlave s = story.j.createOnlineSlave();
                s.setLabelString("test performance test ");
                WorkflowJob p = story.j.createProject(WorkflowJob.class, "demo job");
                p.getRootDir().mkdirs();
                String bztParams =  path + ' ' + "-o modules.jmeter.plugins=[] -o services=[] " +
                        "-o \\'reporting.-1={module: \"junit-xml\", filename: \"report.xml\"}\\' " +
                        "-o \\'execution.0.scenario.requests.1={url: \"http://blazedemo.com/\": assert: [\"yo mamma\"]}\\'";
                p.setDefinition(new CpsFlowDefinition(
                        "node('master'){ bzt(params: '" + bztParams + "', useSystemSitePackages: false, printDebugOutput: true, bztVersion: '1.10.5') }", true));
                WorkflowRun r = p.scheduleBuild2(0).waitForStart();
                story.j.assertBuildStatusSuccess(story.j.waitForCompletion(r));
                story.j.assertLogContains("Writing JUnit XML report into: report.xml", r);
                story.j.assertLogContains("File aggregate-results.xml reported", r);
                story.j.assertLogContains("of errors [SUCCESS].", r);
                if (JenkinsRule.getLog(r).contains("Performance test: Installing bzt into 'taurus-venv'")) {
                    story.j.assertLogContains("Taurus CLI Tool v1.10.5", r);
                }
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
                        "node{ writeFile file: 'test.xml', text: '''" + report + "'''; perfReport errorFailedThreshold: 0, errorUnstableThreshold: 0, sourceDataFiles: 'test.xml' }", true));
                WorkflowRun r = p.scheduleBuild2(0).waitForStart();
                story.j.assertBuildStatus(Result.FAILURE, story.j.waitForCompletion(r));
                story.j.assertLogContains("File test.xml reported 1.625% of errors [FAILURE]. Build status is: FAILURE", r);
            }
        });
    }

}
