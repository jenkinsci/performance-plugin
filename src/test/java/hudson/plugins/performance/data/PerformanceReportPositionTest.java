package hudson.plugins.performance.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class PerformanceReportPositionTest {

    @Test
    public void test() throws Exception {
        PerformanceReportPosition position = new PerformanceReportPosition();

        position.setPerformanceReportPosition("test1");
        position.setSummarizerReportType("test2");
        position.setSummarizerTrendUri("test3");

        assertEquals("test1", position.getPerformanceReportPosition());
        assertEquals("test2", position.getSummarizerReportType());
        assertEquals("test3", position.getSummarizerTrendUri());
    }
}