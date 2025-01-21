package hudson.plugins.performance.data;

import hudson.model.AbstractProject;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.reports.AbstractReport;
import hudson.util.DescribableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportValueSelectorTest {

    private static final long VALUE_ART = 1L;
    private static final long VALUE_MRT = 2L;
    private static final long VALUE_PRT = 3L;

    @Mock
    private PerformancePublisher publisher;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private AbstractReport report;

    @BeforeEach
    void setUp() throws Exception {
        when(report.getAverage()).thenReturn(VALUE_ART);
        when(report.getMedian()).thenReturn(VALUE_MRT);
        when(report.get90Line()).thenReturn(VALUE_PRT);
    }

    @Test
    void testAverageConfiguration() throws Exception {
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.ART);
        ReportValueSelector valueSelector = ReportValueSelector.get(publisher);
        assertEquals(VALUE_ART, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.ART, valueSelector.getGraphType());
    }

    @Test
    void testMedianConfiguration() throws Exception {
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.MRT);
        ReportValueSelector valueSelector = ReportValueSelector.get(publisher);
        assertEquals(VALUE_MRT, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.MRT, valueSelector.getGraphType());
    }

    @Test
    void testPercentileConfiguration() throws Exception {
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.PRT);
        ReportValueSelector valueSelector = ReportValueSelector.get(publisher);
        assertEquals(VALUE_PRT, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.PRT, valueSelector.getGraphType());
    }

    @Test
    void testFallbackNoPublisher() throws Exception {
        ReportValueSelector valueSelector = ReportValueSelector.get((PerformancePublisher) null);
        assertEquals(VALUE_ART, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.ART, valueSelector.getGraphType());
    }

    @Test
    void testFallbackMissingGraphConfig() throws Exception {
        when(publisher.getGraphType()).thenReturn(null);
        ReportValueSelector valueSelector = ReportValueSelector.get(publisher);
        assertEquals(VALUE_ART, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.ART, valueSelector.getGraphType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPublisherSearchedFromAbstractProject() throws Exception {
        AbstractProject project = mock(AbstractProject.class);
        DescribableList publishers = mock(DescribableList.class);
        when(project.getPublishersList()).thenReturn(publishers);
        when(publishers.get(PerformancePublisher.class)).thenReturn(publisher);
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.MRT);
        ReportValueSelector valueSelector = ReportValueSelector.get(project);
        assertEquals(PerformancePublisher.MRT, valueSelector.getGraphType());
        verify(publishers, atLeastOnce()).get(PerformancePublisher.class);
    }
}
