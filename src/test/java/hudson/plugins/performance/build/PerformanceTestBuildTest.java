package hudson.plugins.performance.build;

import com.google.common.io.Files;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.performance.PerformancePublisher;
import hudson.tasks.Publisher;
import hudson.util.StreamTaskListener;
import jenkins.util.BuildListenerAdapter;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;


public class PerformanceTestBuildTest extends HudsonTestCase {

    @Test
    public void testFlow() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[]", true, true, false, false);
        FreeStyleProject project = createFreeStyleProject();
        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        buildExt.setWorkspace(new FilePath(Files.createTempDir()));
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        StreamTaskListener taskListener = new StreamTaskListener(stream);
        buildTest.perform(buildExt, buildExt.getWorkspace(), createLocalLauncher(), new BuildListenerAdapter(taskListener));

        Iterator<Publisher> iterator = project.getPublishersList().iterator();
        StringBuilder builder = new StringBuilder("\n\nList publishers:\n");
        while (iterator.hasNext()) {
            builder.append(iterator.next().getClass().getName()).append("\n");
        }

        String jobLog = new String(stream.toByteArray()) + builder.toString();

        assertEquals(jobLog, Result.SUCCESS, buildExt.getResult());
    }


    public static class FreeStyleBuildExt extends FreeStyleBuild {

        public FreeStyleBuildExt(FreeStyleProject project) throws IOException {
            super(project);
        }

        @Override
        protected void setWorkspace(@Nonnull FilePath ws) {
            super.setWorkspace(ws);
        }

        @Override
        protected void onStartBuilding() {
            super.onStartBuilding();
        }
    }

    @Test
    public void testGetters() throws Exception {
        PerformanceTestBuild.Descriptor descriptor = new PerformanceTestBuild.Descriptor();
        assertTrue(descriptor.isApplicable(null));

        PerformanceTestBuild testBuild = new PerformanceTestBuild("test option", false, false, false, false);
        assertEquals("test option", testBuild.getParams());
        testBuild.setParams("test1");
        assertEquals("test1", testBuild.getParams());

        assertFalse(testBuild.isUseSystemSitePackages());
        assertFalse(testBuild.isPrintDebugOutput());
        assertFalse(testBuild.isGeneratePerformanceTrend());
        assertFalse(testBuild.isUseBztExitCode());
        testBuild.setGeneratePerformanceTrend(true);
        testBuild.setPrintDebugOutput(true);
        testBuild.setUseSystemSitePackages(true);
        testBuild.setUseBztExitCode(true);
        assertTrue(testBuild.isUseSystemSitePackages());
        assertTrue(testBuild.isPrintDebugOutput());
        assertTrue(testBuild.isGeneratePerformanceTrend());
        assertTrue(testBuild.isUseBztExitCode());
    }


    @Test
    public void testGenerateReportInPipe() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();
        String args = new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[]";

        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        FilePath workspace = new FilePath(Files.createTempDir());
        p.createExecutable();
        Run run = p.getFirstBuild();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(args, true, true, false, false);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        StreamTaskListener taskListener = new StreamTaskListener(stream);
        buildTest.perform(run, workspace, createLocalLauncher(), new BuildListenerAdapter(taskListener));


        String jobLog = new String(stream.toByteArray());
        File root = run.getRootDir();
        File reportFile = new File(root, "/performance-reports/Taurus/aggregate-results.xml");

        assertTrue(jobLog.contains("Performance: Recording Taurus reports 'aggregate-results.xml'"));
        assertTrue(jobLog, jobLog.contains("Performance: Parsing JMeter report file '" + reportFile.getAbsolutePath() + "'."));
        assertTrue(jobLog, reportFile.exists());
    }


    @Test
    public void testFailCriteria() throws Exception {
        String path = getClass().getResource("/performanceTestWithFailCriteria.yml").getPath();
        String args = new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[]";

        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        FilePath workspace = new FilePath(Files.createTempDir());
        p.createExecutable();
        Run run = p.getFirstBuild();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(args, false, true, false, true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        StreamTaskListener taskListener = new StreamTaskListener(stream);
        buildTest.perform(run, workspace, createLocalLauncher(), new BuildListenerAdapter(taskListener));

        String jobLog = new String(stream.toByteArray());
        assertEquals(jobLog, Result.UNSTABLE, run.getResult());
        assertTrue(jobLog, jobLog.contains("Done performing with code: 3"));
    }

    @Test
    public void testResutsChecker() throws Exception {
        PerformanceTestBuild testBuild = new PerformanceTestBuild("test option", false, false, false, false);

        assertEquals(Result.SUCCESS, testBuild.getBztJobResult(0));
        assertEquals(Result.FAILURE, testBuild.getBztJobResult(1));
        assertEquals(Result.UNSTABLE, testBuild.getBztJobResult(3));

        assertEquals(Result.SUCCESS, testBuild.getJobResult(0));
        assertEquals(Result.FAILURE, testBuild.getJobResult(1));

        assertTrue(testBuild.isSuccessCode(0));
        assertFalse(testBuild.isSuccessCode(1));
    }
}