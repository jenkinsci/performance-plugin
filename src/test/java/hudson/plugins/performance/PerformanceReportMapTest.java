package hudson.plugins.performance;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.util.DescribableList;
import hudson.util.RunList;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class PerformanceReportMapTest extends AbstractGraphGenerationTest {

    private TestablePerformanceReportMap target;

    @Mock(strictness = Mock.Strictness.LENIENT)
    protected PerformancePublisher performancePublisher;

    @BeforeEach
    void reportMapSetup() throws Exception {
        when(build.getParent()).thenReturn(project);
        when(request.getParameter("performanceReportPosition")).thenReturn("JMeterResults.jtl");
        when(project.getBuilds()).thenReturn(RunList.fromRuns(Collections.singletonList(build)));

        DescribableList publisherList = mock(DescribableList.class, withSettings().strictness(Strictness.LENIENT));
        when(publisherList.get(PerformancePublisher.class)).thenReturn(performancePublisher);
        when(performancePublisher.isModeThroughput()).thenReturn(true);
        when(performancePublisher.isModePerformancePerTestCase()).thenReturn(false);
        when(project.getPublishersList()).thenReturn(publisherList);
        target = new TestablePerformanceReportMap(performanceBuildAction, mock(TaskListener.class));
    }

    @Test
    void testGetters() throws Exception {
        PerformanceReportMap reportMap = new PerformanceReportMap(performanceBuildAction, mock(TaskListener.class));
        assertTrue(reportMap.ifModeThroughputUsed());
        assertFalse(reportMap.ifModePerformancePerTestCaseUsed());
        assertFalse(reportMap.ifShowTrendGraphsUsed());
        assertEquals(performanceBuildAction, reportMap.getBuildAction());
        assertEquals("Performance", reportMap.getDisplayName());
        assertEquals("performance", reportMap.getUrlName());
    }

    @Test
    void testRespondingTimeGraphAverageValues() throws Exception {
        setGraphType(PerformancePublisher.ART);
        target.doRespondingTimeGraph(request, response);
        assertArrayEquals(new Number[]{4142L}, toArray(target.dataset));
    }

    @Test
    void testRespondingTimeGraphMedianValues() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doRespondingTimeGraph(request, response);
        assertArrayEquals(new Number[]{501L}, toArray(target.dataset));
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
    void testThroughputGraph() throws Exception {
        target.doThroughputGraph(request, response);
        assertArrayEquals(new Number[]{0.04515946937623483}, toArray(target.dataset));
    }

    @Test
    void testRespondingTimeGraphPerTestCaseMode() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doRespondingTimeGraphPerTestCaseMode(request, response);
        assertArrayEquals(new Number[]{598L, 63L}, toArray(target.dataset));
    }

    @Test
    void testErrorsGraph() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doErrorsGraph(request, response);
        assertArrayEquals(new Number[]{0.0}, toArray(target.dataset));
    }

    public class TestablePerformanceReportMap extends PerformanceReportMap {

        public CategoryDataset dataset;

        public TestablePerformanceReportMap(PerformanceBuildAction buildAction, TaskListener listener) throws IOException {
            super(buildAction, listener);
        }

        @Override
        protected JFreeChart createRespondingTimeChart(CategoryDataset dataset, int legendLimit) {
            this.dataset = dataset;
            return super.createRespondingTimeChart(dataset, legendLimit);
        }

        @Override
        protected JFreeChart createSummarizerChart(CategoryDataset dataset) {
            this.dataset = dataset;
            return super.createSummarizerChart(dataset);
        }

        @Override
        protected void parseReports(Run<?, ?> build, TaskListener listener, PerformanceReportCollector collector, String filename) {
            collector.addAll(Collections.singletonList(report));
        }

        @Override
        protected JFreeChart createThroughputChart(CategoryDataset dataset) {
            this.dataset = dataset;
            return super.createThroughputChart(dataset);
        }

        @Override
        protected JFreeChart createErrorsChart(CategoryDataset dataset) {
            this.dataset = dataset;
            return super.createErrorsChart(dataset);
        }
    }
}
