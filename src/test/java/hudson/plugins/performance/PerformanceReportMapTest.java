package hudson.plugins.performance;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import hudson.plugins.performance.reports.PerformanceReport;
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
    protected PerformanceBuildAction performanceBuildAction;
    @Mock
    protected PerformanceReportMap reportMap;

    @Before
    public void reportMapSetup() throws Exception {
        PerformanceBuildAction buildAction = mock(PerformanceBuildAction.class);
        when(buildAction.getBuild()).thenReturn(build);
        when(build.getParent()).thenReturn(project);
        when(request.getParameter("performanceReportPosition")).thenReturn("JMeterResults.jtl");
        when(project.getBuilds()).thenReturn(RunList.fromRuns(Collections.singletonList(build)));
        when(build.getAction(PerformanceBuildAction.class)).thenReturn(performanceBuildAction);
        when(performanceBuildAction.getPerformanceReportMap()).thenReturn(reportMap);
        when(reportMap.getPerformanceReport("JMeterResults.jtl")).thenReturn(report);
        target = new TestablePerformanceReportMap(buildAction, mock(TaskListener.class));
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
    }
}
