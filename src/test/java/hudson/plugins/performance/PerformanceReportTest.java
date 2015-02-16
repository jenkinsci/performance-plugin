package hudson.plugins.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import hudson.util.StreamTaskListener;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class PerformanceReportTest {

	private PerformanceReport performanceReport;

	@Before
	public void setUp() throws Exception {
		PerformanceBuildAction buildAction = EasyMock
				.createMock(PerformanceBuildAction.class);
		performanceReport = new PerformanceReport();
		performanceReport.setBuildAction(buildAction);
	}

	@Test
	public void testAddSample() throws Exception {
		PrintStream printStream = EasyMock.createMock(PrintStream.class);
		EasyMock.expect(
				performanceReport.getBuildAction().getHudsonConsoleWriter())
				.andReturn(printStream);
		printStream
				.println("label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
		EasyMock.replay(printStream);
		EasyMock.replay(performanceReport.getBuildAction());

		HttpSample sample1 = new HttpSample();
		sample1.setDate(new Date());
		performanceReport.addSample(sample1);

		sample1.setUri("invalidCharacter/");
		performanceReport.addSample(sample1);
		UriReport uriReport = performanceReport.getUriReportMap().get(
				"invalidCharacter_");
		assertNotNull(uriReport);

		String uri = "uri";
		sample1.setUri(uri);
		performanceReport.addSample(sample1);
		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		uriReport = uriReportMap.get(uri);
		assertNotNull(uriReport);
		List<Long> durations = uriReport.getDurations();
		assertEquals(1, durations.size());
		assertEquals(sample1.getUri(), uriReport.getUri());
	}


/*    @Test
	public void testCountError() throws SAXException {
        HttpSample sample1 = new HttpSample();
		sample1.setSuccessful(false);
		sample1.setUri("sample1");
		performanceReport.addSample(sample1);

		HttpSample sample2 = new HttpSample();
		sample2.setSuccessful(true);
		sample2.setUri("sample2");
		performanceReport.addSample(sample2);
		assertEquals(1, performanceReport.countErrors());
	}
*/
	@Test
	public void testPerformanceReport() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJMeter(new File(
				"src/test/resources/JMeterResults.jtl"));
		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		assertEquals(2, uriReportMap.size());
		String loginUri = "Home";
		UriReport firstUriReport = uriReportMap.get(loginUri);
		assertEquals(loginUri, firstUriReport.getUri());
		assertEquals(14720, firstUriReport.getDurations().get(0).longValue());
		assertEquals(1296846792004L, firstUriReport.getStart().getTime());
		assertFalse(firstUriReport.isFailed());
		String logoutUri = "Workgroup";
		UriReport secondUriReport = uriReportMap.get(logoutUri);
		assertEquals(logoutUri, secondUriReport.getUri());
		assertEquals(278, secondUriReport.getDurations().get(0).longValue());
		assertEquals(1296846969096L + 58L, secondUriReport.getEnd().getTime());
    assertFalse(secondUriReport.isFailed());
	}

	private PerformanceReport parseOneJMeter(File f) throws IOException {
		return new JMeterParser("").parse(null, Collections.singleton(f),
				new StreamTaskListener(System.out)).iterator().next();
	}

	private PerformanceReport parseOneJUnit(File f) throws IOException {
		return new JUnitParser("").parse(null, Collections.singleton(f),
				new StreamTaskListener(System.out)).iterator().next();
	}

	@Test
	public void testPerformanceNonHTTPSamplesMultiThread() throws IOException,
			SAXException {
		PerformanceReport performanceReport = parseOneJMeter(new File(
				"src/test/resources/JMeterResultsMultiThread.jtl"));

		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		assertEquals(1, uriReportMap.size());

		String uri = "WebService(SOAP) Request";
		UriReport report = uriReportMap.get(uri);
		assertNotNull(report);

		int[] expectedDurations = {894, 1508, 1384, 1581, 996};
		for (int i = 0; i < expectedDurations.length; i++) {
		  final Long duration = report.getDurations().get(i);
			assertEquals(expectedDurations[i], duration.intValue());
		}
	}

	@Test
	public void testPerformanceReportJUnit() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJUnit(new File(
				"src/test/resources/TEST-JUnitResults.xml"));
		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		assertEquals(5, uriReportMap.size());
		String firstUri = "hudson.plugins.performance.UriReportTest.testGetMin";
		UriReport firstUriReport = uriReportMap.get(firstUri);
		HttpSample firstHttpSample = firstUriReport.getHttpSampleList().get(0);
		assertEquals(firstUri, firstHttpSample.getUri());
		assertEquals(31, firstHttpSample.getDuration());
		assertEquals(new Date(0L), firstHttpSample.getDate());
		assertFalse(firstUriReport.isFailed());
		String lastUri = "hudson.plugins.performance.UriReportTest.testGetMax";
		UriReport secondUriReport = uriReportMap.get(lastUri);
		assertEquals(lastUri, secondUriReport.getUri());
		assertEquals(26, secondUriReport.getDurations().get(0).longValue());
		assertEquals(0L, secondUriReport.getStart().getTime());
		assertTrue(secondUriReport.isFailed());
	}
        
	 @Test
	public void testIssue5571() throws IOException, SAXException {
	  PerformanceReport performanceReport = parseOneJUnit(new File(
	        "src/test/resources/jUnitIssue5571.xml"));
	    Map<String, UriReport> uriReportMap = performanceReport
	        .getUriReportMap();
	    assertEquals(1, uriReportMap.size());
	    String uri = "junit.framework.JUnit4TestCaseFacade.unknown";
	    UriReport report = uriReportMap.get(uri);
	    assertEquals(uri, report.getUri());
	    assertEquals(890, report.getDurations().get(0).longValue());
      assertEquals(50, report.getDurations().get(1).longValue());
	    assertEquals(0L, report.getStart().getTime());      
      assertFalse(report.isFailed());
      assertEquals(33, report.getMedian());
	}

  @Test
	public void testPerformanceReportMultiLevel() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJMeter(new File(
				"src/test/resources/JMeterResultsMultiLevel.jtl"));
		Map<String, UriReport> uriReportMap = performanceReport
				.getUriReportMap();
		assertEquals(2, uriReportMap.size());
		UriReport report = uriReportMap.get("Home");
		assertNotNull(report);
	}

    @Test
    public void testGetUriListOrdered() throws IOException, SAXException {
        PerformanceReport performanceReport = parseOneJMeter(new File("src/test/resources/JMeterResultsRandomUri.jtl"));
        List<UriReport> uriReports = performanceReport.getUriListOrdered();
        assertEquals("Ant", uriReports.get(0).getUri());
    }
}
