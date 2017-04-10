package hudson.plugins.performance.build;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import jenkins.util.BuildListenerAdapter;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;


public class PerformanceTestBuildTest extends HudsonTestCase {

    @Test
    public void testFlow() throws Exception {
        String path = getClass().getResource("/performanceTest.yml").getPath();

        PerformanceTestBuild buildTest = new PerformanceTestBuild(path, "");
        FreeStyleProject project = createFreeStyleProject();
        FreeStyleBuildExt ext = new FreeStyleBuildExt(project);
        ext.setWorkspace(new FilePath(new File(".")));

        assertTrue(buildTest.perform((AbstractBuild<?,?>) ext, createLocalLauncher(), BuildListenerAdapter.wrap(createTaskListener())));
    }

    public static class FreeStyleBuildExt extends FreeStyleBuild {

        public FreeStyleBuildExt(FreeStyleProject project) throws IOException {
            super(project);
        }

        @Override
        protected void setWorkspace(@Nonnull FilePath ws) {
            super.setWorkspace(ws);
        }
    }
}