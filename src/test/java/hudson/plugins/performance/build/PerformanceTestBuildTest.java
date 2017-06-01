package hudson.plugins.performance.build;

import com.google.common.io.Files;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceProjectAction;
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

        FreeStyleProject project = createFreeStyleProject();

        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        FilePath workspace = new FilePath(Files.createTempDir());
        buildExt.setWorkspace(workspace);
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[] -o modules.jmeter.version=3.1 -o modules.jmeter.path=" + workspace.getRemote());
        buildTest.setGeneratePerformanceTrend(true);
        buildTest.setPrintDebugOutput(true);
        buildTest.setUseSystemSitePackages(false);
        buildTest.setUseBztExitCode(false);

        assertEquals(PerformanceProjectAction.class, buildTest.getProjectAction((AbstractProject) project).getClass());


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
        public void setWorkspace(@Nonnull FilePath ws) {
            super.setWorkspace(ws);
        }

        @Override
        public void onStartBuilding() {
            super.onStartBuilding();
        }
    }

    @Test
    public void testGetters() throws Exception {
        PerformanceTestBuild.Descriptor descriptor = new PerformanceTestBuild.Descriptor();
        assertTrue(descriptor.isApplicable(null));

        PerformanceTestBuild testBuild = new PerformanceTestBuild("test option");
        testBuild.setGeneratePerformanceTrend(false);
        testBuild.setUseBztExitCode(false);
        testBuild.setUseSystemSitePackages(false);
        testBuild.setPrintDebugOutput(false);

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

        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        FilePath workspace = new FilePath(Files.createTempDir());
        p.createExecutable();
        Run run = p.getFirstBuild();
        String args = new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[] -o modules.jmeter.version=3.1 -o modules.jmeter.path=" + workspace.getRemote();


        PerformanceTestBuild buildTest = new PerformanceTestBuild(args);
        buildTest.setGeneratePerformanceTrend(true);
        buildTest.setPrintDebugOutput(true);
        buildTest.setUseSystemSitePackages(false);
        buildTest.setUseBztExitCode(false);

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

        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        FilePath workspace = new FilePath(Files.createTempDir());
        p.createExecutable();
        Run run = p.getFirstBuild();
        String args = new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[] -o modules.jmeter.version=3.1 -o modules.jmeter.path=" + workspace.getRemote();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(args);
        buildTest.setGeneratePerformanceTrend(false);
        buildTest.setPrintDebugOutput(true);
        buildTest.setUseSystemSitePackages(false);
        buildTest.setUseBztExitCode(true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        StreamTaskListener taskListener = new StreamTaskListener(stream);
        buildTest.perform(run, workspace, createLocalLauncher(), new BuildListenerAdapter(taskListener));

        String jobLog = new String(stream.toByteArray());
        assertEquals(jobLog, Result.UNSTABLE, run.getResult());
        assertTrue(jobLog, jobLog.contains("Done performing with code: 3"));
    }

    @Test
    public void testResutsChecker() throws Exception {
        PerformanceTestBuild testBuild = new PerformanceTestBuild("test option");
        testBuild.setGeneratePerformanceTrend(false);
        testBuild.setPrintDebugOutput(false);
        testBuild.setUseSystemSitePackages(false);
        testBuild.setUseBztExitCode(false);

        assertEquals(Result.SUCCESS, testBuild.getBztJobResult(0));
        assertEquals(Result.FAILURE, testBuild.getBztJobResult(1));
        assertEquals(Result.UNSTABLE, testBuild.getBztJobResult(3));

        assertEquals(Result.SUCCESS, testBuild.getJobResult(0));
        assertEquals(Result.FAILURE, testBuild.getJobResult(1));

        assertTrue(testBuild.isSuccessCode(0));
        assertFalse(testBuild.isSuccessCode(1));
    }

    public void testPWD() throws Exception {
        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        File buildWorkspace = Files.createTempDir();
        FilePath workspace = new FilePath(buildWorkspace);
        p.createExecutable();
        Run run = p.getFirstBuild();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        StreamTaskListener taskListener = new StreamTaskListener(stream);

        PerformanceTestBuild testBuild = new PerformanceTestBuild("");
        testBuild.setUseSystemSitePackages(false);
        testBuild.setPrintDebugOutput(true);

        // test absolute path
        String absoluteWorkspace = "/tmp/o/work/bzt";
        testBuild.setWorkspace(absoluteWorkspace);
        testBuild.perform(run, workspace,  createLocalLauncher(), new BuildListenerAdapter(taskListener));
        String jobLog = new String(stream.toByteArray());
        assertTrue(jobLog, new File(absoluteWorkspace).isDirectory());
        assertTrue(jobLog, new File(absoluteWorkspace).exists());
        assertTrue(jobLog, new File(absoluteWorkspace, "jenkins-report.yml").exists());

        // test relative path
        String relativeWorkspace = "oooooh/relative/path";
        testBuild.setWorkspace(relativeWorkspace);
        testBuild.perform(run, workspace,  createLocalLauncher(), new BuildListenerAdapter(taskListener));
        jobLog = new String(stream.toByteArray());
        assertTrue(jobLog, new File(buildWorkspace, relativeWorkspace).isDirectory());
        assertTrue(jobLog, new File(buildWorkspace, relativeWorkspace).exists());
        assertTrue(jobLog, new File(buildWorkspace, relativeWorkspace + "/jenkins-report.yml").exists());



        // test Permission denied
        String rootPath = "/rootWorkspace/";
        testBuild.setWorkspace(rootPath);
        testBuild.perform(run, workspace,  createLocalLauncher(), new BuildListenerAdapter(taskListener));
        jobLog = new String(stream.toByteArray());
        assertTrue(jobLog, jobLog.contains("Cannot create directory because of error: Failed to mkdirs: /rootWorkspace"));
    }
}