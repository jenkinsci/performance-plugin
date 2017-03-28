package hudson.plugins.performance.constraints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import hudson.plugins.performance.constraints.blocks.PreviousResultsBlock;
import hudson.plugins.performance.constraints.blocks.TestCaseBlock;
import hudson.plugins.performance.data.ConstraintSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.steadystate.css.parser.ParseException;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.PerformanceReportMap;
import hudson.plugins.performance.reports.UriReport;
import hudson.plugins.performance.constraints.AbstractConstraint.Escalation;
import hudson.plugins.performance.constraints.AbstractConstraint.Metric;
import hudson.plugins.performance.constraints.AbstractConstraint.Operator;
import jenkins.model.Jenkins;

@RunWith(MockitoJUnitRunner.class)
public class ConstraintCheckerTest {

    @InjectMocks
    ConstraintChecker constraintChecker = new ConstraintChecker(null, null);

    @InjectMocks
    ConstraintSettings constraintSettings = new ConstraintSettings(null, false, false, false);

    @Mock
    Jenkins jenkins;

    @Mock
    ConstraintChecker constraintChecker1;

    // @Mock
    List<AbstractBuild<?, ?>> abstractBuildsList = new ArrayList<AbstractBuild<?, ?>>();

    @Mock
    AbstractBuild<?, ?> abstractBuild0;
    // needed for check_evaluatePreviousBuilds
    @Mock
    AbstractBuild<?, ?> abstractBuild1;
    @Mock
    AbstractBuild<?, ?> abstractBuild2;
    @Mock
    AbstractBuild<?, ?> abstractBuild3;
    @Mock
    AbstractBuild<?, ?> abstractBuild4;
    @Mock
    AbstractBuild<?, ?> abstractBuild5;

    @Mock
    PerformanceBuildAction performanceBuildAction0;
    @Mock
    PerformanceBuildAction performanceBuildAction1;
    @Mock
    PerformanceBuildAction performanceBuildAction2;
    @Mock
    PerformanceBuildAction performanceBuildAction3;
    @Mock
    PerformanceBuildAction performanceBuildAction4;
    @Mock
    PerformanceBuildAction performanceBuildAction5;

    @Mock
    PerformanceReportMap performanceReportMap0;
    @Mock
    PerformanceReportMap performanceReportMap1;
    @Mock
    PerformanceReportMap performanceReportMap2;
    @Mock
    PerformanceReportMap performanceReportMap3;

    @Mock
    PerformanceReport performanceReport0_0;
    @Mock
    PerformanceReport performanceReport0_1;
    @Mock
    PerformanceReport performanceReport1_0;
    @Mock
    PerformanceReport performanceReport1_1;
    @Mock
    PerformanceReport performanceReport2_0;
    @Mock
    PerformanceReport performanceReport2_1;
    @Mock
    PerformanceReport performanceReport3_0;
    @Mock
    PerformanceReport performanceReport3_1;

    @Mock
    UriReport uriReport0_0_0;
    @Mock
    UriReport uriReport0_0_1;
    @Mock
    UriReport uriReport0_1_0;
    @Mock
    UriReport uriReport0_1_1;
    @Mock
    UriReport uriReport1_0_0;
    @Mock
    UriReport uriReport1_0_1;
    @Mock
    UriReport uriReport1_1_0;
    @Mock
    UriReport uriReport1_1_1;
    @Mock
    UriReport uriReport2_0_0;
    @Mock
    UriReport uriReport2_0_1;
    @Mock
    UriReport uriReport2_1_0;
    @Mock
    UriReport uriReport2_1_1;
    @Mock
    UriReport uriReport3_0_0;
    @Mock
    UriReport uriReport3_0_1;
    @Mock
    UriReport uriReport3_1_0;
    @Mock
    UriReport uriReport3_1_1;

    @Mock
    Iterator<AbstractBuild<?, ?>> buildIterator;

    @Mock
    BuildListener buildListener;

    @Mock
    PrintStream printStream;

