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

        PerformanceTestBuild buildTest = new PerformanceTestBuild(path + ' ' + "-o modules.jmeter.plugins=[]");
        FreeStyleProject project = createFreeStyleProject();
        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        buildExt.setWorkspace(new FilePath(Files.createTempDir()));
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();

        buildTest.perform(buildExt, buildExt.getWorkspace(), createLocalLauncher(), BuildListenerAdapter.wrap(createTaskListener()));
        assertEquals(Result.SUCCESS, buildExt.getResult());
    }

    @Test
    public void testLogging() throws Exception {

        PerformanceTestBuild buildTest = new PerformanceTestBuild("--help");

        FreeStyleProject project = createFreeStyleProject();
        FreeStyleBuildExt buildExt = new FreeStyleBuildExt(project);
        buildExt.setWorkspace(new FilePath(Files.createTempDir()));
        buildExt.onStartBuilding();
        buildExt.getRootDir().mkdirs();


        PrintStream out = buildTest.getPerformanceTestLogger(buildExt, System.out);
        out.println("Test log!");
        out.close();

        File tmpFile = File.createTempFile("temp.log", "");
        PrintStream actualLog = new PrintStream(tmpFile);
        buildTest.printPerformanceTestLog(buildExt, actualLog);
        actualLog.close();

        String actualLine = new BufferedReader(new FileReader(tmpFile)).readLine();
        assertEquals("Test log!", actualLine);
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
}