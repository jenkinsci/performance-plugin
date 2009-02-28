package hudson.plugins.jmeter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.easymock.classextension.EasyMock;
import org.xml.sax.SAXException;

public class JMeterReportTest extends TestCase {

	private JMeterReport jmeterReport;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		JMeterBuildAction buildAction = EasyMock
				.createMock(JMeterBuildAction.class);
		jmeterReport = new JMeterReport();
		jmeterReport.setBuildAction(buildAction);
	}

	public void testAddSample() throws Exception {
		PrintStream printStream = EasyMock.createMock(PrintStream.class);
		EasyMock.expect(jmeterReport.getBuildAction().getHudsonConsoleWriter())
				.andReturn(printStream);
		printStream
				.println("label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
		EasyMock.replay(printStream);
		EasyMock.replay(jmeterReport.getBuildAction());

		HttpSample sample1 = new HttpSample();
		jmeterReport.addSample(sample1);

		sample1.setUri("invalidCharacter/");
		jmeterReport.addSample(sample1);
		UriReport uriReport = jmeterReport.getUriReportMap().get(
				"invalidCharacter_");
		assertNotNull(uriReport);

		String uri = "uri";
		sample1.setUri(uri);
		jmeterReport.addSample(sample1);
		Map<String, UriReport> uriReportMap = jmeterReport.getUriReportMap();
		uriReport = uriReportMap.get(uri);
		assertNotNull(uriReport);
		List<HttpSample> httpSampleList = uriReport.getHttpSampleList();
		assertEquals(1, httpSampleList.size());
		assertEquals(sample1, httpSampleList.get(0));
	}

	public void testCountError() throws SAXException {
		HttpSample sample1 = new HttpSample();
		sample1.setSuccessful(false);
		sample1.setUri("sample1");
		jmeterReport.addSample(sample1);

		HttpSample sample2 = new HttpSample();
		sample2.setSuccessful(true);
		sample2.setUri("sample2");
		jmeterReport.addSample(sample2);
		assertEquals(1, jmeterReport.countErrors());
	}

	public void testJMeterReport() throws IOException, SAXException {
		JMeterReport jmeterReport = new JMeterReport(null, new File(
				"src/test/resources/JMeterResults.jtl"));
		Map<String, UriReport> uriReportMap = jmeterReport.getUriReportMap();
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
		HttpSample secondHttpSample = secondUriReport.getHttpSampleList()
				.get(0);
		assertEquals(logoutUri, secondHttpSample.getUri());
		assertEquals(26, secondHttpSample.getDuration());
		assertEquals(new Date(1219160357663L), secondHttpSample.getDate());
		assertFalse(secondHttpSample.isSuccessful());
	}

}
