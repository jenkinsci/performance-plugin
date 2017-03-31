package hudson.plugins.performance.dsl;

import hudson.slaves.DumbSlave;
import org.jenkinsci.plugins.workflow.SingleJobTestBase;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.junit.runners.model.Statement;

import java.util.Arrays;

public class PerfReportDSLVariableTest extends SingleJobTestBase {


    @Test
    public void smokeTests() throws Exception {
        this.story.addStep(new Statement() {
            public void evaluate() throws Throwable {
                DumbSlave s = createSlave(story.j);
                s.setLabelString("test performance report DSL function");
                p = jenkins().createProject(WorkflowJob.class, "demo");
                p.setDefinition(new CpsFlowDefinition(
                        "node{ perfReport 'test.xml' }"));
                startBuilding();
                waitForWorkflowToSuspend();
                String log = Arrays.toString(p.getBuilds().getLastBuild().getLog(Integer.MAX_VALUE).toArray());

                assertTrue(log.contains("Started"));
            }
        });
    }
}