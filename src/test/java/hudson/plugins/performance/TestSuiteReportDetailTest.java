package hudson.plugins.performance;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.jfree.data.category.CategoryDataset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.model.FreeStyleBuild;
import hudson.model.Project;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.RunList;


@RunWith(MockitoJUnitRunner.class)
public class TestSuiteReportDetailTest {

  private static final String TEST_URI = "myUri";
  private static final String FILENAME = "perf.jtl";

  @Mock FreeStyleBuild run;
  @Mock PerformanceReportMap reportMap;
  @Mock PerformanceBuildAction buildAction;
  @Mock PerformanceReport report;

  private Map<String, UriReport> uriReportMap = new HashMap<String, UriReport>();

  @Before
  public void setUp() throws Exception {
    uriReportMap.put(TEST_URI, getUriReport());
  }

  private UriReport getUriReport() {
    UriReport uriReport = new UriReport(report, TEST_URI, TEST_URI);
    final HttpSample sample1 = new HttpSample();
    sample1.setSuccessful(true);
    sample1.setDuration(100);
    sample1.setDate(new Date(System.currentTimeMillis()-1000));
    final HttpSample sample2 = new HttpSample();
    sample2.setSuccessful(true);
    sample2.setDuration(1000);
    sample2.setDate(new Date());
    uriReport.addHttpSample(sample1);
    uriReport.addHttpSample(sample2);
    return uriReport;
  }

  @Test
  @SuppressWarnings("UnnecessaryBoxing")
  public void chartDatasetHasAverageOfSamples() throws Exception {
    run.number = 1;
    when(run.getAction(PerformanceBuildAction.class)).thenReturn(buildAction);
    when(buildAction.getPerformanceReportMap()).thenReturn(reportMap);
    when(reportMap.getPerformanceReport(FILENAME)).thenReturn(report);
    when(report.getUriReportMap()).thenReturn(uriReportMap);

    final TestSuiteReportDetail reportDetail = new TestSuiteReportDetail(mock(Project.class), FILENAME, alwaysInRangeMock());
    final CategoryDataset dataset = reportDetail.getChartDatasetBuilderForBuilds(TEST_URI, RunList.fromRuns(Collections.singletonList(run))).build();
    assertEquals(dataset.getValue(TEST_URI, new ChartUtil.NumberOnlyBuildLabel((Run<?, ?>) run)), new Long(550L));
  }

  public static PerformanceProjectAction.Range alwaysInRangeMock() {
    final PerformanceProjectAction.Range mock = mock(PerformanceProjectAction.Range.class);
    when(mock.in(anyInt())).thenReturn(true);
    when(mock.includedByStep(anyInt())).thenReturn(true);
    return mock;
  }
}