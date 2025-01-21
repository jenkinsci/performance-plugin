package hudson.plugins.performance.actions;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.performance.AbstractGraphGenerationTest;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.util.RunList;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceProjectActionGraphTest extends AbstractGraphGenerationTest {

    private TestablePerformanceProjectAction target;

    @BeforeEach
    void actionSetup() throws Exception {
        when(project.getBuilds()).thenReturn(RunList.fromRuns(Collections.singletonList(build)));
        target = new TestablePerformanceProjectAction(project);
    }

    @Test
    void testRespondingTimeGraphPerTestCaseModeAverageValues() throws Exception {
        setGraphType(PerformancePublisher.ART);
        target.doRespondingTimeGraphPerTestCaseMode(request, response);
        assertArrayEquals(new Number[]{7930L, 354L}, toArray(target.dataset));
    }

    @Test
    void testRespondingTimeGraphPerTestCaseModeMedianValues() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doRespondingTimeGraphPerTestCaseMode(request, response);
        assertArrayEquals(new Number[]{598L, 63L}, toArray(target.dataset));
    }

    @Test
    void testSummarizerGraphAverageValues() throws Exception {
        setGraphType(PerformancePublisher.ART);
        target.doSummarizerGraph(request, response);
        assertArrayEquals(new Number[]{7930L, 354L}, toArray(target.dataset));
    }

    @Test
    void testSummarizerGraphMedianValues() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doSummarizerGraph(request, response);
        assertArrayEquals(new Number[]{598L, 63L}, toArray(target.dataset));
    }

    @Test
    void testErrorsGraph() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doErrorsGraph(request, response);
        assertArrayEquals(new Number[]{0.0}, toArray(target.dataset));
    }

    @Test
    void testRespondingTimeGraph() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doRespondingTimeGraph(request, response);
        assertArrayEquals(new Number[]{14720L, 14720L, 4142L, 501L}, toArray(target.dataset));
    }

    @Test
    void testThroughputGraph() throws Exception {
        target.doThroughputGraph(request, response);
        assertArrayEquals(new Number[]{0.04515946937623483}, toArray(target.dataset));
    }

    private class TestablePerformanceProjectAction extends PerformanceProjectAction {

        public CategoryDataset dataset;

        public TestablePerformanceProjectAction(AbstractProject<?, ?> project) {
            super(project);
        }

        @NonNull
        @Override
        public List<String> getPerformanceReportList() {
            return Collections.singletonList("JMeterResults.jtl");
        }

        @Override
        protected PerformanceReport getPerformanceReport(Run<?, ?> build, String reportFileName) {
            return report;
        }

        @Override
        protected JFreeChart createRespondingTimeChart(CategoryDataset dataset, int legendLimit) {
            this.dataset = dataset;
            return super.createRespondingTimeChart(dataset, legendLimit);
        }

        @Override
        protected JFreeChart createSummarizerChart(CategoryDataset dataset, String yAxis, String chartTitle) {
            this.dataset = dataset;
            return super.createSummarizerChart(dataset, yAxis, chartTitle);
        }

        @Override
        protected JFreeChart createErrorsGraph(CategoryDataset dataset) {
            this.dataset = dataset;
            return super.createErrorsGraph(dataset);
        }

        @Override
        protected JFreeChart createThroughputGraph(CategoryDataset dataset) {
            this.dataset = dataset;
            return super.createThroughputGraph(dataset);
        }
    }
}
