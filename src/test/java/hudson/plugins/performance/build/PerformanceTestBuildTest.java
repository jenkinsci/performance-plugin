package hudson.plugins.performance.build;

import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Run;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import hudson.model.Result;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

public class PerformanceTestBuildTest extends HudsonTestCase {

    @Test
    public void testFlow() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(path, "");
        PerformanceTestBuildTest.RunExt run = new PerformanceTestBuildTest.RunExt(createFreeStyleProject());
        run.onStartBuilding();
        buildTest.perform(run, new FilePath(new File(".")), createLocalLauncher(), createTaskListener());
        assertEquals(Result.SUCCESS, run.getResult());
    }

    private class RunExt extends Run {

        protected RunExt(@Nonnull Job job) throws IOException {
            super(job);
        }

        @Override
        protected void onStartBuilding() {
            super.onStartBuilding();
        }
    }
}