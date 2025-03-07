package hudson.plugins.performance.constraints;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.constraints.AbstractConstraint.Escalation;
import hudson.plugins.performance.constraints.AbstractConstraint.Metric;
import hudson.plugins.performance.constraints.AbstractConstraint.Operator;
import hudson.plugins.performance.constraints.blocks.TestCaseBlock;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@WithJenkins
class ConstraintTest {

    /**
     * Testing: Escalation.INFORMATION
     * Build must stay successful if the value is exceeded.
     * <p>
     * Specified value: 1
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void informationModeDoesntAffectBuildStatus(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // Value set to 1L to violate constraint. Due to Escalation.INFORMATION the build status must be SUCCESS.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "testResult.xml", Escalation.INFORMATION, false, testCaseBlock, 1L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);

        PerformancePublisher performancePublisher = new PerformancePublisher("", 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);
        performancePublisher.setFailBuildIfNoResultFile(false);

        FreeStyleProject p = j.createFreeStyleProject("informationModeDoesntAffectBuildStatus");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.SUCCESS, result.getResult());
    }

    /**
     * Testing: Escalation.WARNING
     * Build must be unstable if the value is exceeded.
     * <p>
     * Specified value: 1
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void warningModeMakesBuildUnstable(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // Value set to 1L to violate constraint. Due to Escalation.WARNING the build status must be UNSTABLE.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "testResult.xml", Escalation.WARNING, false, testCaseBlock, 1L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);


        PerformancePublisher performancePublisher = new PerformancePublisher("testResult.xml", 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);

        FreeStyleProject p = j.createFreeStyleProject("warningModeMakesBuildUnstable");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.UNSTABLE, result.getResult());
    }

    /**
     * Testing: Escalation.ERROR
     * Build must fail if the value is exceeded.
     * <p>
     * Specified value: 1
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void errorModeMakesBuildFail(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // Value set to 1L to violate constraint. Due to Escalation.ERROR the build status must be FAILURE.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "constraint-test.xml", Escalation.ERROR, false, testCaseBlock, 1L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);

        PerformancePublisher performancePublisher = new PerformancePublisher(getClass().getResource("/constraint-test.xml").getFile(), 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);

        FreeStyleProject p = j.createFreeStyleProject("errorModeMakesBuildFail");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, result.getResult());
    }

    /**
     * Testing: Operator.NOT_GREATER
     * Calculated value is equal to specified => Build.SUCCESS
     * <p>
     * Specified value: 56
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void equalValuesWithNotGreaterOperator(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // The specified value and calculated value are equal.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "testResult.xml", Escalation.ERROR, false, testCaseBlock, 56L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);

        PerformancePublisher performancePublisher = new PerformancePublisher("", 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);
        performancePublisher.setFailBuildIfNoResultFile(false);

        FreeStyleProject p = j.createFreeStyleProject("equalValuesWithNotGreaterOperator");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.SUCCESS, result.getResult());
    }

    /**
     * Testing: Operator.NOT_GREATER
     * Calculated value is greater than specified => Build.FAILURE
     * <p>
     * Specified value: 55
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void calculatedValueGreaterWithNotGreaterOperator(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // The specified should not be exceeded.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "constraint-test.xml", Escalation.ERROR, false, testCaseBlock, 55L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);

        PerformancePublisher performancePublisher = new PerformancePublisher(getClass().getResource("/constraint-test.xml").getFile(), 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);

        FreeStyleProject p = j.createFreeStyleProject("calculatedValueGreaterWithNotGreaterOperator");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, result.getResult());
    }

    /**
     * Testing: Operator.NOT_EQUAL
     * Calculated value is equal to specified => Build.FAILURE
     * <p>
     * Specified value: 56
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void equalValuesWithNotEqualOperator(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // The specified value and calculated value are equal.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_EQUAL, "constraint-test.xml", Escalation.ERROR, false, testCaseBlock, 56L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);

        PerformancePublisher performancePublisher = new PerformancePublisher(getClass().getResource("/constraint-test.xml").getFile(), 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);

        FreeStyleProject p = j.createFreeStyleProject("equalValuesWithNotEqualOperator");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, result.getResult());
    }

    /**
     * Testing: Operator.NOT_EQUAL
     * Calculated value is not equal than specified => Build.SUCCESS
     * <p>
     * Specified value: 55
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void notEqualValueWithNotEqualOperator(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // The specified should not be exceeded.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_EQUAL, "testResult.xml", Escalation.ERROR, false, testCaseBlock, 55L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);

        PerformancePublisher performancePublisher = new PerformancePublisher("", 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);
        performancePublisher.setFailBuildIfNoResultFile(false);

        FreeStyleProject p = j.createFreeStyleProject("notEqualValueWithNotEqualOperator");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.SUCCESS, result.getResult());
    }

    /**
     * Testing: Operator.NOT_LESS
     * Calculated value is equal to specified => Build.SUCCES
     * <p>
     * Specified value: 56
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void equalValuesWithNotLessOperator(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // The specified value and calculated value are equal.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_LESS, "testResult.xml", Escalation.ERROR, false, testCaseBlock, 56L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);

        PerformancePublisher performancePublisher = new PerformancePublisher("", 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);
        performancePublisher.setFailBuildIfNoResultFile(false);

        FreeStyleProject p = j.createFreeStyleProject("equalValuesWithNotLessOperator");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.SUCCESS, result.getResult());
    }

    /**
     * Testing: Operator.NOT_LESS
     * Calculated value is less than specified => Build.FAILURE
     * <p>
     * Specified value: 57
     * Calculated average value: 56
     *
     * @throws Exception if test encounters errors.
     */
    @Test
    void calculatedValueLessWithNotLessOperator(JenkinsRule j) throws Exception {
        TestCaseBlock testCaseBlock = new TestCaseBlock("listShows");

        // The specified should not be exceeded.
        AbsoluteConstraint absoluteConstraint = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_LESS, "constraint-test.xml", Escalation.ERROR, false, testCaseBlock, 57L);

        List<AbstractConstraint> abstractBuildsList = new ArrayList<AbstractConstraint>();
        abstractBuildsList.add(absoluteConstraint);

        PerformancePublisher performancePublisher = new PerformancePublisher(getClass().getResource("/constraint-test.xml").getFile(), 10, 20, "", 0, 0, 0, 0, 0, false, "", false, true, false, true,false, null);
        performancePublisher.setModeEvaluation(true);
        performancePublisher.setConstraints(abstractBuildsList);
        performancePublisher.setIgnoreFailedBuilds(false);
        performancePublisher.setIgnoreUnstableBuilds(false);
        performancePublisher.setPersistConstraintLog(false);

        FreeStyleProject p = j.createFreeStyleProject("calculatedValueLessWithNotLessOperator");
        p.getPublishersList().add(performancePublisher);
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build,
                                   Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace().child("testResult.xml").copyFrom(getClass().getResource("/constraint-test.xml"));
                return true;
            }
        });

        FreeStyleBuild result = p.scheduleBuild2(0).get();
        assertEquals(Result.FAILURE, result.getResult());
    }
}