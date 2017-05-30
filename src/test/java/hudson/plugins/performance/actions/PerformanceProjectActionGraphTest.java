package hudson.plugins.performance.actions;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.performance.AbstractGraphGenerationTest;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.util.RunList;

@RunWith(MockitoJUnitRunner.class)
public class PerformanceProjectActionGraphTest extends AbstractGraphGenerationTest {

    private TestablePerformanceProjectAction target;

    @Before
    public void actionSetup() throws Exception {
        when(project.getBuilds()).thenReturn(RunList.fromRuns(Collections.singletonList(build)));
        target = new TestablePerformanceProjectAction(project);
    }

    @Test
    public void testRespondingTimeGraphPerTestCaseModeAverageValues() throws Exception {
        setGraphType(PerformancePublisher.ART);
        target.doRespondingTimeGraphPerTestCaseMode(request, response);
        assertArrayEquals(new Number[]{7930L, 354L}, toArray(target.dataset));
    }

    @Test
    public void testRespondingTimeGraphPerTestCaseModeMedianValues() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doRespondingTimeGraphPerTestCaseMode(request, response);
        assertArrayEquals(new Number[]{14720L, 278L}, toArray(target.dataset));
    }

    @Test
    public void testSummarizerGraphAverageValues() throws Exception {
        setGraphType(PerformancePublisher.ART);
        target.doSummarizerGraph(request, response);
        assertArrayEquals(new Number[]{7930L, 354L}, toArray(target.dataset));
    }

    @Test
    public void testSummarizerGraphMedianValues() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doSummarizerGraph(request, response);
        assertArrayEquals(new Number[]{14720L, 278L}, toArray(target.dataset));
    }

    private class TestablePerformanceProjectAction extends PerformanceProjectAction {

        public CategoryDataset dataset;

        public TestablePerformanceProjectAction(AbstractProject<?, ?> project) {
            super(project);
        }

        @Nonnull
        @Override
        public List<String> getPerformanceReportList() {
            return Collections.singletonList("JMeterResults.jtl");
        }

        @Override
        protected PerformanceReport getPerformanceReport(Run<?, ?> build, String reportFileName) {
            return report;
        }

        @Override
        protected JFreeChart createRespondingTimeChart(CategoryDataset dataset) {
            this.dataset = dataset;
            return super.createRespondingTimeChart(dataset);
        }

        @Override
        protected JFreeChart createSummarizerChart(CategoryDataset dataset, String yAxis, String chartTitle) {
            this.dataset = dataset;
            return super.createSummarizerChart(dataset, yAxis, chartTitle);
        }
    }
}
