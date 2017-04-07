package hudson.plugins.performance.build;

import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Run;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class PerformanceTestBuildTest extends HudsonTestCase {

    @Test
    public void testEmptyReportParsersList() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(path, "");
        PerformanceTestBuildTest.RunExt run = new PerformanceTestBuildTest.RunExt(createFreeStyleProject());
        run.onStartBuilding();
        try {
            buildTest.perform(run, new FilePath(new File(".")), createLocalLauncher(), createTaskListener());
        } catch (NullPointerException ex) {
            fail("Plugin must work with empty parser list" + ex.getMessage());
        }
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