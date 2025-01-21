package hudson.plugins.performance.reports;

import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.reports.UriReport.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UriReportTest {

    private static final String HTTP_200 = "200";
    private static final long AVERAGE = 5;
    private static final long MIN = 0;
    private static final long MAX = 10;
    private UriReport uriReport;

    @BeforeEach
    void setUp() {
        PerformanceReport performanceReport = new PerformanceReport(PerformanceReportTest.DEFAULT_PERCENTILES);
        uriReport = new UriReport(performanceReport, null, null);
        HttpSample httpSample1 = new HttpSample();
        httpSample1.setDuration(MAX);
        Date date = new Date();
        httpSample1.setDate(date);
        httpSample1.setSuccessful(false);
        HttpSample httpSample2 = new HttpSample();
        httpSample2.setDuration(AVERAGE);
        httpSample2.setDate(date);
        httpSample2.setSuccessful(true);
        HttpSample httpSample3 = new HttpSample();
        httpSample3.setDuration(MIN);
        httpSample3.setDate(date);
        httpSample3.setSuccessful(false);
        uriReport.addHttpSample(httpSample1);
        uriReport.addHttpSample(httpSample2);
        uriReport.addHttpSample(httpSample3);
    }

    @Test
    void testHasSamples() throws Exception {
        assertTrue(uriReport.hasSamples());
    }

    @Test
    void testCountErrors() {
        assertEquals(2, uriReport.countErrors());
    }

    @Test
    void testGetAverage() {
        assertEquals(AVERAGE, uriReport.getAverage());
    }

    @Test
    void testGetMax() {
		assertEquals(MAX, uriReport.getMax());
	}

    @Test
    void testGetMin() {
		assertEquals(MIN, uriReport.getMin());
	}

    @Test
    void testIsFailed() {
        assertTrue(uriReport.isFailed());
    }

    /**
     * Same dates, different duration. Shortest duration should be ordered first.
     */
    @Test
    void testCompareSameDateDifferentDuration() {
        // setup fixture
        final List<Sample> samples = new ArrayList<Sample>();
        samples.add(new Sample(new Date(1), 2, HTTP_200, true, false));
        samples.add(new Sample(new Date(1), 1, HTTP_200, true, false));

        // execute system under test
        Collections.sort(samples);

        // verify result
        final Iterator<Sample> iter = samples.iterator();
        assertEquals(1, iter.next().duration);
        assertEquals(2, iter.next().duration);
    }

    /**
     * Different dates, same duration. Oldest date should be ordered first.
     */
    @Test
    void testCompareDifferentDateSameDuration() {
        // setup fixture
        final List<Sample> samples = new ArrayList<Sample>();
        samples.add(new Sample(new Date(2), 1, HTTP_200, true, false));
        samples.add(new Sample(new Date(1), 1, HTTP_200, true, false));

        // execute system under test
        Collections.sort(samples);

        // verify result
        final Iterator<Sample> iter = samples.iterator();
        assertEquals(1, iter.next().date.getTime());
        assertEquals(2, iter.next().date.getTime());
    }

    /**
     * Different dates, different duration. Shortest duration should be ordered first.
     */
    @Test
    void testCompareDifferentDateDifferentDuration() {
        // setup fixture
        final List<Sample> samples = new ArrayList<Sample>();
        samples.add(new Sample(new Date(1), 2, HTTP_200, true, false));
        samples.add(new Sample(new Date(2), 1, HTTP_200, true, false));

        // execute system under test
        Collections.sort(samples);

        // verify result
        final Iterator<Sample> iter = samples.iterator();
        assertEquals(1, iter.next().duration);
        assertEquals(2, iter.next().duration);
    }

    /**
     * Null dates. Ordering is unspecified, but should not cause exceptions.
     */
    @Test
    void testCompareNullDateSameDuration() {
        // setup fixture
        final List<Sample> samples = new ArrayList<Sample>();
        samples.add(new Sample(null, 1, HTTP_200, true, false));
        samples.add(new Sample(null, 1, HTTP_200, true, false));

        assertDoesNotThrow(() -> {
            // execute system under test
            Collections.sort(samples);
        }, "A NullPointerException was thrown (which should not have happened).");
    }
}
