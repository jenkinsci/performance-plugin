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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.net.URL;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class WorkflowActionsFactoryTest {
    private final WorkflowActionsFactory factory = new WorkflowActionsFactory();

    @Rule
    public RestartableJenkinsRule story = new RestartableJenkinsRule();

    @Test
    public void testFlow() throws Exception {
        assertEquals(Job.class, factory.type());

        story.addStep(new Statement() {
            public void evaluate() throws Throwable {
                DumbSlave s = story.j.createOnlineSlave();
                s.setLabelString("test performance report DSL function");
                WorkflowJob p = story.j.createProject(WorkflowJob.class, "demo");
                p.setDefinition(new CpsFlowDefinition(
                        "node{ echo 'hi, world!' }", true));
                WorkflowRun r = p.scheduleBuild2(0).waitForStart();
                story.j.assertBuildStatus(Result.SUCCESS, story.j.waitForCompletion(r));
                r = p.scheduleBuild2(1).waitForStart();
                story.j.assertBuildStatus(Result.SUCCESS, story.j.waitForCompletion(r));
                Collection<? extends Action> actions = factory.createFor(p);
                assertEquals(0, actions.size());
            }
        });
    }

    @Test
    public void testFlowWithProjectAction() throws Exception {
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
                WorkflowJob p = story.j.createProject(WorkflowJob.class, "demo2");
                p.setDefinition(new CpsFlowDefinition(
                        "node{ writeFile file: 'test.xml', text: '''" + report + "'''; perfReport errorFailedThreshold: 2, errorUnstableThreshold: 2, sourceDataFiles: 'test.xml' }", true));
                WorkflowRun r = p.scheduleBuild2(0).waitForStart();
                story.j.assertBuildStatus(Result.SUCCESS, story.j.waitForCompletion(r));
                r = p.scheduleBuild2(1).waitForStart();
                story.j.assertBuildStatus(Result.SUCCESS, story.j.waitForCompletion(r));
                Collection<? extends Action> actions = factory.createFor(p);
                assertEquals(1, actions.size());
                assertEquals(PerformanceProjectAction.class, actions.toArray()[0].getClass());
            }
        });
    }
}