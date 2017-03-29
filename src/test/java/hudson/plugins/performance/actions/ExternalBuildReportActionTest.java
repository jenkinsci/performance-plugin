package hudson.plugins.performance.actions;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExternalBuildReportActionTest {

    @Test
    public void test() throws Exception {
        ExternalBuildReportAction report = new ExternalBuildReportAction("http://some.url.com");

        assertEquals("View External Report", report.getDisplayName());
        assertEquals("graph.gif", report.getIconFileName());
        assertEquals("http://some.url.com", report.getUrlName());
        assertEquals(null, report.getTarget());
    }
}