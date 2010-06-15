package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.IOException2;

import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parser for JUnit.
 *
 * @author Manuel Carrasco
 */
public class JUnitParser extends JMeterParser {

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "JUnit";
        }
    }

    @DataBoundConstructor
    public JUnitParser(String glob) {
        super(glob);
    }

    @Override
    public String getDefaultGlobPattern() {
      return "**/TEST-*.xml";
    }
    
    @Override
    public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build, Collection<File> reports, TaskListener listener) throws IOException {
        List<PerformanceReport> result = new ArrayList<PerformanceReport>();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        PrintStream logger = listener.getLogger();

        for (File f : reports) {
            try {
                SAXParser parser = factory.newSAXParser();
                final PerformanceReport r = new PerformanceReport();
                r.setReportFileName(f.getName());
                logger.println("Performance: Parsing JUnit report file " + f.getName());
                parser.parse(f, new DefaultHandler() {
                    private HttpSample currentSample;
                    private int status;

                    @Override
                    public void endElement(String uri, String localName, String qName)
                            throws SAXException {
                        if (("testsuite".equalsIgnoreCase(qName) || "testcase".equalsIgnoreCase(qName))
                                && status != 0) {
                            r.addSample(currentSample);
                            status = 0;
                        }
                    }

                    /**
                     * JUnit XML format is: tag "testcase" with attributes: "name" and "time". 
                     * If there is one error, there is an other tag, "failure" inside testcase tag.
                     * SOAPUI uses JUnit format
                     */
                    @Override
                    public void startElement(String uri, String localName, String qName,
                            Attributes attributes) throws SAXException {
                        if ("testcase".equalsIgnoreCase(qName)) {
                            if (status != 0) {
                                r.addSample(currentSample);
                            }
                            status = 1;
                            currentSample = new HttpSample();
                            currentSample.setDate(new Date(0));
                            String time = attributes.getValue("time");
                            double duration = Double.parseDouble(time);
                            currentSample.setDuration((long)(duration * 1000));
                            currentSample.setSuccessful(true);
                            currentSample.setUri(attributes.getValue("name"));
                        } else if ("failure".equalsIgnoreCase(qName) && status != 0) {
                            currentSample.setSuccessful(false);
                            r.addSample(currentSample);
                            status = 0;
                        }
                    }
                });
                result.add(r);
            } catch (ParserConfigurationException e) {
                throw new IOException2("Failed to create parser ", e);
            } catch (SAXException e) {
                logger.println("Performance: Failed to parse " + f + ": " + e.getMessage());
            }
        }

        return result;
    }
}