    // Constraints
    TestCaseBlock ob0;
    TestCaseBlock ob1;
    TestCaseBlock ob2;
    AbsoluteConstraint ac0;
    AbsoluteConstraint ac1;
    AbsoluteConstraint ac2;
    AbsoluteConstraint ac3;
    AbsoluteConstraint ac4;
    AbsoluteConstraint ac5;
    PreviousResultsBlock rb0;
    PreviousResultsBlock rb1;
    PreviousResultsBlock rb2;
    PreviousResultsBlock rb3;
    PreviousResultsBlock rb4;
    PreviousResultsBlock rb5;
    RelativeConstraint rc0;
    RelativeConstraint rc1;
    RelativeConstraint rc2;
    RelativeConstraint rc3;
    RelativeConstraint rc4;
    RelativeConstraint rc5;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    /**
     * TestCase1 HappyPath - Testing every combination of constraints against some
     * builds
     *
     * @throws InterruptedException
     * @throws IOException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws ParseException
     * @throws java.text.ParseException
     */

    @Test
    public void happyPathForAbsoluteConstraints()
            throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, IOException, InterruptedException, ParseException, java.text.ParseException {

        List<AbstractConstraint> constraints = new ArrayList<AbstractConstraint>();
        constraints.add(ac0);
        constraints.add(ac1);
        constraints.add(ac2);
        constraints.add(ac3);
        constraints.add(ac4);
        constraints.add(ac5);

        ArrayList<ConstraintEvaluation> result = new ArrayList<ConstraintEvaluation>();
        result = constraintChecker.checkAllConstraints(constraints);

        assertEquals(6, result.size());
        assertEquals(ac0, result.get(0).getAbstractConstraint());
        assertEquals(ac1, result.get(1).getAbstractConstraint());
        assertEquals(ac2, result.get(2).getAbstractConstraint());
        assertEquals(ac3, result.get(3).getAbstractConstraint());
        assertEquals(ac4, result.get(4).getAbstractConstraint());
        assertEquals(ac5, result.get(5).getAbstractConstraint());

        assertTrue(result.get(0).getAbstractConstraint().getSuccess());
        assertTrue(result.get(4).getAbstractConstraint().getSuccess());
        assertFalse(result.get(5).getAbstractConstraint().getSuccess());

        assertEquals(100, result.get(0).getConstraintValue(), 0);
        assertEquals(10, result.get(3).getMeasuredValue(), 0);
    }

    @Test
    public void happyPathForRelativeConstraints()
            throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, IOException, InterruptedException, ParseException, java.text.ParseException {

        List<AbstractConstraint> constraints = new ArrayList<AbstractConstraint>();
        constraints.add(rc0);
        constraints.add(rc1);
        constraints.add(rc2);
        constraints.add(rc3);
        constraints.add(rc4);
        constraints.add(rc5);

        ArrayList<ConstraintEvaluation> result = new ArrayList<ConstraintEvaluation>();
        result = constraintChecker.checkAllConstraints(constraints);

        assertEquals(6, result.size());
        assertEquals(rc0, result.get(0).getAbstractConstraint());
        assertEquals(rc1, result.get(1).getAbstractConstraint());
        assertEquals(rc2, result.get(2).getAbstractConstraint());
        assertEquals(rc3, result.get(3).getAbstractConstraint());
        assertEquals(rc4, result.get(4).getAbstractConstraint());
        assertEquals(rc5, result.get(5).getAbstractConstraint());

        assertTrue(result.get(0).getAbstractConstraint().getSuccess());
        assertTrue(result.get(4).getAbstractConstraint().getSuccess());
        assertTrue(result.get(5).getAbstractConstraint().getSuccess());

        assertEquals(11, result.get(0).getConstraintValue(), 0);
        assertEquals(10, result.get(3).getMeasuredValue(), 0);
    }

