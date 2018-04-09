package hudson.plugins.performance;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import hudson.util.DescribableList;
import hudson.util.RunList;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.actions.PerformanceBuildAction;

@RunWith(MockitoJUnitRunner.class)
public class PerformanceReportMapTest extends AbstractGraphGenerationTest {

    private TestablePerformanceReportMap target;

    @Mock
    protected PerformancePublisher performancePublisher;

    @Before
    public void reportMapSetup() throws Exception {
        when(build.getParent()).thenReturn(project);
        when(request.getParameter("performanceReportPosition")).thenReturn("JMeterResults.jtl");
        when(project.getBuilds()).thenReturn(RunList.fromRuns(Collections.singletonList(build)));

        DescribableList publisherList = mock(DescribableList.class);
        when(publisherList.get(PerformancePublisher.class)).thenReturn(performancePublisher);
        when(performancePublisher.isModeThroughput()).thenReturn(true);
        when(performancePublisher.isModePerformancePerTestCase()).thenReturn(false);
        when(project.getPublishersList()).thenReturn(publisherList);
        target = new TestablePerformanceReportMap(performanceBuildAction, mock(TaskListener.class));
    }

    @Test
    public void testGetters() throws Exception {
        PerformanceReportMap reportMap = new PerformanceReportMap(performanceBuildAction, mock(TaskListener.class));
        assertTrue(reportMap.ifModeThroughputUsed());
        assertFalse(reportMap.ifModePerformancePerTestCaseUsed());
        assertEquals(performanceBuildAction, reportMap.getBuildAction());
        assertEquals("Performance", reportMap.getDisplayName());
        assertEquals("performance", reportMap.getUrlName());
    }

    @Test
    public void testRespondingTimeGraphAverageValues() throws Exception {
        setGraphType(PerformancePublisher.ART);
        target.doRespondingTimeGraph(request, response);
        assertArrayEquals(new Number[]{4142L}, toArray(target.dataset));
    }

    @Test
    public void testRespondingTimeGraphMedianValues() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doRespondingTimeGraph(request, response);
        assertArrayEquals(new Number[]{501L}, toArray(target.dataset));
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
        assertArrayEquals(new Number[]{598L, 63L}, toArray(target.dataset));
    }

    @Test
    public void testThroughputGraph() throws Exception {
        target.doThroughputGraph(request, response);
        assertArrayEquals(new Number[]{0.04515946937623483}, toArray(target.dataset));
    }

    @Test
    public void testRespondingTimeGraphPerTestCaseMode() throws Exception {
        setGraphType(PerformancePublisher.MRT);
        target.doRespondingTimeGraphPerTestCaseMode(request, response);
        assertArrayEquals(new Number[]{598L, 63L}, toArray(target.dataset));
    }

    @Test
    public void testErrorsGraph() throws Exception {
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
