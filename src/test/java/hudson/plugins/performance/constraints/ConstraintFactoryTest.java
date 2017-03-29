package hudson.plugins.performance.constraints;


import static org.hamcrest.CoreMatchers.is;

import java.util.ArrayList;
import java.util.List;

import hudson.plugins.performance.constraints.blocks.PreviousResultsBlock;
import hudson.plugins.performance.constraints.blocks.TestCaseBlock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.model.AbstractBuild;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.PerformanceReportMap;
import hudson.plugins.performance.reports.UriReport;
import hudson.plugins.performance.constraints.AbstractConstraint.Escalation;
import hudson.plugins.performance.constraints.AbstractConstraint.Metric;
import hudson.plugins.performance.constraints.AbstractConstraint.Operator;


@RunWith(MockitoJUnitRunner.class)
public class ConstraintFactoryTest {

    private List<AbstractConstraint> constraints1;
    private List<AbstractConstraint> constraints2;

    @InjectMocks
    ConstraintFactory constraintFactory;

    @Mock
    AbstractBuild<?, ?> abstractBuild;

    @Mock
    PerformanceBuildAction performanceBuildAction;

    @Mock
    PerformanceReportMap performanceReportMap;

    @Mock
    PerformanceReport performanceReport;


    @Before
    public void setUp() {
        /**
         * Creating constraints
         */
        TestCaseBlock ob1 = new TestCaseBlock("tc1");
        TestCaseBlock ob2 = new TestCaseBlock("*");
        TestCaseBlock ob3 = new TestCaseBlock("tc1, tc2, tc3");
        PreviousResultsBlock rb = new PreviousResultsBlock("true", "3", "", "");

        AbsoluteConstraint ac1 = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "Result.xml", Escalation.INFORMATION, true, ob1, 100L);
        AbsoluteConstraint ac2 = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "Result.xml", Escalation.INFORMATION, true, ob2, 100L);
        AbsoluteConstraint ac3 = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "Result.xml", Escalation.INFORMATION, true, ob3, 100L);
        AbsoluteConstraint ac4 = new AbsoluteConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "Result.xml", Escalation.INFORMATION, true, null, 100L);
        ac4.setSpecifiedTestCase(false);

        RelativeConstraint rc1 = new RelativeConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "Result.xml", Escalation.INFORMATION, true, ob1, rb, 10);
        RelativeConstraint rc2 = new RelativeConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "Result.xml", Escalation.INFORMATION, true, ob2, rb, 10);
        RelativeConstraint rc3 = new RelativeConstraint(Metric.AVERAGE, Operator.NOT_GREATER, "Result.xml", Escalation.INFORMATION, true, ob3, rb, 10);
        /**
         * Creating UriReports
         */
        List<UriReport> list = new ArrayList<UriReport>();
        UriReport ur1 = new UriReport(null, "tc1", "tc1");
        UriReport ur2 = new UriReport(null, "tc2", "tc2");
        UriReport ur3 = new UriReport(null, "tc3", "tc3");
        UriReport ur4 = new UriReport(null, "tc4", "tc4");
        list.add(ur1);
        list.add(ur2);
        list.add(ur3);
        list.add(ur4);
        /**
         * Mocking Performance Report
         */
        Mockito.when(this.abstractBuild.getAction(PerformanceBuildAction.class)).thenReturn(performanceBuildAction);
        Mockito.when(this.performanceBuildAction.getPerformanceReportMap()).thenReturn(performanceReportMap);
        Mockito.when(this.performanceReportMap.getPerformanceReport(Mockito.anyString())).thenReturn(performanceReport);
        Mockito.when(this.performanceReport.getUriListOrdered()).thenReturn(list);

        // List 1 fill
        constraints1 = new ArrayList<AbstractConstraint>();
        constraints1.add(ac1);
        constraints1.add(ac2);
        constraints1.add(ac3);
        constraints1.add(rc1);
        constraints1.add(rc2);
        constraints1.add(rc3);

        // List 2 fill
        constraints2 = new ArrayList<AbstractConstraint>();
        constraints2.add(ac1);
        constraints2.add(ac4);
    }

    @Test
    public void test_happyPath() {
        /**
         * Executing method
         */
        List<? extends AbstractConstraint> result = this.constraintFactory.createConstraintClones(abstractBuild, constraints1);
        /**
         * Check list samplesCount
         */
        Assert.assertThat(result.size(), is(16));
        /**
         * Check test cases
         */
        Assert.assertThat(result.get(0).getTestCaseBlock().getTestCase(), is("tc1"));
        Assert.assertThat(result.get(1).getTestCaseBlock().getTestCase(), is("tc1"));
        Assert.assertThat(result.get(2).getTestCaseBlock().getTestCase(), is("tc2"));
        Assert.assertThat(result.get(3).getTestCaseBlock().getTestCase(), is("tc3"));
        Assert.assertThat(result.get(4).getTestCaseBlock().getTestCase(), is("tc4"));
        Assert.assertThat(result.get(5).getTestCaseBlock().getTestCase(), is("tc1"));
        Assert.assertThat(result.get(6).getTestCaseBlock().getTestCase(), is("tc2"));
        Assert.assertThat(result.get(7).getTestCaseBlock().getTestCase(), is("tc3"));
        Assert.assertThat(result.get(8).getTestCaseBlock().getTestCase(), is("tc1"));
        Assert.assertThat(result.get(9).getTestCaseBlock().getTestCase(), is("tc1"));
        Assert.assertThat(result.get(10).getTestCaseBlock().getTestCase(), is("tc2"));
        Assert.assertThat(result.get(11).getTestCaseBlock().getTestCase(), is("tc3"));
        Assert.assertThat(result.get(12).getTestCaseBlock().getTestCase(), is("tc4"));
        Assert.assertThat(result.get(13).getTestCaseBlock().getTestCase(), is("tc1"));
        Assert.assertThat(result.get(14).getTestCaseBlock().getTestCase(), is("tc2"));
        Assert.assertThat(result.get(15).getTestCaseBlock().getTestCase(), is("tc3"));
    }

    @Test
    public void test_optionalBlock() {
        /**
         * Executing method
         */
        List<? extends AbstractConstraint> result = this.constraintFactory.createConstraintClones(abstractBuild, constraints2);
        /**
         * Checking optional blocks
         */
        Assert.assertThat(result.get(0).getTestCaseBlock().getTestCase(), is("tc1"));
        Assert.assertThat(result.get(1).isSpecifiedTestCase(), is(false));
        Assert.assertNull(result.get(1).getTestCaseBlock());
    }
}
