package hudson.plugins.performance.dsl;

import hudson.model.Result;
import hudson.slaves.DumbSlave;
import org.jenkinsci.plugins.workflow.SingleJobTestBase;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsFlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.junit.runners.model.Statement;


public class PerfTestDSLVariableTest extends SingleJobTestBase {


    @Test
    public void smokeTests() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();
        final String bztParams =  path + ' ' + "-o modules.jmeter.plugins=[] -o services=[]";

        this.story.addStep(new Statement() {
            public void evaluate() throws Throwable {
                DumbSlave s = createSlave(story.j);
                s.setLabelString("test performance test ");
                p = jenkins().createProject(WorkflowJob.class, "demo");
                p.setDefinition(new CpsFlowDefinition(
                        "node{ bzt '" + bztParams + "' }"));
                startBuilding();
                waitForWorkflowToSuspend();

                FlowExecution execution = p.getLastBuild().getExecution();
                assertTrue(execution instanceof CpsFlowExecution);
                assertEquals(Result.SUCCESS, ((CpsFlowExecution) execution).getResult());
            }
        });
    }

}