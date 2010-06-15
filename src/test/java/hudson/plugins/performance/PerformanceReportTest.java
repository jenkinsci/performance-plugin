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
		PerformanceBuildAction buildAction = EasyMock.createMock(PerformanceBuildAction.class);
		performanceReport = new PerformanceReport();
		performanceReport.setBuildAction(buildAction);
	}

	@Test
	public void testAddSample() throws Exception {
		PrintStream printStream = EasyMock.createMock(PrintStream.class);
		EasyMock.expect(performanceReport.getBuildAction().getHudsonConsoleWriter()).andReturn(printStream);
		printStream.println("label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
		EasyMock.replay(printStream);
		EasyMock.replay(performanceReport.getBuildAction());

		HttpSample sample1 = new HttpSample();
		performanceReport.addSample(sample1);

		sample1.setUri("invalidCharacter/");
		performanceReport.addSample(sample1);
		UriReport uriReport = performanceReport.getUriReportMap().get("invalidCharacter_");
		assertNotNull(uriReport);

		String uri = "uri";
		sample1.setUri(uri);
		performanceReport.addSample(sample1);
		Map<String, UriReport> uriReportMap = performanceReport.getUriReportMap();
		uriReport = uriReportMap.get(uri);
		assertNotNull(uriReport);
		List<HttpSample> httpSampleList = uriReport.getHttpSampleList();
		assertEquals(1, httpSampleList.size());
		assertEquals(sample1, httpSampleList.get(0));
	}

	@Test
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

	@Test
	public void testPerformanceReport() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJMeter(new File("src/test/resources/JMeterResults.jtl"));
		Map<String, UriReport> uriReportMap = performanceReport.getUriReportMap();
		assertEquals(2, uriReportMap.size());
		String loginUri = "Login";
		UriReport firstUriReport = uriReportMap.get(loginUri);
		HttpSample firstHttpSample = firstUriReport.getHttpSampleList().get(0);
		assertEquals(loginUri, firstHttpSample.getUri());
		assertEquals(31, firstHttpSample.getDuration());
		assertEquals(new Date(1219160357175L), firstHttpSample.getDate());
		assertTrue(firstHttpSample.isSuccessful());
		String logoutUri = "Logout";
		UriReport secondUriReport = uriReportMap.get(logoutUri);
		HttpSample secondHttpSample = secondUriReport.getHttpSampleList().get(0);
		assertEquals(logoutUri, secondHttpSample.getUri());
		assertEquals(26, secondHttpSample.getDuration());
		assertEquals(new Date(1219160357663L), secondHttpSample.getDate());
		assertFalse(secondHttpSample.isSuccessful());
	}

	private PerformanceReport parseOneJMeter(File f) throws IOException {
		return new JMeterParser("").parse(null, Collections.singleton(f), new StreamTaskListener(System.out)).iterator().next();
	}

	private PerformanceReport parseOneJUnit(File f) throws IOException {
		return new JUnitParser("").parse(null, Collections.singleton(f), new StreamTaskListener(System.out)).iterator().next();
	}

	@Test
	public void testPerformanceNonHTTPSamplesMultiThread() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJMeter(new File("src/test/resources/JMeterResultsMultiThread.jtl"));

		Map<String, UriReport> uriReportMap = performanceReport.getUriReportMap();
		assertEquals(1, uriReportMap.size());

		String uri = "WebService(SOAP) Request";
		UriReport report = uriReportMap.get(uri);
		assertNotNull(report);

		int[] expectedDurations = { 894, 1508, 1384, 1581, 996 };
		for (int i = 0; i < expectedDurations.length; i++) {
			HttpSample sample = report.getHttpSampleList().get(i);
			assertEquals(expectedDurations[i], sample.getDuration());
		}
	}

	@Test
	public void testPerformanceReportJUnit() throws IOException, SAXException {
		PerformanceReport performanceReport = parseOneJUnit(new File("src/test/resources/TEST-JUnitResults.xml"));
		Map<String, UriReport> uriReportMap = performanceReport.getUriReportMap();
		assertEquals(5, uriReportMap.size());
		String firstUri = "testGetMin";
		UriReport firstUriReport = uriReportMap.get(firstUri);
		HttpSample firstHttpSample = firstUriReport.getHttpSampleList().get(0);
		assertEquals(firstUri, firstHttpSample.getUri());
		assertEquals(31, firstHttpSample.getDuration());
		assertEquals(new Date(0L), firstHttpSample.getDate());
		assertTrue(firstHttpSample.isSuccessful());
		String lastUri = "testGetMax";
		UriReport secondUriReport = uriReportMap.get(lastUri);
		HttpSample secondHttpSample = secondUriReport.getHttpSampleList().get(0);
		assertEquals(lastUri, secondHttpSample.getUri());
		assertEquals(26, secondHttpSample.getDuration());
		assertEquals(new Date(0L), secondHttpSample.getDate());
		assertFalse(secondHttpSample.isSuccessful());
	}
}
