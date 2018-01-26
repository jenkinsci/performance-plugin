package hudson.plugins.performance.parsers;

import java.io.File;

import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.reports.PerformanceReportTest;

/**
 * Allows tests in other packages to call parse(File)
 */
public class JMeterTestHelper {

    public static PerformanceReport parse(String resourceName) throws Exception {
        File xmlFile = new File(JMeterTestHelper.class.getResource(resourceName).toURI());
        return new JMeterParser(null, PerformanceReportTest.DEFAULT_PERCENTILES).parse(xmlFile);
    }
}
