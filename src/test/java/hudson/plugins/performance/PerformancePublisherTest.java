package hudson.plugins.performance;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.constraints.AbstractConstraint;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
                new PerformancePublisher("", 0, 0, "", 0, 0, 0, 0, 0, false, "", false, false, false, false, null));

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
                new PerformancePublisher("", 0, 0, "", 0, 0, 0, 0, 0, false, "", false, false, false, false, null));

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
                new PerformancePublisher("test.jtl", 0, 0, "test.jtl:100", 0, 0, 0, 0, 0, false, "", false, false, false, false, null));

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
                new PerformancePublisher("", 0, 0, "test.jtl:5000", 0, 0, 0, 0, 0, false, "", false, false, false, false, null));

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
                new PerformancePublisher("", 0, 0, null, 100.0d, 0, 50.0d, 0, 0, false, "ART", true, false, true, false, null));
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
                true, true, true, true, null);
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

    @Test
    public void testOptionMethods() throws Exception {
        final double DELTA = 0.001;
        PerformancePublisher publisher = new PerformancePublisher("reportFile.xml", 15, 16, "reportFile.xml:100", 9.0, 8.0, 7.0, 6.0, 3, true, "MRT",
                true, true, true, true, null);
        assertEquals("reportFile.xml", publisher.getSourceDataFiles());
        assertEquals(15, publisher.getErrorFailedThreshold());
        assertEquals(16, publisher.getErrorUnstableThreshold());
        assertEquals("reportFile.xml:100", publisher.getErrorUnstableResponseTimeThreshold());
        assertEquals(9.0, publisher.getRelativeFailedThresholdPositive(), DELTA);
        assertEquals(8.0, publisher.getRelativeFailedThresholdNegative(), DELTA);
        assertEquals(7.0, publisher.getRelativeUnstableThresholdPositive(), DELTA);
        assertEquals(6.0, publisher.getRelativeUnstableThresholdNegative(), DELTA);
        assertEquals(3, publisher.getNthBuildNumber());
        assertTrue(publisher.getModePerformancePerTestCase());
        assertEquals("MRT", publisher.getConfigType());
        assertTrue(publisher.getModeOfThreshold());
        assertTrue(publisher.isFailBuildIfNoResultFile());
        assertTrue(publisher.getCompareBuildPrevious());
        assertTrue(publisher.isModeThroughput());

        publisher.setSourceDataFiles("newReportFile.xml");
        publisher.setErrorFailedThreshold(0);
        publisher.setErrorUnstableThreshold(0);
        publisher.setErrorUnstableResponseTimeThreshold("newReportFile.xml:101");
        publisher.setRelativeFailedThresholdPositive(0.0);
        publisher.setRelativeFailedThresholdNegative(0.0);
        publisher.setRelativeUnstableThresholdPositive(0.0);
        publisher.setRelativeUnstableThresholdNegative(0.0);
        publisher.setNthBuildNumber(0);
        publisher.setModePerformancePerTestCase(false);
        publisher.setConfigType("ART");
        publisher.setModeOfThreshold(false);
        publisher.setFailBuildIfNoResultFile(false);
        publisher.setCompareBuildPrevious(false);
        publisher.setModeThroughput(false);

        assertEquals("newReportFile.xml", publisher.getSourceDataFiles());
        assertEquals(0, publisher.getErrorFailedThreshold());
        assertEquals(0, publisher.getErrorUnstableThreshold());
        assertEquals("newReportFile.xml:101", publisher.getErrorUnstableResponseTimeThreshold());
        assertEquals(0.0, publisher.getRelativeFailedThresholdPositive(), DELTA);
        assertEquals(0.0, publisher.getRelativeFailedThresholdNegative(), DELTA);
        assertEquals(0.0, publisher.getRelativeUnstableThresholdPositive(), DELTA);
        assertEquals(0.0, publisher.getRelativeUnstableThresholdNegative(), DELTA);
        assertEquals(0, publisher.getNthBuildNumber());
        assertFalse(publisher.getModePerformancePerTestCase());
        assertEquals("ART", publisher.getConfigType());
        assertFalse(publisher.getModeOfThreshold());
        assertFalse(publisher.isFailBuildIfNoResultFile());
        assertFalse(publisher.getCompareBuildPrevious());
        assertFalse(publisher.isModeThroughput());

        publisher.setModeEvaluation(true);
        assertTrue(publisher.isModeEvaluation());
        publisher.setPersistConstraintLog(true);
        assertTrue(publisher.isPersistConstraintLog());
        publisher.setIgnoreUnstableBuilds(true);
        assertTrue(publisher.isIgnoreUnstableBuilds());
        publisher.setIgnoreFailedBuilds(true);
        assertTrue(publisher.isIgnoreFailedBuilds());
        publisher.setModeRelativeThresholds(true);
        assertTrue(publisher.getModeRelativeThresholds());
        List<AbstractConstraint> allConstraints = AbstractConstraint.all();
        publisher.setConstraints(allConstraints);
        assertEquals(allConstraints, publisher.getConstraints());
        assertEquals(PerformancePublisher.optionType, PerformancePublisher.getOptionType());
    }
}
