package hudson.plugins.performance.details;

import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Run;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.PerformanceReportMap;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.actions.PerformanceProjectAction;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.UriReport;
import hudson.tasks.Publisher;
import hudson.util.ChartUtil;
import hudson.util.DescribableList;
import hudson.util.RunList;
import org.jfree.data.category.CategoryDataset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@WithJenkins
@ExtendWith(MockitoExtension.class)
public class TestSuiteReportDetailTest {

    private static final String TEST_URI = "myUri";
    private static final String FILENAME = "perf.jtl";

    @Mock
    FreeStyleBuild run;
    @Mock
    Project<?, ?> project;
    @Mock
    PerformancePublisher publisher;
    @Mock
    PerformanceReportMap reportMap;
    @Mock
    PerformanceBuildAction buildAction;
    @Mock
    PerformanceReport report;

    private Map<String, UriReport> uriReportMap = new HashMap<String, UriReport>();

    @BeforeEach
    void setUp() throws Exception {
        uriReportMap.put(TEST_URI, getUriReport());
    }

    private UriReport getUriReport() {
        UriReport uriReport = new UriReport(report, TEST_URI, TEST_URI);
        final HttpSample sample1 = new HttpSample();
        sample1.setSuccessful(true);
        sample1.setDuration(100);
        sample1.setDate(new Date(System.currentTimeMillis() - 1000));
        final HttpSample sample2 = new HttpSample();
        sample2.setSuccessful(true);
        sample2.setDuration(1000);
        sample2.setDate(new Date());
        uriReport.addHttpSample(sample1);
        uriReport.addHttpSample(sample2);
        return uriReport;
    }

    @Test
    void chartDatasetHasAverageOfSamples(JenkinsRule j) throws Exception {
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.ART);
        assertEquals(550L, getDatasetValue());
    }

    @Test
    void chartDatasetHasMedianOfSamples(JenkinsRule j) throws Exception {
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.MRT);
        assertEquals(100L, getDatasetValue());
    }

    private Number getDatasetValue() {
        DescribableList<Publisher, Descriptor<Publisher>> publishers = new DescribableList<>(project);
        publishers.add(publisher);
        run.number = 1;
        when(run.getAction(PerformanceBuildAction.class)).thenReturn(buildAction);
        when(project.getPublishersList()).thenReturn(publishers);
        when(buildAction.getPerformanceReportMap()).thenReturn(reportMap);
        when(reportMap.getPerformanceReport(FILENAME)).thenReturn(report);
        when(report.getUriReportMap()).thenReturn(uriReportMap);

        final TestSuiteReportDetail reportDetail = new TestSuiteReportDetail(project, FILENAME, alwaysInRangeMock());
        final CategoryDataset dataset = reportDetail.getChartDatasetBuilderForBuilds(TEST_URI, RunList.fromRuns(Collections.singletonList(run))).build();
        return dataset.getValue(TEST_URI, new ChartUtil.NumberOnlyBuildLabel((Run<?, ?>) run));
    }

    public static PerformanceProjectAction.Range alwaysInRangeMock() {
        final PerformanceProjectAction.Range mock = mock(PerformanceProjectAction.Range.class, withSettings().strictness(Strictness.LENIENT));
        when(mock.in(anyInt())).thenReturn(true);
        when(mock.includedByStep(anyInt())).thenReturn(true);
        return mock;
    }

    @Test
    void testFlow(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.createExecutable();

        TestSuiteReportDetail reportDetail = new TestSuiteReportDetail(project, "testFilename", alwaysInRangeMock());

        assertEquals("Test Suite report", reportDetail.getDisplayName());
        assertEquals("testFilename", reportDetail.getFilename());
        assertEquals(project, reportDetail.getProject());

        List<String> list = reportDetail.getPerformanceReportTestCaseList();
        assertEquals(0, list.size());
    }
}