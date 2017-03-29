package hudson.plugins.performance.descriptors;

import hudson.plugins.performance.parsers.IagoParser;
import hudson.plugins.performance.parsers.JMeterCsvParser;
import hudson.plugins.performance.parsers.JMeterParser;
import hudson.plugins.performance.parsers.JUnitParser;
import hudson.plugins.performance.parsers.JmeterSummarizerParser;
import hudson.plugins.performance.parsers.TaurusParser;
import hudson.plugins.performance.parsers.WrkSummarizerParser;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.*;

public class PerformanceReportParserDescriptorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @After
    public void shutdown() throws Exception {
        j.after();
    }

    @Test
    public void name() throws Exception {
        PerformanceReportParserDescriptor descriptor = PerformanceReportParserDescriptor.getById(IagoParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor);
        assertTrue(descriptor instanceof IagoParser.DescriptorImpl);

        PerformanceReportParserDescriptor descriptor2 = PerformanceReportParserDescriptor.getById(JMeterCsvParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor2);
        assertTrue(descriptor2 instanceof JMeterCsvParser.DescriptorImpl);

        PerformanceReportParserDescriptor descriptor3 = PerformanceReportParserDescriptor.getById(JMeterParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor3);
        assertTrue(descriptor3 instanceof JMeterParser.DescriptorImpl);

        PerformanceReportParserDescriptor descriptor4 = PerformanceReportParserDescriptor.getById(JmeterSummarizerParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor4);
        assertTrue(descriptor4 instanceof JmeterSummarizerParser.DescriptorImpl);

        PerformanceReportParserDescriptor descriptor5 = PerformanceReportParserDescriptor.getById(JUnitParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor5);
        assertTrue(descriptor5 instanceof JUnitParser.DescriptorImpl);


        PerformanceReportParserDescriptor descriptor6 = PerformanceReportParserDescriptor.getById(TaurusParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor6);
        assertTrue(descriptor6 instanceof TaurusParser.DescriptorImpl);

        PerformanceReportParserDescriptor descriptor7 = PerformanceReportParserDescriptor.getById(WrkSummarizerParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor7);
        assertTrue(descriptor7 instanceof WrkSummarizerParser.DescriptorImpl);

        assertNull(ConstraintDescriptor.getById("null"));
    }
}