    @Test
    public void happyPathForMixedConstraints()
            throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException, IOException, InterruptedException, ParseException, java.text.ParseException {

        List<AbstractConstraint> constraints = new ArrayList<AbstractConstraint>();
        constraints.add(rc0);
        constraints.add(rc1);
        constraints.add(rc2);
        constraints.add(ac3);
        constraints.add(ac4);
        constraints.add(ac5);

        ArrayList<ConstraintEvaluation> result = new ArrayList<ConstraintEvaluation>();
        result = constraintChecker.checkAllConstraints(constraints);

        assertEquals(6, result.size());
        assertEquals(rc0, result.get(0).getAbstractConstraint());
        assertEquals(rc1, result.get(1).getAbstractConstraint());
        assertEquals(rc2, result.get(2).getAbstractConstraint());
        assertEquals(ac3, result.get(3).getAbstractConstraint());
        assertEquals(ac4, result.get(4).getAbstractConstraint());
        assertEquals(ac5, result.get(5).getAbstractConstraint());

        assertTrue(result.get(0).getAbstractConstraint().getSuccess());
        assertTrue(result.get(2).getAbstractConstraint().getSuccess());
        assertTrue(result.get(4).getAbstractConstraint().getSuccess());
        assertFalse(result.get(5).getAbstractConstraint().getSuccess());

        assertEquals(11, result.get(2).getConstraintValue(), 0);
        assertEquals(10, result.get(2).getMeasuredValue(), 0);

        assertEquals(100, result.get(5).getConstraintValue(), 0);
        assertEquals(10, result.get(5).getMeasuredValue(), 0);
    }

    @Before
    public void setUp() {
        /**
         * Mock behaviour of the builds
         */
        constraintSettings = new ConstraintSettings(buildListener, true, true, true);
        when(this.buildListener.getLogger()).thenReturn(printStream);
        constraintChecker = new ConstraintChecker(constraintSettings, abstractBuildsList);

        ob0 = new TestCaseBlock("testUri0");
        ob1 = new TestCaseBlock("testUri1");
        ob2 = new TestCaseBlock(null);

        ac0 = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "testResult0.xml", Escalation.INFORMATION, false,
                ob0, 100L);
        ac1 = new AbsoluteConstraint(Metric.ERRORPRC, Operator.NOT_GREATER, "testResult0.xml", Escalation.INFORMATION,
                false, ob1, 100L);
        ac2 = new AbsoluteConstraint(Metric.LINE90, Operator.NOT_GREATER, "testResult0.xml", Escalation.WARNING, false, ob0,
                100L);
        ac3 = new AbsoluteConstraint(Metric.MAXIMUM, Operator.NOT_GREATER, "testResult1.xml", Escalation.WARNING, false,
                ob1, 100L);
        ac4 = new AbsoluteConstraint(Metric.MEDIAN, Operator.NOT_EQUAL, "testResult1.xml", Escalation.ERROR, false, ob2,
                100L);
        ac4.setSpecifiedTestCase(false);
        ac5 = new AbsoluteConstraint(Metric.MINIMUM, Operator.NOT_LESS, "testResult1.xml", Escalation.ERROR, false, ob2,
                100L);
        ac5.setSpecifiedTestCase(false);

        rb0 = new PreviousResultsBlock("true", "3", "", "");
        rb1 = new PreviousResultsBlock("true", "*", "", "");
        rb2 = new PreviousResultsBlock("false", "", "2015-01-01 02:00", "2015-02-01 23:00");
        rb3 = new PreviousResultsBlock("false", "", "2015-01-01", "2015-02-01");
        rb4 = new PreviousResultsBlock("false", "", "2015-01-01 12:00", "2015-02-01");
        rb5 = new PreviousResultsBlock("false", "", "2015-01-01", "now");

