package hudson.plugins.performance;

import com.google.common.io.Files;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.build.PerformanceTestBuildTest;
import hudson.plugins.performance.constraints.AbstractConstraint;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.parsers.JMeterCsvParser;
import hudson.plugins.performance.parsers.JMeterParser;
import hudson.plugins.performance.parsers.PerformanceReportParser;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;
import hudson.plugins.performance.reports.UriReport;
import hudson.util.StreamTaskListener;
import jenkins.util.BuildListenerAdapter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    public void testStandardResultsXML() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("test.jtl").copyFrom(getClass().getResource("/JMeterResults.jtl"));
                return true;
            }
        });

        List<PerformanceReportParser> parsers = new ArrayList<PerformanceReportParser>();
        parsers.add(new JMeterParser("test.jtl", PerformanceReportTest.DEFAULT_PERCENTILES));

        PerformancePublisher publisher = new PerformancePublisher("", 0, 0, "", 0, 0, 0, 0, 0, false, "", false, false, false, false, parsers);
        publisher.setModeEvaluation(false);
        p.getPublishersList().add(publisher);

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0).get());
        b.getAction(PerformanceBuildAction.class);

        String standardExportFilename = b.getRootDir().getAbsolutePath() + File.separator + "archive" + File.separator + "standardResults.xml";
        String content = new String (java.nio.file.Files.readAllBytes(Paths.get(standardExportFilename)));
        Assert.assertEquals("<?xml version=\"1.0\"?>\n" +
                "<results>\n" +
                "<api>\n" +
                "\t<uri>Home</uri>\n" +
                "\t<samples>4</samples>\n" +
                "\t<average>7930</average>\n" +
                "\t<min>501</min>\n" +
                "\t<median>598</median>\n" +
                "\t<ninetieth>14720</ninetieth>\n" +
                "\t<max>15902</max>\n" +
                "\t<httpCode>200</httpCode>\n" +
                "\t<errors>0.0</errors>\n" +
                "</api>\n" +
                "<api>\n" +
                "\t<uri>Workgroup</uri>\n" +
                "\t<samples>4</samples>\n" +
                "\t<average>354</average>\n" +
                "\t<min>58</min>\n" +
                "\t<median>63</median>\n" +
                "\t<ninetieth>278</ninetieth>\n" +
                "\t<max>1017</max>\n" +
                "\t<httpCode>200</httpCode>\n" +
                "\t<errors>0.0</errors>\n" +
                "</api>\n" +
                "</results>\n", content);
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
        assertTrue(publisher.isModePerformancePerTestCase());
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
        assertFalse(publisher.isModePerformancePerTestCase());
        assertFalse(publisher.isModePerformancePerTestCase());
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
        List<AbstractConstraint> allConstraints = AbstractConstraint.all();
        publisher.setConstraints(allConstraints);
        assertEquals(allConstraints, publisher.getConstraints());

        publisher = new PerformancePublisher("reportFile.xml", 15, 16, "reportFile.xml:100", 9.0, 8.0, 7.0, 6.0, 3, true, "MRT",
                true, true, true, true, null);
        assertTrue(publisher.isMRT());
        publisher = new PerformancePublisher("reportFile.xml", 15, 16, "reportFile.xml:100", 9.0, 8.0, 7.0, 6.0, 3, true, "ART",
                true, true, true, true, null);
        assertTrue(publisher.isART());
        publisher = new PerformancePublisher("reportFile.xml", 15, 16, "reportFile.xml:100", 9.0, 8.0, 7.0, 6.0, 3, true, "PRT",
                true, true, true, true, null);
        assertTrue(publisher.isPRT());

        publisher.setFilename("testfilename");
        assertEquals("testfilename", publisher.getFilename());

        List<PerformanceReportParser> emptyList = Collections.emptyList();
        publisher.setParsers(emptyList);
        assertEquals(emptyList, publisher.getParsers());
    }

    @Test
    public void testErrorThresholdUnstable() throws Exception {

        PerformancePublisher publisherUnstable = new PerformancePublisher("JMeterPublisher.csv",
                -1,
                1,  // errorUnstableThreshold
                "", 0.0, 0.0, 0.0, 0.0, 1, true, "MRT",
                false, // modeOfThreshold (false = Error Threshold)
                true, true, true, null);

        FreeStyleProject project = createFreeStyleProject();

        PerformanceTestBuildTest.FreeStyleBuildExt buildExt = new PerformanceTestBuildTest.FreeStyleBuildExt(project);
        buildExt.setWorkspace(new FilePath(Files.createTempDir()));
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();
        buildExt.getWorkspace().child("JMeterPublisher.csv").copyFrom(
                getClass().getResource("/JMeterPublisher.csv"));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        publisherUnstable.perform(buildExt, buildExt.getWorkspace(), createLocalLauncher(), new BuildListenerAdapter(new StreamTaskListener(stream)));

        String log = new String(stream.toByteArray());
        assertEquals(log, Result.UNSTABLE, buildExt.getResult());
        assertTrue(log, log.contains("Performance: File JMeterPublisher.csv reported 33.333% of errors [UNSTABLE]. Build status is: UNSTABLE"));
    }

    @Test
    public void testErrorThresholdFailed() throws Exception {

        PerformancePublisher publisherFailed = new PerformancePublisher("JMeterPublisher.csv",
                 2, //errorFailedThreshold
                -1, "", 0.0, 0.0, 0.0, 0.0, 1, true, "MRT",
                false, // modeOfThreshold (false = Error Threshold)
                true, true, true, null);

        FreeStyleProject project = createFreeStyleProject();

        PerformanceTestBuildTest.FreeStyleBuildExt buildExt = new PerformanceTestBuildTest.FreeStyleBuildExt(project);
        buildExt.setWorkspace(new FilePath(Files.createTempDir()));
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();
        buildExt.getWorkspace().child("JMeterPublisher.csv").copyFrom(
                getClass().getResource("/JMeterPublisher.csv"));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        publisherFailed.perform(buildExt, buildExt.getWorkspace(), createLocalLauncher(), new BuildListenerAdapter(new StreamTaskListener(stream)));

        String log = new String(stream.toByteArray());
        assertEquals(log, Result.FAILURE, buildExt.getResult());
        assertTrue(log, log.contains("Performance: File JMeterPublisher.csv reported 33.333% of errors [FAILURE]. Build status is: FAILURE"));
    }

    @Test
    public void testErrorThresholdAverageResponseTime() throws Exception {

        PerformancePublisher publisherART = new PerformancePublisher("JMeterPublisher.csv", -1, -1,
                "JMeterPublisher.csv:1000", 0.0, 0.0, 0.0, 0.0, 1, true, "MRT",
                false, // modeOfThreshold (false = Error Threshold)
                true, true, true, null);

        FreeStyleProject project = createFreeStyleProject();

        PerformanceTestBuildTest.FreeStyleBuildExt buildExt = new PerformanceTestBuildTest.FreeStyleBuildExt(project);
        buildExt.setWorkspace(new FilePath(Files.createTempDir()));
        buildExt.onStartBuilding();

        buildExt.getRootDir().mkdirs();
        buildExt.getWorkspace().child("JMeterPublisher.csv").copyFrom(
                getClass().getResource("/JMeterPublisher.csv"));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        publisherART.perform(buildExt, buildExt.getWorkspace(), createLocalLauncher(), new BuildListenerAdapter(new StreamTaskListener(stream)));

        String log = new String(stream.toByteArray());
        assertEquals(log, Result.UNSTABLE, buildExt.getResult());
        assertTrue(log, log.contains("UNSTABLE: JMeterPublisher.csv has exceeded the threshold of [1000] with the time of [1433]"));


        // For Number Format Exception
        publisherART.setErrorUnstableResponseTimeThreshold("JMeterPublisher.csv:1!00");
        stream = new ByteArrayOutputStream();
        publisherART.perform(buildExt, buildExt.getWorkspace(), createLocalLauncher(), new BuildListenerAdapter(new StreamTaskListener(stream)));

        log = new String(stream.toByteArray());
        assertEquals(log, Result.FAILURE, buildExt.getResult());
        assertTrue(log, log.contains("ERROR: Threshold set to a non-number [1!00]"));
    }

    @Test
    public void testMigration() throws Exception {
        List<PerformanceReportParser> parsers = new ArrayList<PerformanceReportParser>();
        parsers.add(new JMeterCsvParser("test1", PerformanceReportTest.DEFAULT_PERCENTILES));
        parsers.add(new JMeterParser("test2", PerformanceReportTest.DEFAULT_PERCENTILES));

        PerformancePublisher publisher = new PerformancePublisher("", -1, -1, "", 0.0, 0.0, 0.0, 0.0, 1, true, "MRT", false, true, true, true, parsers);

        assertEquals("test1;test2", publisher.getSourceDataFiles());
        assertNull(publisher.getParsers());
        publisher.setSourceDataFiles("");
        assertEquals("", publisher.getSourceDataFiles());

        publisher.setParsers(parsers);
        publisher.setFilename("test3");
        publisher.readResolve();
        assertEquals("test1;test2;test3", publisher.getSourceDataFiles());
        assertNull(publisher.getParsers());
    }

    @Test
    public void testRelativeThresholdUnstableNegative() throws Exception {

        FreeStyleProject p = createFreeStyleProject();

        PerformancePublisher publisher = new PerformancePublisher("JMeterCsvResults.csv", -1, -1, "", -0.1, -0.1, -0.1,
                5.0, // relativeUnstableThresholdNegative
                1, true, "MRT",
                true, // modeOfThreshold (true = Relative Threshold)
                true, true, true, null);

        p.getPublishersList().add(publisher);
        // first build
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("JMeterCsvResults.csv").copyFrom(
                        getClass().getResource("/JMeterCsvResults.csv"));
                build.getWorkspace().child("JMeterPublisher.csv").copyFrom(
                        getClass().getResource("/JMeterPublisher.csv"));

                return true;
            }
        });

        assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        publisher.setSourceDataFiles("JMeterPublisher.csv");
        assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
    }

    @Test
    public void testRelativeThresholdUnstablePositive() throws Exception {

        FreeStyleProject p = createFreeStyleProject();

        PerformancePublisher publisher = new PerformancePublisher("JMeterPublisher.csv", -1, -1, "", -0.1, -0.1,
                9.0, // relativeUnstableThresholdPositive
                -0.1, // relativeUnstableThresholdNegative
                1, true, "ART",
                true, // modeOfThreshold (true = Relative Threshold)
                true, true, true, null);

        p.getPublishersList().add(publisher);
        // first build
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("JMeterCsvResults.csv").copyFrom(
                        getClass().getResource("/JMeterCsvResults.csv"));
                build.getWorkspace().child("JMeterPublisher.csv").copyFrom(
                        getClass().getResource("/JMeterPublisher.csv"));

                return true;
            }
        });

        assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        publisher.setSourceDataFiles("JMeterCsvResults.csv");
        assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
    }

    @Test
    public void testRelativeThresholdFailedNegative() throws Exception {

        FreeStyleProject p = createFreeStyleProject();

        PerformancePublisher publisher = new PerformancePublisher("JMeterCsvResults.csv", -1, -1, "", -0.1,
                5.1, // relativeFailedThresholdNegative
                -0.1, -0.1, 1, true, "PRT",
                true, // modeOfThreshold (true = Relative Threshold)
                true, false, // false - means compare with Build Number
                true, null);

        p.getPublishersList().add(publisher);
        // first build
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("JMeterCsvResults.csv").copyFrom(
                        getClass().getResource("/JMeterCsvResults.csv"));
                build.getWorkspace().child("JMeterPublisher.csv").copyFrom(
                        getClass().getResource("/JMeterPublisher.csv"));

                return true;
            }
        });

        assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        publisher.setSourceDataFiles("JMeterPublisher.csv");
        assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
    }

    @Test
    public void testRelativeThresholdFailedPositive() throws Exception {

        FreeStyleProject p = createFreeStyleProject();

        PerformancePublisher publisher = new PerformancePublisher("JMeterPublisher.csv", -1, -1, "",
                5.1, // relativeFailedThresholdPositive
                -0.1, -0.1, -0.1, 1, true, "PRT",
                true, // modeOfThreshold (true = Relative Threshold)
                true, false, // false - means compare with Build Number
                true, null);

        p.getPublishersList().add(publisher);
        // first build
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("JMeterCsvResults.csv").copyFrom(
                        getClass().getResource("/JMeterCsvResults.csv"));
                build.getWorkspace().child("JMeterPublisher.csv").copyFrom(
                        getClass().getResource("/JMeterPublisher.csv"));

                return true;
            }
        });

        assertBuildStatus(Result.SUCCESS, p.scheduleBuild2(0).get());

        publisher.setSourceDataFiles("JMeterCsvResults.csv");
        assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
    }

    public void testRelativeThresholds() throws Exception {
        FreeStyleProject freeStyleProject = createFreeStyleProject();

        final RunExt prevBuild = new RunExt(freeStyleProject);
        prevBuild.onStartBuilding();

        PerformancePublisher publisher = new PerformancePublisher("") {
            @Override
            protected List<PerformanceReportParser> getParsers(Run<?, ?> build, FilePath workspace, PrintStream logger, EnvVars env) throws IOException, InterruptedException {
                List<PerformanceReportParser> parsers = new ArrayList<>();
                parsers.add(new JMeterCsvParser("", ""));
                return parsers;
            }

            @Override
            public Collection<PerformanceReport> prepareEvaluation(Run<?, ?> run, FilePath workspace, TaskListener listener, List<PerformanceReportParser> parsers) throws IOException, InterruptedException {
                List<PerformanceReport> reports = new ArrayList<>();
                reports.add(new PerformanceReport());
                reports.add(new PerformanceReport());
                return reports;
            }

            @Override
            protected List<UriReport> getBuildUriReports(Run<?, ?> build, FilePath workspace, TaskListener listener, List<PerformanceReportParser> parsers, boolean locatePerformanceReports) throws IOException, InterruptedException {
                List<UriReport> uriReports = new ArrayList<>();
                if (locatePerformanceReports) {
                    UriReport report = new UriReport(new PerformanceReport(), "aaaaa", "bbbbb");
                    TaurusFinalStats taurusFinalStats = new TaurusFinalStats();
                    taurusFinalStats.setAverageResponseTime(100d);
                    report.setFromTaurusFinalStats(taurusFinalStats);
                    uriReports.add(report);
                } else {
                    UriReport report = new UriReport(new PerformanceReport(), "aaaaa", "bbbbb");
                    TaurusFinalStats taurusFinalStats = new TaurusFinalStats();
                    taurusFinalStats.setAverageResponseTime(200d);
                    report.setFromTaurusFinalStats(taurusFinalStats);
                    uriReports.add(report);
                }
                return uriReports;
            }

            @Override
            public Run<?, ?> getnthBuild(Run<?, ?> build) {
                return prevBuild;
            }
        };
        publisher.setModeOfThreshold(true);
        publisher.setRelativeFailedThresholdPositive(10);
        publisher.setRelativeUnstableThresholdPositive(5);

        RunExt run1 = new RunExt(freeStyleProject);
        run1.onStartBuilding();
        publisher.perform(run1, new FilePath(new File(".")), createLocalLauncher(), createTaskListener());
        assertEquals(Result.SUCCESS, run1.getResult());

        publisher.setRelativeUnstableThresholdNegative(10);
        RunExt run2 = new RunExt(freeStyleProject);
        run2.onStartBuilding();
        publisher.perform(run2, new FilePath(new File(".")), createLocalLauncher(), createTaskListener());
        assertEquals(Result.UNSTABLE, run2.getResult());

        publisher.setRelativeFailedThresholdNegative(10);
        RunExt run3 = new RunExt(freeStyleProject);
        run3.onStartBuilding();
        publisher.perform(run3, new FilePath(new File(".")), createLocalLauncher(), createTaskListener());
        assertEquals(Result.FAILURE, run3.getResult());
    }
}
