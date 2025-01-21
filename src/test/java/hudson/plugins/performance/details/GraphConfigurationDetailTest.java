package hudson.plugins.performance.details;

import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
@ExtendWith(MockitoExtension.class)
class GraphConfigurationDetailTest {

    @Mock
    private StaplerRequest request;

    @Test
    void testDefault(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("testProject");

        GraphConfigurationDetail detail =
                new GraphConfigurationDetail(project, "testPluginName", request);

        assertEquals(GraphConfigurationDetail.NONE_CONFIG, detail.getConfigType());
        assertTrue(detail.isNone());
        assertFalse(detail.isBuildCount());
        assertFalse(detail.isBuildNth());
        assertFalse(detail.isDate());
        assertTrue(detail.isDefaultDates());
        assertEquals("Configure", detail.getDisplayName());

        assertEquals(0, detail.getBuildCount());
        detail.setBuildCount(3);
        assertEquals(3, detail.getBuildCount());

        assertEquals(1, detail.getBuildStep());
        detail.setBuildStep(5);
        assertEquals(5, detail.getBuildStep());

        assertEquals(GraphConfigurationDetail.DEFAULT_DATE, detail.getFirstDayCount());
        assertEquals(GraphConfigurationDetail.DEFAULT_DATE, detail.getLastDayCount());
        detail.setFirstDayCount("testDatePattern");
        detail.setLastDayCount("testDatePattern");
        assertEquals("testDatePattern", detail.getFirstDayCount());
        assertEquals("testDatePattern", detail.getLastDayCount());
        assertFalse(detail.isDefaultDates());

        detail.setConfigType("TEST_CONFIG_TYPE");
        assertEquals("TEST_CONFIG_TYPE", detail.getConfigType());
    }

    @Test
    void testCalendar(JenkinsRule j) throws Exception {
        GregorianCalendar calendar = GraphConfigurationDetail.getGregorianCalendarFromString("9/5/2001");

        assertEquals(2001, calendar.get(GregorianCalendar.YEAR));
        assertEquals(9, calendar.get(GregorianCalendar.DAY_OF_MONTH));
        assertEquals(Calendar.MAY, calendar.get(GregorianCalendar.MONTH));
    }
}