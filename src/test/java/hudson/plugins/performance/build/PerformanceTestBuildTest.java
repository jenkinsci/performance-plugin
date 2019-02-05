package hudson.plugins.performance.build;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import com.google.common.io.Files;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceProjectAction;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.tasks.Publisher;
import hudson.util.StreamTaskListener;
import jenkins.util.BuildListenerAdapter;


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

    @Test
    public void testInstallFromGit() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();
        String gitRepo = "git+https://github.com/Blazemeter/taurus.git";

        FreeStyleProject project = createFreeStyleProject();

        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        FilePath workspace = new FilePath(Files.createTempDir());
        buildExt.setWorkspace(workspace);
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();

        PerformanceTestBuildExt buildTest = new PerformanceTestBuildExt(new File(path).getAbsolutePath());
        buildTest.setGeneratePerformanceTrend(false);
        buildTest.setPrintDebugOutput(true);
        buildTest.setUseSystemSitePackages(false);
        buildTest.setUseBztExitCode(false);
        buildTest.setAlwaysUseVirtualenv(true);
        buildTest.setBztVersion(gitRepo);

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
        assertEquals(jobLog, 5, buildTest.commands.size());
        assertTrue(jobLog, Arrays.toString(buildTest.commands.get(buildTest.commands.size() - 3)).contains("install, " + gitRepo));
    }

    @Test
    public void testInstallFromURL() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();
        String url = "http://gettaurus.org/snapshots/bzt-1.9.5.1622.tar.gz";

        FreeStyleProject project = createFreeStyleProject();

        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        FilePath workspace = new FilePath(Files.createTempDir());
        buildExt.setWorkspace(workspace);
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();

        PerformanceTestBuildExt buildTest = new PerformanceTestBuildExt(new File(path).getAbsolutePath());
        buildTest.setGeneratePerformanceTrend(false);
        buildTest.setPrintDebugOutput(true);
        buildTest.setUseSystemSitePackages(false);
        buildTest.setUseBztExitCode(false);
        buildTest.setAlwaysUseVirtualenv(true);
        buildTest.setBztVersion(url);

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
        assertEquals(jobLog, 5, buildTest.commands.size());
        assertTrue(jobLog, Arrays.toString(buildTest.commands.get(buildTest.commands.size() - 3)).contains("install, " + url));
    }

    @Test
    public void testInstallFromPath() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();

        FreeStyleProject project = createFreeStyleProject();

        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        FilePath workspace = new FilePath(Files.createTempDir());
        buildExt.setWorkspace(workspace);
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();

        PerformanceTestBuildExt buildTest = new PerformanceTestBuildExt(new File(path).getAbsolutePath());
        buildTest.setGeneratePerformanceTrend(false);
        buildTest.setPrintDebugOutput(true);
        buildTest.setUseSystemSitePackages(false);
        buildTest.setUseBztExitCode(false);
        buildTest.setAlwaysUseVirtualenv(true);
        buildTest.setBztVersion(path);

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
        assertEquals(jobLog, 5, buildTest.commands.size());
        assertTrue(jobLog, Arrays.toString(buildTest.commands.get(buildTest.commands.size() - 3)).contains("install, " + path));
    }

    public static class PerformanceTestBuildExt extends PerformanceTestBuild {
        public PerformanceTestBuildExt(String params) {
            super(params);
        }

        public List<String[]> commands = new LinkedList<>();

        @Override
        public int runCmd(String[] commands, FilePath workspace, OutputStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
            this.commands.add(commands);
            return 0;
        }
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
        testBuild.setAlwaysUseVirtualenv(false);
        testBuild.setBztVersion("1.0.0.0.0");
        testBuild.setWorkingDirectory("workingDir");
        assertEquals("workingDir", testBuild.getWorkingDirectory());
        assertEquals("1.0.0.0.0", testBuild.getBztVersion());
        assertEquals("test option", testBuild.getParams());
        testBuild.setParams("test1");
        assertEquals("test1", testBuild.getParams());
        testBuild.setWorkspace("workingDirResolve");
        assertEquals("workingDirResolve", testBuild.getWorkingDirectory());
        assertEquals("", testBuild.getWorkspace());

        assertFalse(testBuild.isUseSystemSitePackages());
        assertFalse(testBuild.isPrintDebugOutput());
        assertFalse(testBuild.isGeneratePerformanceTrend());
        assertFalse(testBuild.isUseBztExitCode());
        assertFalse(testBuild.isAlwaysUseVirtualenv());
        testBuild.setGeneratePerformanceTrend(true);
        testBuild.setPrintDebugOutput(true);
        testBuild.setUseSystemSitePackages(true);
        testBuild.setUseBztExitCode(true);
        testBuild.setAlwaysUseVirtualenv(true);
        assertTrue(testBuild.isUseSystemSitePackages());
        assertTrue(testBuild.isPrintDebugOutput());
        assertTrue(testBuild.isGeneratePerformanceTrend());
        assertTrue(testBuild.isUseBztExitCode());
        assertTrue(testBuild.isAlwaysUseVirtualenv());
    }


    @Test
    public void testGenerateReportInPipe() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();

        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        FilePath workspace = new FilePath(Files.createTempDir());
        p.createExecutable();
        Run run = p.getFirstBuild();
        String args = new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[]";

        FilePath report = new FilePath(new File(workspace.getRemote(), "aggregate-results.xml"));
        report.copyFrom(getClass().getResource("/aggregate-results.xml"));


        PerformanceTestBuildExt buildTest = new PerformanceTestBuildExt(args);
        buildTest.setGeneratePerformanceTrend(true);
        buildTest.setPrintDebugOutput(true);
        buildTest.setUseSystemSitePackages(false);
        buildTest.setUseBztExitCode(false);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        StreamTaskListener taskListener = new StreamTaskListener(stream);
        buildTest.perform(run, workspace, createLocalLauncher(), new BuildListenerAdapter(taskListener));


        String jobLog = new String(stream.toByteArray());

        File reportFile = new File(run.getRootDir(), "performance-reports/Taurus/aggregate-results.xml");
        assertTrue("Report file " + reportFile.getAbsolutePath() + " expected to exist", reportFile.exists());
        assertTrue("Job log expected to contains 'Performance: Recording Taurus reports', jobLog:" + jobLog, 
                jobLog.contains("Performance: Recording Taurus reports"));
        assertTrue("Job log expected to contains 'aggregate-results.xml', jobLog:" + jobLog, jobLog.contains("aggregate-results.xml'"));
        assertTrue("Job log expected to contains 'Performance: Parsing report file ...', jobLog:" + jobLog, 
                jobLog.contains("Performance: Parsing report file '" + reportFile.getAbsolutePath() + "' with filterRegex '" 
                        + PerformanceReport.INCLUDE_ALL + "'."));
    }


    @Test
    public void testFailCriteria() throws Exception {
        String path = getClass().getResource("/performanceTestWithFailCriteria.yml").getPath();

        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        FilePath workspace = new FilePath(Files.createTempDir());
        p.createExecutable();
        Run run = p.getFirstBuild();
        String args = new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[]" ;

        PerformanceTestBuild buildTest = new PerformanceTestBuildExt(args) {
            @Override
            public int runCmd(String[] commands, FilePath workspace, OutputStream logger, Launcher launcher, EnvVars envVars) throws InterruptedException, IOException {
                for (String cmd : commands) {
                    if (cmd.contains("performanceTestWithFailCriteria.yml")) {
                        logger.write("Done performing with code: 3".getBytes());
                        return 3;
                    }
                }
                return super.runCmd(commands, workspace, logger, launcher, envVars);
            }
        };
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

    @Test
    public void testPWD() throws Exception {
        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        File buildWorkspace = Files.createTempDir();
        FilePath workspace = new FilePath(buildWorkspace);
        p.createExecutable();
        Run run = p.getFirstBuild();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        StreamTaskListener taskListener = new StreamTaskListener(stream);

        PerformanceTestBuildExt testBuild = new PerformanceTestBuildExt("");
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
        assertTrue(jobLog, jobLog.contains("Cannot create working directory because of error: Failed to mkdirs: /rootWorkspace"));
    }
}