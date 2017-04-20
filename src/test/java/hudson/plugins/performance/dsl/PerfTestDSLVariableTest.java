package hudson.plugins.performance.dsl;

import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.DumbSlave;
import org.jenkinsci.plugins.workflow.SingleJobTestBase;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.junit.runners.model.Statement;

import java.util.Arrays;


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
                QueueTaskFuture<WorkflowRun> task = startBuilding();
                WorkflowRun run = task.get();
                String log = Arrays.toString(p.getBuilds().getLastBuild().getLog(Integer.MAX_VALUE).toArray());
                // means that jenkins called method in bzt.groovy
                assertTrue(log.contains("No such DSL method 'performanceTest'"));
            }
        });
    }

}