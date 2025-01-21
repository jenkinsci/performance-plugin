package hudson.plugins.performance;

import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.parsers.JMeterTestHelper;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.util.DescribableList;
import org.jfree.data.category.CategoryDataset;
import org.junit.jupiter.api.BeforeEach;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.quality.Strictness;

import javax.servlet.ServletOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;


public abstract class AbstractGraphGenerationTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    protected PerformanceBuildAction performanceBuildAction;
    @Mock(strictness = Mock.Strictness.LENIENT)
    protected AbstractProject project;
    @Mock(strictness = Mock.Strictness.LENIENT)
    protected Run build;
    @Mock(strictness = Mock.Strictness.LENIENT)
    protected StaplerRequest request;
    @Mock(strictness = Mock.Strictness.LENIENT)
    protected StaplerResponse response;

    protected PerformanceReport report;

    @Mock(strictness = Mock.Strictness.LENIENT)
    protected PerformanceReportMap reportMap;

    @BeforeEach
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
        DescribableList list = mock(DescribableList.class, withSettings().strictness(Strictness.LENIENT));
        when(project.getPublishersList()).thenReturn(list);
        PerformancePublisher publisher = mock(PerformancePublisher.class, withSettings().strictness(Strictness.LENIENT));
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
