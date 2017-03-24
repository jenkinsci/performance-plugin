package hudson.plugins.performance;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExternalBuildReportTest {

    @Test
    public void test() throws Exception {
        ExternalBuildReport report = new ExternalBuildReport("http://some.url.com");

        assertEquals("View External Report", report.getDisplayName());
        assertEquals("graph.gif", report.getIconFileName());
        assertEquals("http://some.url.com", report.getUrlName());
        assertEquals(null, report.getTarget());
    }
}