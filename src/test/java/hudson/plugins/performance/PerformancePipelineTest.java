package hudson.plugins.performance;

import hudson.model.Result;
import hudson.slaves.DumbSlave;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.net.URL;
import java.nio.charset.StandardCharsets;

@WithJenkins
class PerformancePipelineTest {

    @Test
    void bztSmokeTests(JenkinsRule rule) throws Exception {
        final String path = getClass().getResource("/performanceTest.yml").getPath();
        DumbSlave s = rule.createOnlineSlave();
        s.setLabelString("test performance test ");
        WorkflowJob p = rule.createProject(WorkflowJob.class, "demo job");
        p.getRootDir().mkdirs();
        String bztParams = path + ' ' + "-o modules.jmeter.plugins=[] -o services=[] " +
                "-o \\'reporting.-1={module: \"junit-xml\", filename: \"report.xml\"}\\' " +
                "-o \\'execution.0.scenario.requests.1={url: \"http://blazedemo.com/\": assert: [\"yo mamma\"]}\\'";
        p.setDefinition(new CpsFlowDefinition(
                "node('" + rule.jenkins.getSelfLabel().getName() + "'){ bzt(params: '" + bztParams
                        + "', useSystemSitePackages: false, printDebugOutput: true, bztVersion: '1.16.19') }",
                true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();
        rule.assertBuildStatusSuccess(rule.waitForCompletion(r));
        rule.assertLogContains("Writing JUnit XML report into: report.xml", r);
        rule.assertLogContains("File aggregate-results.xml reported", r);
        rule.assertLogContains("of errors [SUCCESS].", r);
        if (JenkinsRule.getLog(r).contains("Performance test: Installing bzt into 'taurus-venv'")) {
            rule.assertLogContains("Taurus CLI Tool v1.16.19", r);
        }
    }

    @Test
    void perfReportSmokeTests(JenkinsRule rule) throws Exception {
        String fileContents = null;

        URL url = getClass().getResource("/TaurusXMLReport.xml");
        if (url != null) {
            fileContents = IOUtils.toString(url, StandardCharsets.UTF_8);
        }

        final String report = fileContents;

        DumbSlave s = rule.createOnlineSlave();
        s.setLabelString("test performance report DSL function");
        WorkflowJob p = rule.createProject(WorkflowJob.class, "demo");
        p.setDefinition(new CpsFlowDefinition(
                "node{ writeFile file: 'test.xml', text: '''" + report
                        + "'''; perfReport errorFailedThreshold: 0, errorUnstableThreshold: 0, sourceDataFiles: 'test.xml' }",
                true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();
        rule.assertBuildStatus(Result.FAILURE, rule.waitForCompletion(r));
        rule.assertLogContains("File test.xml reported 1.625% of errors [FAILURE]. Build status is: FAILURE", r);
    }

}
