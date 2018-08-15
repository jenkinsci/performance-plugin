package hudson.plugins.performance.parsers;

import hudson.Extension;
import hudson.plugins.performance.data.HttpSample;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Parser for JUnit.
 *
 * @author Manuel Carrasco
 */
public class JUnitParser extends AbstractParser {

    public static final String ISO8601_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    
    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "JUnit";
        }
    }

    @DataBoundConstructor
    public JUnitParser(String glob, String percentiles) {
        super(glob, percentiles);
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/TEST-*.xml";
    }

    @Override
    PerformanceReport parse(File reportFile) throws Exception {

        final SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);

        final SAXParser parser = factory.newSAXParser();
        final PerformanceReport report = new PerformanceReport(percentiles);
        report.setExcludeResponseTime(excludeResponseTime);
        report.setReportFileName(reportFile.getName());
        parser.parse(reportFile, new DefaultHandler() {
            private HttpSample currentSample;
            private int status;

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                if (("testsuite".equalsIgnoreCase(qName) || "testcase".equalsIgnoreCase(qName)) && status != 0) {
                    report.addSample(currentSample);
                    status = 0;
                }
            }

            /**
             * JUnit XML format is: tag "testcase" with attributes: "name" and
             * "time". If there is one error, there is an other tag, "failure"
             * inside testcase tag. SOAPUI uses JUnit format
             */
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                if ("testcase".equalsIgnoreCase(qName)) {
                    if (status != 0) {
                        report.addSample(currentSample);
                    }
                    status = 1;
                    currentSample = new HttpSample();
                    try {
                    	String timestamp = attributes.getValue("timestamp");
			currentSample.setDate(new SimpleDateFormat(ISO8601_DATETIME_PATTERN).parse(timestamp));
		    } catch (Exception e) {
			currentSample.setDate(new Date(0));
		    }
                    String time = attributes.getValue("time");
                    currentSample.setDuration(parseDuration(time));
                    currentSample.setSuccessful(true);
                    currentSample.setUri(attributes.getValue("classname") + "." + attributes.getValue("name"));
                    currentSample.setErrorObtained(false);
                } else if ("failure".equalsIgnoreCase(qName) && status != 0) {
                    currentSample.setErrorObtained(false);
                    currentSample.setSuccessful(false);
                    report.addSample(currentSample);
                    status = 0;
                } else if ("error".equalsIgnoreCase(qName) && status != 0) {
                    currentSample.setErrorObtained(true);
                    currentSample.setSuccessful(false);
                    report.addSample(currentSample);
                    status = 0;
                }
            }
        });
        return report;
    }

    /**
     * Strips any commas from <code>time</code>, then parses it into a long.
     */
    static long parseDuration(final String time) {
        if (StringUtils.isEmpty(time)) {
            return 0;
        } else {
            // don't want commas or else will break on parse.
            final double duration = Double.parseDouble(time.replaceAll(",", ""));
            return (long) (duration * 1000);
        }
    }
}
