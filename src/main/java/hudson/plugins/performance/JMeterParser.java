package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.IOException2;
import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser for JMeter.
 *
 * @author Kohsuke Kawaguchi
 */
public class JMeterParser extends PerformanceReportParser {

    private static final Logger logger = Logger.getLogger(JMeterParser.class.getName());

    @DataBoundConstructor
    public JMeterParser(String glob) {
        super(glob);
    }

    @Override
    public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build, Collection<File> reports, TaskListener listener) throws IOException {
        List<PerformanceReport> result = new ArrayList<PerformanceReport>();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);

        for (File f : reports) {
            try {
                SAXParser parser = factory.newSAXParser();
                final PerformanceReport r = new PerformanceReport();
                r.setReportFileName(f.getName());
                parser.parse(f, new DefaultHandler() {
                    private HttpSample currentSample;
                    private int status;

                    /**
                     * Performance XML log format is in http://jakarta.apache.org/jmeter/usermanual/listeners.html
                     *
                     * There are two different tags which delimit jmeter samples:
                     *    httpSample for http samples
                     *    sample     for non http samples
                     *
                     * There are also two different XML formats which we have to handle:
                     *   v2.0 = "label", "timeStamp", "time", "success"
                     *   v2.1 = "lb", "ts", "t", "s"
                     *
                     * JUnit XML format is different : tag "testcase" with attributes : "name"
                     * and "time". If there is one error, there is an other tag, "failure" in
                     * testcase tag.
                     * For exemple, Junit format is used by SOAPUI.
                     */
                    @Override
                    public void startElement(String uri, String localName, String qName,
                            Attributes attributes) throws SAXException {
                        if ("httpSample".equalsIgnoreCase(qName)
                                || "sample".equalsIgnoreCase(qName)) {
                            HttpSample sample = new HttpSample();
                            sample
                                    .setDate(new Date(
                                            Long
                                                    .valueOf(attributes.getValue("ts") != null ? attributes
                                                            .getValue("ts")
                                                            : attributes.getValue("timeStamp"))));
                            sample.setDuration(Long
                                    .valueOf(attributes.getValue("t") != null ? attributes
                                            .getValue("t") : attributes.getValue("time")));
                            sample.setSuccessful(Boolean
                                    .valueOf(attributes.getValue("s") != null ? attributes
                                            .getValue("s") : attributes.getValue("success")));
                            sample.setUri(attributes.getValue("lb") != null ? attributes
                                    .getValue("lb") : attributes.getValue("label"));
                            r.addSample(sample);
                        } else if ("testcase".equalsIgnoreCase(qName)) {
                            if (status != 0) {
                                r.addSample(currentSample);
                            }
                            status = 1;
                            currentSample = new HttpSample();
                            currentSample.setDate(new Date(0));
                            String time = attributes.getValue("time");
                            StringTokenizer st = new StringTokenizer(time, ".");
                            List<String> listTime = new ArrayList<String>(2);
                            while (st.hasMoreTokens()) {
                                listTime.add(st.nextToken());
                            }
                            currentSample.setDuration(Long.valueOf(listTime.get(0)));
                            currentSample.setSuccessful(true);
                            currentSample.setUri(attributes.getValue("name"));

                        } else if ("failure".equalsIgnoreCase(qName) && status != 0) {
                            currentSample.setSuccessful(false);
                            r.addSample(currentSample);
                            status = 0;
                        }
                    }

                    @Override
                    public void endElement(String uri, String localName, String qName)
                            throws SAXException {
                        if (("testsuite".equalsIgnoreCase(qName) || "testcase".equalsIgnoreCase(qName))
                                && status != 0) {
                            r.addSample(currentSample);
                            status = 0;
                        }
                    }
                });
                result.add(r);

            } catch (ParserConfigurationException e) {
                throw new IOException2("Failed to create parser ", e);
            } catch (SAXException e) {
                if (logger.isLoggable(Level.FINE))
                    logger.log (Level.FINE, "Failed to parse " + f, e);
                else
                    logger.warning ("Failed to parse " + f);
            }
        }

        return result;
    }

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "JMeter *.jtl files";
        }

    }
}