        rc0 = new RelativeConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "testResult0.xml", Escalation.INFORMATION, false,
                ob0, rb0, 10);
        rc1 = new RelativeConstraint(Metric.ERRORPRC, Operator.NOT_GREATER, "testResult0.xml", Escalation.INFORMATION,
                false, ob1, rb1, 10);
        rc2 = new RelativeConstraint(Metric.LINE90, Operator.NOT_GREATER, "testResult0.xml", Escalation.WARNING, false, ob0,
                rb2, 10);
        rc3 = new RelativeConstraint(Metric.MAXIMUM, Operator.NOT_LESS, "testResult1.xml", Escalation.WARNING, false, ob1,
                rb3, 10);
        rc4 = new RelativeConstraint(Metric.MEDIAN, Operator.NOT_LESS, "testResult1.xml", Escalation.ERROR, false, ob2, rb4,
                10);
        rc4.setSpecifiedTestCase(false);
        rc5 = new RelativeConstraint(Metric.MINIMUM, Operator.NOT_LESS, "testResult1.xml", Escalation.ERROR, false, ob2,
                rb5, 10);
        rc5.setSpecifiedTestCase(false);

        abstractBuildsList.add(abstractBuild0);
        abstractBuildsList.add(abstractBuild1);
        abstractBuildsList.add(abstractBuild2);
        abstractBuildsList.add(abstractBuild3);

        List<PerformanceReport> performanceReportList0 = new ArrayList<PerformanceReport>();
        performanceReportList0.add(performanceReport0_0);
        performanceReportList0.add(performanceReport0_1);

        List<PerformanceReport> performanceReportList1 = new ArrayList<PerformanceReport>();
        performanceReportList1.add(performanceReport1_0);
        performanceReportList1.add(performanceReport1_1);

        List<PerformanceReport> performanceReportList2 = new ArrayList<PerformanceReport>();
        performanceReportList2.add(performanceReport2_0);
        performanceReportList2.add(performanceReport2_1);

        List<PerformanceReport> performanceReportList3 = new ArrayList<PerformanceReport>();
        performanceReportList3.add(performanceReport3_0);
        performanceReportList3.add(performanceReport3_1);

        when(this.abstractBuild0.getUrl()).thenReturn("abstractBuild0testUrl");
        when(this.abstractBuild1.getUrl()).thenReturn("abstractBuild1testUrl");
        when(this.abstractBuild2.getUrl()).thenReturn("abstractBuild2testUrl");
        when(this.abstractBuild3.getUrl()).thenReturn("abstractBuild3testUrl");

        when(this.abstractBuild0.getResult()).thenReturn(Result.SUCCESS);
        when(this.abstractBuild1.getResult()).thenReturn(Result.SUCCESS);
        when(this.abstractBuild2.getResult()).thenReturn(Result.SUCCESS);
        when(this.abstractBuild3.getResult()).thenReturn(Result.SUCCESS);

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        Calendar calendar1 = new GregorianCalendar(2015, 0, 1, 12, 0, 0);
        Calendar calendar2 = new GregorianCalendar(2015, 0, 15, 12, 0, 0);
        Calendar calendar3 = new GregorianCalendar(2015, 1, 1, 12, 0, 0);

        when(this.abstractBuild0.getTimestamp()).thenReturn(calendar);
        when(this.abstractBuild1.getTimestamp()).thenReturn(calendar1);
        when(this.abstractBuild2.getTimestamp()).thenReturn(calendar2);
        when(this.abstractBuild3.getTimestamp()).thenReturn(calendar3);

        when(this.abstractBuild0.getAction(PerformanceBuildAction.class)).thenReturn(performanceBuildAction0);
        when(this.abstractBuild1.getAction(PerformanceBuildAction.class)).thenReturn(performanceBuildAction1);
        when(this.abstractBuild2.getAction(PerformanceBuildAction.class)).thenReturn(performanceBuildAction2);
        when(this.abstractBuild3.getAction(PerformanceBuildAction.class)).thenReturn(performanceBuildAction3);

        when(this.performanceBuildAction0.getPerformanceReportMap()).thenReturn(performanceReportMap0);
        when(this.performanceBuildAction1.getPerformanceReportMap()).thenReturn(performanceReportMap1);
        when(this.performanceBuildAction2.getPerformanceReportMap()).thenReturn(performanceReportMap2);
        when(this.performanceBuildAction3.getPerformanceReportMap()).thenReturn(performanceReportMap3);

        when(this.performanceReportMap0.getPerformanceListOrdered()).thenReturn(performanceReportList0);
        when(this.performanceReportMap1.getPerformanceListOrdered()).thenReturn(performanceReportList1);
        when(this.performanceReportMap2.getPerformanceListOrdered()).thenReturn(performanceReportList2);
        when(this.performanceReportMap3.getPerformanceListOrdered()).thenReturn(performanceReportList3);

        when(this.performanceReportMap0.getPerformanceReport("testResult0.xml")).thenReturn(performanceReport0_0);
        when(this.performanceReportMap0.getPerformanceReport("testResult1.xml")).thenReturn(performanceReport0_1);

        when(this.performanceReportMap1.getPerformanceReport("testResult0.xml")).thenReturn(performanceReport1_0);
        when(this.performanceReportMap1.getPerformanceReport("testResult1.xml")).thenReturn(performanceReport1_1);

        when(this.performanceReportMap2.getPerformanceReport("testResult0.xml")).thenReturn(performanceReport2_0);
        when(this.performanceReportMap2.getPerformanceReport("testResult1.xml")).thenReturn(performanceReport2_1);

        when(this.performanceReportMap3.getPerformanceReport("testResult0.xml")).thenReturn(performanceReport3_0);
        when(this.performanceReportMap3.getPerformanceReport("testResult1.xml")).thenReturn(performanceReport3_1);

        when(this.performanceReport0_0.getReportFileName()).thenReturn("testResult0.xml");
        when(this.performanceReport0_1.getReportFileName()).thenReturn("testResult1.xml");

        when(this.performanceReport1_0.getReportFileName()).thenReturn("testResult0.xml");
        when(this.performanceReport1_1.getReportFileName()).thenReturn("testResult1.xml");

        when(this.performanceReport2_0.getReportFileName()).thenReturn("testResult0.xml");
        when(this.performanceReport2_1.getReportFileName()).thenReturn("testResult1.xml");

        when(this.performanceReport3_0.getReportFileName()).thenReturn("testResult0.xml");
        when(this.performanceReport3_1.getReportFileName()).thenReturn("testResult1.xml");

        List<UriReport> uriReportList0_0 = new ArrayList<UriReport>();
        uriReportList0_0.add(uriReport0_0_0);
        uriReportList0_0.add(uriReport0_0_1);

        List<UriReport> uriReportList0_1 = new ArrayList<UriReport>();
        uriReportList0_1.add(uriReport0_1_0);
        uriReportList0_1.add(uriReport0_1_1);

        List<UriReport> uriReportList1_0 = new ArrayList<UriReport>();
        uriReportList1_0.add(uriReport1_0_0);
        uriReportList1_0.add(uriReport1_0_1);

        List<UriReport> uriReportList1_1 = new ArrayList<UriReport>();
        uriReportList1_1.add(uriReport1_1_0);
        uriReportList1_1.add(uriReport1_1_1);

        List<UriReport> uriReportList2_0 = new ArrayList<UriReport>();
        uriReportList2_0.add(uriReport2_0_0);
        uriReportList2_0.add(uriReport2_0_1);

        List<UriReport> uriReportList2_1 = new ArrayList<UriReport>();
        uriReportList2_1.add(uriReport2_1_0);
        uriReportList2_1.add(uriReport2_1_1);

        List<UriReport> uriReportList3_0 = new ArrayList<UriReport>();
        uriReportList3_0.add(uriReport3_0_0);
        uriReportList3_0.add(uriReport3_0_1);

        List<UriReport> uriReportList3_1 = new ArrayList<UriReport>();
        uriReportList3_1.add(uriReport3_1_0);
        uriReportList3_1.add(uriReport3_1_1);

        when(this.performanceReport0_0.getUriListOrdered()).thenReturn(uriReportList0_0);
        when(this.performanceReport0_1.getUriListOrdered()).thenReturn(uriReportList0_1);

        when(this.performanceReport1_0.getUriListOrdered()).thenReturn(uriReportList1_0);
        when(this.performanceReport1_1.getUriListOrdered()).thenReturn(uriReportList1_1);

        when(this.performanceReport2_0.getUriListOrdered()).thenReturn(uriReportList2_0);
        when(this.performanceReport2_1.getUriListOrdered()).thenReturn(uriReportList2_1);

        when(this.performanceReport3_0.getUriListOrdered()).thenReturn(uriReportList3_0);
        when(this.performanceReport3_1.getUriListOrdered()).thenReturn(uriReportList3_1);

        when(uriReport0_0_0.getUri()).thenReturn("testUri0");
        when(uriReport0_0_0.getAverage()).thenReturn((long) 10);
        when(uriReport0_0_0.errorPercent()).thenReturn((double) 10);
        when(uriReport0_0_0.get90Line()).thenReturn((long) 10);
        when(uriReport0_0_0.getMax()).thenReturn((long) 10);
        when(uriReport0_0_0.getMedian()).thenReturn((long) 10);
        when(uriReport0_0_0.getMin()).thenReturn((long) 10);

        when(uriReport0_0_1.getUri()).thenReturn("testUri1");
        when(uriReport0_0_1.getAverage()).thenReturn((long) 10);
        when(uriReport0_0_1.errorPercent()).thenReturn((double) 10);
        when(uriReport0_0_1.get90Line()).thenReturn((long) 10);
        when(uriReport0_0_1.getMax()).thenReturn((long) 10);
        when(uriReport0_0_1.getMedian()).thenReturn((long) 10);
        when(uriReport0_0_1.getMin()).thenReturn((long) 10);

        when(uriReport0_1_0.getUri()).thenReturn("testUri0");
        when(uriReport0_1_0.getAverage()).thenReturn((long) 10);
        when(uriReport0_1_0.errorPercent()).thenReturn((double) 10);
        when(uriReport0_1_0.get90Line()).thenReturn((long) 10);
        when(uriReport0_1_0.getMax()).thenReturn((long) 10);
        when(uriReport0_1_0.getMedian()).thenReturn((long) 10);
        when(uriReport0_1_0.getMin()).thenReturn((long) 10);

        when(uriReport0_1_1.getUri()).thenReturn("testUri1");
        when(uriReport0_1_1.getAverage()).thenReturn((long) 10);
        when(uriReport0_1_1.errorPercent()).thenReturn((double) 10);
        when(uriReport0_1_1.get90Line()).thenReturn((long) 10);
        when(uriReport0_1_1.getMax()).thenReturn((long) 10);
        when(uriReport0_1_1.getMedian()).thenReturn((long) 10);
        when(uriReport0_1_1.getMin()).thenReturn((long) 10);

        when(uriReport1_0_0.getUri()).thenReturn("testUri0");
        when(uriReport1_0_0.getAverage()).thenReturn((long) 10);
        when(uriReport1_0_0.errorPercent()).thenReturn((double) 10);
        when(uriReport1_0_0.get90Line()).thenReturn((long) 10);
        when(uriReport1_0_0.getMax()).thenReturn((long) 10);
        when(uriReport1_0_0.getMedian()).thenReturn((long) 10);
        when(uriReport1_0_0.getMin()).thenReturn((long) 10);

        when(uriReport1_0_1.getUri()).thenReturn("testUri1");
        when(uriReport1_0_1.getAverage()).thenReturn((long) 10);
        when(uriReport1_0_1.errorPercent()).thenReturn((double) 10);
        when(uriReport1_0_1.get90Line()).thenReturn((long) 10);
        when(uriReport1_0_1.getMax()).thenReturn((long) 10);
        when(uriReport1_0_1.getMedian()).thenReturn((long) 10);
        when(uriReport1_0_1.getMin()).thenReturn((long) 10);

        when(uriReport1_1_0.getUri()).thenReturn("testUri0");
        when(uriReport1_1_0.getAverage()).thenReturn((long) 10);
        when(uriReport1_1_0.errorPercent()).thenReturn((double) 10);
        when(uriReport1_1_0.get90Line()).thenReturn((long) 10);
        when(uriReport1_1_0.getMax()).thenReturn((long) 10);
        when(uriReport1_1_0.getMedian()).thenReturn((long) 10);
        when(uriReport1_1_0.getMin()).thenReturn((long) 10);

        when(uriReport1_1_1.getUri()).thenReturn("testUri1");
        when(uriReport1_1_1.getAverage()).thenReturn((long) 10);
        when(uriReport1_1_1.errorPercent()).thenReturn((double) 10);
        when(uriReport1_1_1.get90Line()).thenReturn((long) 10);
        when(uriReport1_1_1.getMax()).thenReturn((long) 10);
        when(uriReport1_1_1.getMedian()).thenReturn((long) 10);
        when(uriReport1_1_1.getMin()).thenReturn((long) 10);

        when(uriReport2_0_0.getUri()).thenReturn("testUri0");
        when(uriReport2_0_0.getAverage()).thenReturn((long) 10);
        when(uriReport2_0_0.errorPercent()).thenReturn((double) 10);
        when(uriReport2_0_0.get90Line()).thenReturn((long) 10);
        when(uriReport2_0_0.getMax()).thenReturn((long) 10);
        when(uriReport2_0_0.getMedian()).thenReturn((long) 10);
        when(uriReport2_0_0.getMin()).thenReturn((long) 10);

        when(uriReport2_0_1.getUri()).thenReturn("testUri1");
        when(uriReport2_0_1.getAverage()).thenReturn((long) 10);
        when(uriReport2_0_1.errorPercent()).thenReturn((double) 10);
        when(uriReport2_0_1.get90Line()).thenReturn((long) 10);
        when(uriReport2_0_1.getMax()).thenReturn((long) 10);
        when(uriReport2_0_1.getMedian()).thenReturn((long) 10);
        when(uriReport2_0_1.getMin()).thenReturn((long) 10);

        when(uriReport2_1_0.getUri()).thenReturn("testUri0");
        when(uriReport2_1_0.getAverage()).thenReturn((long) 10);
        when(uriReport2_1_0.errorPercent()).thenReturn((double) 10);
        when(uriReport2_1_0.get90Line()).thenReturn((long) 10);
        when(uriReport2_1_0.getMax()).thenReturn((long) 10);
        when(uriReport2_1_0.getMedian()).thenReturn((long) 10);
        when(uriReport2_1_0.getMin()).thenReturn((long) 10);

        when(uriReport2_1_1.getUri()).thenReturn("testUri1");
        when(uriReport2_1_1.getAverage()).thenReturn((long) 10);
        when(uriReport2_1_1.errorPercent()).thenReturn((double) 10);
        when(uriReport2_1_1.get90Line()).thenReturn((long) 10);
        when(uriReport2_1_1.getMax()).thenReturn((long) 10);
        when(uriReport2_1_1.getMedian()).thenReturn((long) 10);
        when(uriReport2_1_1.getMin()).thenReturn((long) 10);

        when(uriReport3_0_0.getUri()).thenReturn("testUri0");
        when(uriReport3_0_0.getAverage()).thenReturn((long) 10);
        when(uriReport3_0_0.errorPercent()).thenReturn((double) 10);
        when(uriReport3_0_0.get90Line()).thenReturn((long) 10);
        when(uriReport3_0_0.getMax()).thenReturn((long) 10);
        when(uriReport3_0_0.getMedian()).thenReturn((long) 10);
        when(uriReport3_0_0.getMin()).thenReturn((long) 10);

        when(uriReport3_0_1.getUri()).thenReturn("testUri1");
        when(uriReport3_0_1.getAverage()).thenReturn((long) 10);
        when(uriReport3_0_1.errorPercent()).thenReturn((double) 10);
        when(uriReport3_0_1.get90Line()).thenReturn((long) 10);
        when(uriReport3_0_1.getMax()).thenReturn((long) 10);
        when(uriReport3_0_1.getMedian()).thenReturn((long) 10);
        when(uriReport3_0_1.getMin()).thenReturn((long) 10);

        when(uriReport3_1_0.getUri()).thenReturn("testUri0");
        when(uriReport3_1_0.getAverage()).thenReturn((long) 10);
        when(uriReport3_1_0.errorPercent()).thenReturn((double) 10);
        when(uriReport3_1_0.get90Line()).thenReturn((long) 10);
        when(uriReport3_1_0.getMax()).thenReturn((long) 10);
        when(uriReport3_1_0.getMedian()).thenReturn((long) 10);
        when(uriReport3_1_0.getMin()).thenReturn((long) 10);

        when(uriReport3_1_1.getUri()).thenReturn("testUri1");
        when(uriReport3_1_1.getAverage()).thenReturn((long) 10);
        when(uriReport3_1_1.errorPercent()).thenReturn((double) 10);
        when(uriReport3_1_1.get90Line()).thenReturn((long) 10);
        when(uriReport3_1_1.getMax()).thenReturn((long) 10);
        when(uriReport3_1_1.getMedian()).thenReturn((long) 10);
        when(uriReport3_1_1.getMin()).thenReturn((long) 10);

    /*
     * Mocking behaviour of performance Reports
     */
        when(performanceReport0_0.getAverage()).thenReturn((long) 10);
        when(performanceReport0_0.errorPercent()).thenReturn((double) 10);
        when(performanceReport0_0.get90Line()).thenReturn((long) 10);
        when(performanceReport0_0.getMax()).thenReturn((long) 10);
        when(performanceReport0_0.getMedian()).thenReturn((long) 10);
        when(performanceReport0_0.getMin()).thenReturn((long) 10);

        when(performanceReport0_1.getAverage()).thenReturn((long) 10);
        when(performanceReport0_1.errorPercent()).thenReturn((double) 10);
        when(performanceReport0_1.get90Line()).thenReturn((long) 10);
        when(performanceReport0_1.getMax()).thenReturn((long) 10);
        when(performanceReport0_1.getMedian()).thenReturn((long) 10);
        when(performanceReport0_1.getMin()).thenReturn((long) 10);

        when(performanceReport1_0.getAverage()).thenReturn((long) 10);
        when(performanceReport1_0.errorPercent()).thenReturn((double) 10);
        when(performanceReport1_0.get90Line()).thenReturn((long) 10);
        when(performanceReport1_0.getMax()).thenReturn((long) 10);
        when(performanceReport1_0.getMedian()).thenReturn((long) 10);
        when(performanceReport1_0.getMin()).thenReturn((long) 10);

        when(performanceReport1_1.getAverage()).thenReturn((long) 10);
        when(performanceReport1_1.errorPercent()).thenReturn((double) 10);
        when(performanceReport1_1.get90Line()).thenReturn((long) 10);
        when(performanceReport1_1.getMax()).thenReturn((long) 10);
        when(performanceReport1_1.getMedian()).thenReturn((long) 10);
        when(performanceReport1_1.getMin()).thenReturn((long) 10);

        when(performanceReport2_0.getAverage()).thenReturn((long) 10);
        when(performanceReport2_0.errorPercent()).thenReturn((double) 10);
        when(performanceReport2_0.get90Line()).thenReturn((long) 10);
        when(performanceReport2_0.getMax()).thenReturn((long) 10);
        when(performanceReport2_0.getMedian()).thenReturn((long) 10);
        when(performanceReport2_0.getMin()).thenReturn((long) 10);

        when(performanceReport2_1.getAverage()).thenReturn((long) 10);
        when(performanceReport2_1.errorPercent()).thenReturn((double) 10);
        when(performanceReport2_1.get90Line()).thenReturn((long) 10);
        when(performanceReport2_1.getMax()).thenReturn((long) 10);
        when(performanceReport2_1.getMedian()).thenReturn((long) 10);
        when(performanceReport2_1.getMin()).thenReturn((long) 10);

        when(performanceReport3_0.getAverage()).thenReturn((long) 10);
        when(performanceReport3_0.errorPercent()).thenReturn((double) 10);
        when(performanceReport3_0.get90Line()).thenReturn((long) 10);
        when(performanceReport3_0.getMax()).thenReturn((long) 10);
        when(performanceReport3_0.getMedian()).thenReturn((long) 10);
        when(performanceReport3_0.getMin()).thenReturn((long) 10);

        when(performanceReport3_1.getAverage()).thenReturn((long) 10);
        when(performanceReport3_1.errorPercent()).thenReturn((double) 10);
        when(performanceReport3_1.get90Line()).thenReturn((long) 10);
        when(performanceReport3_1.getMax()).thenReturn((long) 10);
        when(performanceReport3_1.getMedian()).thenReturn((long) 10);
        when(performanceReport3_1.getMin()).thenReturn((long) 10);
    }

    @Test
    public void test() throws Exception {
        ConstraintChecker checker = new ConstraintChecker(null, null);
        checker.setSettings(constraintSettings);
        assertEquals(constraintSettings, checker.getSettings());

        List<AbstractBuild<?, ?>> list = new ArrayList<AbstractBuild<?, ?>>();
        list.add(abstractBuild0);
        checker.setBuilds(list);

        assertEquals(list, checker.getBuilds());
    }
}
