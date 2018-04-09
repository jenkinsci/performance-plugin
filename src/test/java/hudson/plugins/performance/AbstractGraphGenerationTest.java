package hudson.plugins.performance;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;

import hudson.plugins.performance.actions.PerformanceBuildAction;
import org.jfree.data.category.CategoryDataset;
import org.junit.Before;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;

import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.performance.parsers.JMeterTestHelper;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.util.DescribableList;


public abstract class AbstractGraphGenerationTest {

    @Mock
    protected PerformanceBuildAction performanceBuildAction;
    @Mock
    protected AbstractProject project;
    @Mock
    protected Run build;
    @Mock
    protected StaplerRequest request;
    @Mock
    protected StaplerResponse response;

    protected PerformanceReport report;

    @Mock
    protected PerformanceReportMap reportMap;

    @Before
    public void baseSetup() throws Exception {
        report = JMeterTestHelper.parse("/JMeterResults.jtl");
        report.setBuildAction(performanceBuildAction);
        when(build.getDisplayName()).thenReturn("mock");
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        when(build.getAction(PerformanceBuildAction.class)).thenReturn(performanceBuildAction);
        when(performanceBuildAction.getPerformanceReportMap()).thenReturn(reportMap);
        when(performanceBuildAction.getBuild()).thenReturn(build);
        when(performanceBuildAction.getParserByDisplayName("JmeterSummarizer")).thenReturn(null);
        when(performanceBuildAction.getParserByDisplayName("Iago")).thenReturn(null);
        when(reportMap.getPerformanceReport("JMeterResults.jtl")).thenReturn(report);
    }

    protected void setGraphType(String graphType) {
        DescribableList list = mock(DescribableList.class);
        when(project.getPublishersList()).thenReturn(list);
        PerformancePublisher publisher = mock(PerformancePublisher.class);
        when(list.get(PerformancePublisher.class)).thenReturn(publisher);
        when(publisher.getGraphType()).thenReturn(graphType);
    }

    protected Number[] toArray(CategoryDataset cd) {
        List<Number> values = new ArrayList<>();
        for (int i = 0; i < cd.getRowCount(); i++) {
            for (int j = 0; j < cd.getColumnCount(); j++) {
                values.add(cd.getValue(i, j));
            }
        }
        return values.toArray(new Number[0]);
    }
}
