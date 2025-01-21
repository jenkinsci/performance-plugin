package hudson.plugins.performance.descriptors;

import hudson.plugins.performance.parsers.IagoParser;
import hudson.plugins.performance.parsers.JMeterCsvParser;
import hudson.plugins.performance.parsers.JMeterParser;
import hudson.plugins.performance.parsers.JUnitParser;
import hudson.plugins.performance.parsers.JmeterSummarizerParser;
import hudson.plugins.performance.parsers.TaurusParser;
import hudson.plugins.performance.parsers.WrkSummarizerParser;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@WithJenkins
class PerformanceReportParserDescriptorTest {

    @Test
    void name(JenkinsRule j) throws Exception {
        PerformanceReportParserDescriptor descriptor = PerformanceReportParserDescriptor.getById(IagoParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor);
        assertInstanceOf(IagoParser.DescriptorImpl.class, descriptor);

        PerformanceReportParserDescriptor descriptor2 = PerformanceReportParserDescriptor.getById(JMeterCsvParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor2);
        assertInstanceOf(JMeterCsvParser.DescriptorImpl.class, descriptor2);

        PerformanceReportParserDescriptor descriptor3 = PerformanceReportParserDescriptor.getById(JMeterParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor3);
        assertInstanceOf(JMeterParser.DescriptorImpl.class, descriptor3);

        PerformanceReportParserDescriptor descriptor4 = PerformanceReportParserDescriptor.getById(JmeterSummarizerParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor4);
        assertInstanceOf(JmeterSummarizerParser.DescriptorImpl.class, descriptor4);

        PerformanceReportParserDescriptor descriptor5 = PerformanceReportParserDescriptor.getById(JUnitParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor5);
        assertInstanceOf(JUnitParser.DescriptorImpl.class, descriptor5);


        PerformanceReportParserDescriptor descriptor6 = PerformanceReportParserDescriptor.getById(TaurusParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor6);
        assertInstanceOf(TaurusParser.DescriptorImpl.class, descriptor6);

        PerformanceReportParserDescriptor descriptor7 = PerformanceReportParserDescriptor.getById(WrkSummarizerParser.DescriptorImpl.class.getName());
        assertNotNull(descriptor7);
        assertInstanceOf(WrkSummarizerParser.DescriptorImpl.class, descriptor7);

        assertNull(ConstraintDescriptor.getById("null"));
    }
}