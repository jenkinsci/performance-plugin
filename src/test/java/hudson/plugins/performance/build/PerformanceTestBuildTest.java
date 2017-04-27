package hudson.plugins.performance.build;

import com.google.common.io.Files;
import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import jenkins.util.BuildListenerAdapter;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;


public class PerformanceTestBuildTest extends HudsonTestCase {

    @Test
    public void testFlow() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(new File(path).getAbsolutePath() + ' ' + "-o modules.jmeter.plugins=[] -o services=[]", false, false);
        FreeStyleProject project = createFreeStyleProject();
        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        buildExt.setWorkspace(new FilePath(Files.createTempDir()));
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();

        buildTest.perform(buildExt, buildExt.getWorkspace(), createLocalLauncher(), BuildListenerAdapter.wrap(createTaskListener()));


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

        PerformanceTestBuild testBuild = new PerformanceTestBuild("test option", false, false);
        assertEquals("test option", testBuild.getParams());
        testBuild.setParams("test1");
        assertEquals("test1", testBuild.getParams());

        assertFalse(testBuild.isUseSystemSitePackages());
        assertFalse(testBuild.isPrintDebugOutput());
        testBuild.setPrintDebugOutput(true);
        testBuild.setUseSystemSitePackages(true);
        assertTrue(testBuild.isUseSystemSitePackages());
        assertTrue(testBuild.isPrintDebugOutput());
    }
}