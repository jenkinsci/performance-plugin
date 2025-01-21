package hudson.plugins.performance.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExternalBuildReportActionTest {

    @Test
    void test() throws Exception {
        ExternalBuildReportAction report = new ExternalBuildReportAction("http://some.url.com");

        assertEquals("View External Report", report.getDisplayName());
        assertEquals("graph.gif", report.getIconFileName());
        assertEquals("http://some.url.com", report.getUrlName());
        assertNull(report.getTarget());
    }
}