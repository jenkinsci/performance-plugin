package hudson.plugins.performance.build;

import com.google.common.io.Files;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.performance.PerformancePublisher;
import jenkins.util.BuildListenerAdapter;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;


public class PerformanceTestBuildTest extends HudsonTestCase {

    @Test
    public void testFlow() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[]", true, true, false);
        FreeStyleProject project = createFreeStyleProject();
        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        buildExt.setWorkspace(new FilePath(Files.createTempDir()));
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();

        buildTest.perform(buildExt, buildExt.getWorkspace(), createLocalLauncher(), BuildListenerAdapter.wrap(createTaskListener()));

        assertNotNull(project.getPublishersList().get(PerformancePublisher.class));
        assertEquals("aggregate-results.xml", project.getPublishersList().get(PerformancePublisher.class).getSourceDataFiles());

        assertEquals(Result.SUCCESS, buildExt.getResult());
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

        PerformanceTestBuild testBuild = new PerformanceTestBuild("test option", false, false, false);
        assertEquals("test option", testBuild.getParams());
        testBuild.setParams("test1");
        assertEquals("test1", testBuild.getParams());

        assertFalse(testBuild.isUseSystemSitePackages());
        assertFalse(testBuild.isPrintDebugOutput());
        assertFalse(testBuild.isGeneratePerformanceTrend());
        testBuild.setGeneratePerformanceTrend(true);
        testBuild.setPrintDebugOutput(true);
        testBuild.setUseSystemSitePackages(true);
        assertTrue(testBuild.isUseSystemSitePackages());
        assertTrue(testBuild.isPrintDebugOutput());
        assertTrue(testBuild.isGeneratePerformanceTrend());
    }


    @Test
    public void testGenerateReportInPipe() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();
        String args = new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[]";

        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "p");
        FilePath workspace = new FilePath(Files.createTempDir());
        p.createExecutable();
        Run run = p.getFirstBuild();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(args, true, true, false);
        buildTest.perform(run, workspace, createLocalLauncher(), BuildListenerAdapter.wrap(createTaskListener()));

        File root = run.getRootDir();

        File reportFile = new File(root, "/performance-reports/Taurus/aggregate-results.xml");
        assertTrue(reportFile.exists());
    }
}