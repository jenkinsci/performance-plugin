package hudson.plugins.performance.workflow;

import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Result;
import hudson.plugins.performance.actions.PerformanceProjectAction;
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
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class WorkflowActionsFactoryTest {
    private final WorkflowActionsFactory factory = new WorkflowActionsFactory();

    @Test
    void testFlow(JenkinsRule rule) throws Exception {
        assertEquals(Job.class, factory.type());

        DumbSlave s = rule.createOnlineSlave();
        s.setLabelString("test performance report DSL function");
        WorkflowJob p = rule.jenkins.createProject(WorkflowJob.class, "demo");
        p.setDefinition(new CpsFlowDefinition(
                "node{ echo 'hi, world!' }", true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();
        rule.assertBuildStatus(Result.SUCCESS, rule.waitForCompletion(r));
        r = p.scheduleBuild2(1).waitForStart();
        rule.assertBuildStatus(Result.SUCCESS, rule.waitForCompletion(r));
        Collection<? extends Action> actions = factory.createFor(p);
        assertEquals(0, actions.size());
    }

    @Test
    void testFlowWithProjectAction(JenkinsRule rule) throws Exception {
        String fileContents = null;

        URL url = getClass().getResource("/TaurusXMLReport.xml");
        if (url != null) {
            fileContents = IOUtils.toString(url, StandardCharsets.UTF_8);
        }
        final String report = fileContents;

        DumbSlave s = rule.createOnlineSlave();
        s.setLabelString("test performance report DSL function");
        WorkflowJob p = rule.createProject(WorkflowJob.class, "demo2");
        p.setDefinition(new CpsFlowDefinition(
                "node{ writeFile file: 'test.xml', text: '''" + report
                        + "'''; perfReport errorFailedThreshold: 2, errorUnstableThreshold: 2, sourceDataFiles: 'test.xml' }",
                true));
        WorkflowRun r = p.scheduleBuild2(0).waitForStart();
        rule.assertBuildStatus(Result.SUCCESS, rule.waitForCompletion(r));
        r = p.scheduleBuild2(1).waitForStart();
        rule.assertBuildStatus(Result.SUCCESS, rule.waitForCompletion(r));
        Collection<? extends Action> actions = factory.createFor(p);
        assertEquals(1, actions.size());
        assertEquals(PerformanceProjectAction.class, actions.toArray()[0].getClass());
    }
}