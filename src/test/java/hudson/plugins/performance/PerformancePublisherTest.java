package hudson.plugins.performance;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.parsers.JMeterParser;
import hudson.plugins.performance.parsers.JUnitParser;
import hudson.plugins.performance.parsers.PerformanceReportParser;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

/**
 * @author Kohsuke Kawaguchi
 */
public class PerformancePublisherTest extends HudsonTestCase {

    @Test
    public void testBuild() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.jtl").copyFrom(
                        getClass().getResource("/JMeterResults.jtl"));
                return true;
            }
        });
        p.getPublishersList().add(
                new PerformancePublisher("", 0, 0, "", 0, 0, 0, 0, 0, false, "", false, false, false, Collections.<PerformanceReportParser>singletonList(new JMeterParser(
                        "**/*.jtl")), false));

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        PerformanceBuildAction a = b.getAction(PerformanceBuildAction.class);

        try {
            //assertNotNull(a);
            // poke a few random pages to verify rendering
            WebClient wc = createWebClient();
            wc.getPage(b, "performance");
            wc.getPage(b, "performance/uriReport/test.jtl:Home.endperformanceparameter/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBuildWithParameters() throws Exception {
        FreeStyleProject p = createFreeStyleProject("JobTest");
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("JobTest/test.jtl").copyFrom(
                        getClass().getResource("/JMeterResults.jtl"));
                return true;
            }
        });
        p.getPublishersList().add(
                new PerformancePublisher("", 0, 0, "", 0, 0, 0, 0, 0, false, "", false, false, false, Collections.<PerformanceReportParser>singletonList(new JMeterParser(
                        "${JOB_NAME}/*.jtl")), false));

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        PerformanceBuildAction a = b.getAction(PerformanceBuildAction.class);

        try {
            //assertNotNull(a);
            // poke a few random pages to verify rendering
            WebClient wc = createWebClient();
            wc.getPage(b, "performance");
            wc.getPage(b, "performance/uriReport/test.jtl:Home.endperformanceparameter/");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testBuildUnstableResponseThreshold() throws Exception {
        FreeStyleProject p = createFreeStyleProject("TestJob");
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.jtl").copyFrom(
                        getClass().getResource("/JMeterResults.jtl"));
                return true;
            }
        });
        p.getPublishersList().add(
                new PerformancePublisher("test.jtl", 0, 0, "test.jtl:100", 0, 0, 0, 0, 0, false, "", false, false, false, Collections.<PerformanceReportParser>singletonList(new JMeterParser(
                        "**/*.jtl")), false));

        FreeStyleBuild b = assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        PerformanceBuildAction a = b.getAction(PerformanceBuildAction.class);

        try {
            //assertNotNull(a);

            // poke a few random pages to verify rendering
            WebClient wc = createWebClient();
            wc.getPage(b, "performance");
            wc.getPage(b, "performance/uriReport/test.jtl:Home.endperformanceparameter/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBuildStableResponseThreshold() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.jtl").copyFrom(
                        getClass().getResource("/JMeterResults.jtl"));
                return true;
            }
        });
        p.getPublishersList().add(
                new PerformancePublisher("", 0, 0, "test.jtl:5000", 0, 0, 0, 0, 0, false, "", false, false, false, Collections.<PerformanceReportParser>singletonList(new JMeterParser(
                        "**/*.jtl")), false));

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        PerformanceBuildAction a = b.getAction(PerformanceBuildAction.class);

        try {
            //assertNotNull(a);

            // poke a few random pages to verify rendering
            WebClient wc = createWebClient();
            wc.getPage(b, "performance");
            wc.getPage(b, "performance/uriReport/test.jtl:Home.endperformanceparameter/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bug(22011)
    //TODO - Fix this test case, it was not compiling with forking over
    //and now its not passing due to second build being successful,
    //not failing due to threshold problems  Ignore flag not being
    //used, have to look at dependency tree, most likely pre 4.x
    //junit dependency
    @Ignore
    public void buildUnstableAverageResponseTimeRelativeThreshold() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        p.getPublishersList().add(
                new PerformancePublisher("", 0, 0, null, 100.0d, 0, 50.0d, 0, 0, false, "ART", true, false, true, Collections.<PerformanceReportParser>singletonList(new JUnitParser(
                        "**/*.xml")), false));
        // first build
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test1.xml").copyFrom(
                        getClass().getResource("/TEST-JUnitResults-relative-thrashould.xml"));
                return true;
            }
        });

        assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());


        // second build with high time
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test2.xml").copyFrom(
                        getClass().getResource("/TEST-JUnitResults-relative-thrashould-2.xml"));
                return true;
            }
        });

        assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());

    }

    @Test
    public void testEmptyReportParsersList() throws Exception {
        PerformancePublisher publisher = new PerformancePublisher("", 0, 0, "", 0.0, 0.0, 0.0, 0.0, 0, true, "MRT",
                true, true, true, Collections.<PerformanceReportParser>emptyList(), true);
        RunExt run = new RunExt( createFreeStyleProject());
        run.onStartBuilding();
        try {
            publisher.perform(run, new FilePath(new File(".")), createLocalLauncher(), createTaskListener());
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
