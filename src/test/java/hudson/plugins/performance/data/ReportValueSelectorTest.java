package hudson.plugins.performance.data;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import hudson.model.AbstractProject;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.reports.AbstractReport;
import hudson.util.DescribableList;

@RunWith(MockitoJUnitRunner.class)
public class ReportValueSelectorTest {

    private static final long VALUE_ART = 1L;
    private static final long VALUE_MRT = 2L;
    private static final long VALUE_PRT = 3L;

    @Mock
    private PerformancePublisher publisher;
    @Mock
    private AbstractReport report;

    @Before
    public void setUp() throws Exception {
        when(report.getAverage()).thenReturn(VALUE_ART);
        when(report.getMedian()).thenReturn(VALUE_MRT);
        when(report.get90Line()).thenReturn(VALUE_PRT);
    }

    @Test
    public void testAverageConfiguration() throws Exception {
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.ART);
        ReportValueSelector valueSelector = ReportValueSelector.get(publisher);
        assertEquals(VALUE_ART, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.ART, valueSelector.getGraphType());
    }

    @Test
    public void testMedianConfiguration() throws Exception {
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.MRT);
        ReportValueSelector valueSelector = ReportValueSelector.get(publisher);
        assertEquals(VALUE_MRT, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.MRT, valueSelector.getGraphType());
    }

    @Test
    public void testPercentileConfiguration() throws Exception {
        when(publisher.getGraphType()).thenReturn(PerformancePublisher.PRT);
        ReportValueSelector valueSelector = ReportValueSelector.get(publisher);
        assertEquals(VALUE_PRT, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.PRT, valueSelector.getGraphType());
    }

    @Test
    public void testFallbackNoPublisher() throws Exception {
        ReportValueSelector valueSelector = ReportValueSelector.get((PerformancePublisher) null);
        assertEquals(VALUE_ART, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.ART, valueSelector.getGraphType());
    }

    @Test
    public void testFallbackMissingGraphConfig() throws Exception {
        when(publisher.getGraphType()).thenReturn(null);
        ReportValueSelector valueSelector = ReportValueSelector.get(publisher);
        assertEquals(VALUE_ART, valueSelector.getValue(report));
        assertEquals(PerformancePublisher.ART, valueSelector.getGraphType());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPublisherSearchedFromAbstractProject() throws Exception {
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
