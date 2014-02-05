package hudson.plugins.performance;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.util.IOException2;

import org.kohsuke.stapler.DataBoundConstructor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Parser for JMeter.
 *
 * @author Kohsuke Kawaguchi
 */
public class JMeterParser extends PerformanceReportParser {

  private static final Logger LOGGER = Logger.getLogger(JMeterParser.class.getName());
  private static final Cache<String, PerformanceReport> cache = CacheBuilder.newBuilder().maximumSize(100).build();

  @Extension
  public static class DescriptorImpl extends PerformanceReportParserDescriptor {
    @Override
    public String getDisplayName() {
      return "JMeter";
    }
  }

  @DataBoundConstructor
  public JMeterParser(String glob) {
    super(glob);
  }

  @Override
  public String getDefaultGlobPattern() {
    return "**/*.jtl";
  }

  @Override
  public Collection<PerformanceReport> parse(AbstractBuild<?, ?> build,
                                             Collection<File> reports, TaskListener listener) throws IOException {
    List<PerformanceReport> result = new ArrayList<PerformanceReport>();

    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    factory.setNamespaceAware(false);
    PrintStream logger = listener.getLogger();

    for (File f : reports) {
      try {
        String fser = f.getPath() + ".serialized";
        ObjectInputStream in = null;
        synchronized (JMeterParser.class) {
          try {
            PerformanceReport r = cache.getIfPresent(fser);
            if (r == null) {
              in = new ObjectInputStream(new FileInputStream(fser));
              r = (PerformanceReport) in.readObject();
            }
            result.add(r);
            continue;
          } catch (FileNotFoundException fne) {
            // That's OK
          } catch (Exception unknown) {
            LOGGER.warning("Deserialization failed. " + unknown);
          } finally {
            if (in != null) {
              in.close();
            }
          }
        }
        SAXParser parser = factory.newSAXParser();
        final PerformanceReport r = new PerformanceReport();
        r.setReportFileName(f.getName());
        logger.println("Performance: Parsing JMeter report file " + f.getPath());
        parser.parse(f, new DefaultHandler() {
          HttpSample currentSample;
          int counter = 0;

          /**
           * Performance XML log format is in
           * http://jakarta.apache.org
           * /jmeter/usermanual/listeners.html
           *
           * There are two different tags which delimit jmeter
           * samples: httpSample for http samples sample for non http
           * samples
           *
           * There are also two different XML formats which we have to
           * handle: v2.0 = "label", "timeStamp", "time", "success"
           * v2.1 = "lb", "ts", "t", "s"
           *
           */
          @Override
          public void startElement(String uri, String localName, String qName,
                                   Attributes attributes) throws SAXException {
            if ("httpSample".equalsIgnoreCase(qName)
              || "sample".equalsIgnoreCase(qName)) {
              HttpSample sample = new HttpSample();
              sample.setDate(new Date(
                Long.valueOf(attributes.getValue("ts") != null
                  ? attributes.getValue("ts")
                  : attributes.getValue("timeStamp"))));
              sample.setDuration(Long.valueOf(attributes.getValue("t") != null
                ? attributes.getValue("t") : attributes.getValue("time")));
              sample.setSuccessful(Boolean.valueOf(attributes.getValue("s") != null
                ? attributes.getValue("s") : attributes.getValue("success")));
              sample.setUri(attributes.getValue("lb") != null
                ? attributes.getValue("lb") : attributes.getValue("label"));
              sample.setHttpCode(attributes.getValue("rc") != null && attributes.getValue("rc").length() <= 3
                ? attributes.getValue("rc") : "0");
              sample.setSizeInKb(attributes.getValue("by") != null ? Double.valueOf(attributes.getValue("by")) / 1024d : 0d);
              if (counter == 0) {
                currentSample = sample;
              }
              counter++;
            }
          }

          @Override
          public void endElement(String uri, String localName, String qName) {
            if ("httpSample".equalsIgnoreCase(qName)
              || "sample".equalsIgnoreCase(qName)) {
              if (counter == 1) {
                try {
                  r.addSample(currentSample);
                } catch (SAXException e) {
                  e.printStackTrace();
                }
              }
              counter--;
            }
          }
        });
        result.add(r);
        ObjectOutputStream out = null;
        synchronized (JMeterParser.class) {
          try {
            cache.put(fser, r);
            out = new ObjectOutputStream(new FileOutputStream(fser));
            out.writeObject(r);
          } catch (Exception unknown) {
            LOGGER.warning("Serialization failed. " + unknown);
          } finally {
            if (out != null) {
              out.close();
            }
          }
        }

      } catch (ParserConfigurationException e) {
        throw new IOException2("Failed to create parser ", e);
      } catch (SAXException e) {
        logger.println("Performance: Failed to parse " + f + ": "
          + e.getMessage());
      }
    }
    return result;
  }
}
