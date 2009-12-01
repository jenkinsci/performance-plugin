package hudson.plugins.jmeter;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class UriReportTest {
	
	private static final long AVERAGE = 5;
	private static final long MIN = 0;
	private static final long MAX = 10;
	private UriReport uriReport;
	
	@Before
	public void setUp() {
		uriReport= new UriReport(null, null, null);
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
		uriReport.addHttpSample(httpSample1 );
		uriReport.addHttpSample(httpSample2);
		uriReport.addHttpSample(httpSample3);
	}

	@Test
	public void testCountErrors() {
		assertEquals(2, uriReport.countErrors());
	}

	@Test
	public void testGetAverage() {
		assertEquals(AVERAGE, uriReport.getAverage());
	}

	@Test
	public void testGetMax() {
		assertEquals(MAX, uriReport.getMax());
	}

	@Test
	public void testGetMin() {
		assertEquals(MIN, uriReport.getMin());
	}

	@Test
	public void testIsFailed() {
		assertTrue(uriReport.isFailed());
	}

}
