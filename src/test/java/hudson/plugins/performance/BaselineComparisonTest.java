package hudson.plugins.performance;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.data.TaurusFinalStats;
import hudson.plugins.performance.reports.PerformanceReport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BaselineComparisonTest extends AbstractGraphGenerationTest {
    private final static String LABEL = "BlazeDemo";

    @Mock
    protected Run prevBuild;
    @Mock
    protected PerformanceBuildAction prevAction;
    @Mock
    protected PerformanceReportMap prevReportMap;


    @Mock
    protected Run buildWithNumber3;
    @Mock
    protected PerformanceBuildAction actionWithNumber3;
    @Mock
    protected PerformanceReportMap reportMapWithNumber3;

    private Map<String, PerformanceReport> reportMapForBuild = new HashMap<>();
    private Map<String, PerformanceReport> reportMapForPreviousBuild = new HashMap<>();
    private Map<String, PerformanceReport> reportMapForBuildNumber3 = new HashMap<>();


    @Before
    public void setup() {
        when(build.getPreviousBuild()).thenReturn(prevBuild);
        when(prevBuild.getPreviousBuild()).thenReturn(buildWithNumber3);
        when(build.getNumber()).thenReturn(5);
        when(prevBuild.getNumber()).thenReturn(4);
        when(buildWithNumber3.getNumber()).thenReturn(3);

        prepareReportForBuild(5, 3, reportMapForBuild);
        prepareReportForBuild(4, 0, reportMapForPreviousBuild);
        prepareReportForBuild(3, 0, reportMapForBuildNumber3);
        when(reportMap.getPerformanceReportMap()).thenReturn(reportMapForBuild);


        when(prevBuild.getAction(PerformanceBuildAction.class)).thenReturn(prevAction);
        when(prevAction.getBuild()).thenReturn(prevBuild);
        when(prevAction.getPerformanceReportMap(false)).thenReturn(prevReportMap);
        when(prevReportMap.getPerformanceReportMap()).thenReturn(reportMapForPreviousBuild);

        when(buildWithNumber3.getAction(PerformanceBuildAction.class)).thenReturn(actionWithNumber3);
        when(actionWithNumber3.getBuild()).thenReturn(buildWithNumber3);
        when(actionWithNumber3.getPerformanceReportMap(false)).thenReturn(reportMapWithNumber3);
        when(reportMapWithNumber3.getPerformanceReportMap()).thenReturn(reportMapForBuildNumber3);
    }

    private void prepareReportForBuild(int num, int baseline, Map<String, PerformanceReport> map) {
        PerformanceReport report = new PerformanceReport();
        TaurusFinalStats sample = new TaurusFinalStats();
        sample.setLabel(LABEL);
        sample.setSucc(num * 30);
        sample.setFail(num * 3);
        sample.setPerc0(num * 10);
        sample.setPerc50(num * 10 + 1);
        sample.setPerc90(num * 10 + 2);
        sample.setPerc100(num * 10 + 3);
        sample.setAverageResponseTime(num * 10 + 6);
        report.addSample(sample, true);
        report.setBaselineBuild(baseline);
        map.put(LABEL, report);
    }

    @Test
    public void testBaselineBuild5() throws Exception {
        new PerformanceReportMap(performanceBuildAction, mock(TaskListener.class)) {
            @Override
            protected void parseReports(Run<?, ?> build, TaskListener listener, PerformanceReportCollector collector, String filename) throws IOException {
                setPerformanceReportMap(reportMapForBuild);
            }
        };

        // Comparison between build 5 and 3.
        PerformanceReport report = reportMapForBuild.get(LABEL);
        assertEquals(165, report.samplesCount());
        assertEquals(66, report.getSamplesCountDiff());

        assertEquals(56, report.getAverage());
        assertEquals(20, report.getAverageDiff());

        Map<Double, Long> percentilesValues = report.getPercentilesValues();
        Map<Double, Long> percentilesDiffValues = report.getPercentilesDiffValues();
        assertEquals(Long.valueOf(50), percentilesValues.get(0.0));
        assertEquals(Long.valueOf(51), percentilesValues.get(50.0));
        assertEquals(Long.valueOf(52), percentilesValues.get(90.0));
        assertEquals(Long.valueOf(53), percentilesValues.get(100.0));

        assertEquals(Long.valueOf(20), percentilesDiffValues.get(0.0));
        assertEquals(Long.valueOf(20), percentilesDiffValues.get(50.0));
        assertEquals(Long.valueOf(20), percentilesDiffValues.get(90.0));
        assertEquals(Long.valueOf(20), percentilesDiffValues.get(100.0));
    }

    @Test
    public void testBaselineBuild4() throws Exception {
        new PerformanceReportMap(prevAction, mock(TaskListener.class)) {
            @Override
            protected void parseReports(Run<?, ?> build, TaskListener listener, PerformanceReportCollector collector, String filename) throws IOException {
                setPerformanceReportMap(reportMapForPreviousBuild);
            }
        };

        // Comparison between build 4 and previous 3.
        PerformanceReport report = reportMapForPreviousBuild.get(LABEL);
        assertEquals(132, report.samplesCount());
        assertEquals(33, report.getSamplesCountDiff());

        assertEquals(46, report.getAverage());
        assertEquals(10, report.getAverageDiff());

        Map<Double, Long> percentilesValues = report.getPercentilesValues();
        Map<Double, Long> percentilesDiffValues = report.getPercentilesDiffValues();
        assertEquals(Long.valueOf(40), percentilesValues.get(0.0));
        assertEquals(Long.valueOf(41), percentilesValues.get(50.0));
        assertEquals(Long.valueOf(42), percentilesValues.get(90.0));
        assertEquals(Long.valueOf(43), percentilesValues.get(100.0));

        assertEquals(Long.valueOf(10), percentilesDiffValues.get(0.0));
        assertEquals(Long.valueOf(10), percentilesDiffValues.get(50.0));
        assertEquals(Long.valueOf(10), percentilesDiffValues.get(90.0));
        assertEquals(Long.valueOf(10), percentilesDiffValues.get(100.0));
    }

    @Test
    public void testBaselineBuild3() throws Exception {
        new PerformanceReportMap(actionWithNumber3, mock(TaskListener.class)) {
            @Override
            protected void parseReports(Run<?, ?> build, TaskListener listener, PerformanceReportCollector collector, String filename) throws IOException {
                setPerformanceReportMap(reportMapForBuildNumber3);
            }
        };

        // Comparison between build 3 and 3
        PerformanceReport report = reportMapForBuildNumber3.get(LABEL);
        assertEquals(99, report.samplesCount());
        assertEquals(0, report.getSamplesCountDiff());

        assertEquals(36, report.getAverage());
        assertEquals(0, report.getAverageDiff());

        Map<Double, Long> percentilesValues = report.getPercentilesValues();
        Map<Double, Long> percentilesDiffValues = report.getPercentilesDiffValues();
        assertEquals(Long.valueOf(30), percentilesValues.get(0.0));
        assertEquals(Long.valueOf(31), percentilesValues.get(50.0));
        assertEquals(Long.valueOf(32), percentilesValues.get(90.0));
        assertEquals(Long.valueOf(33), percentilesValues.get(100.0));

        assertEquals(Long.valueOf(0), percentilesDiffValues.get(0.0));
        assertEquals(Long.valueOf(0), percentilesDiffValues.get(50.0));
        assertEquals(Long.valueOf(0), percentilesDiffValues.get(90.0));
        assertEquals(Long.valueOf(0), percentilesDiffValues.get(100.0));
    }
}
