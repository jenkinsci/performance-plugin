package hudson.plugins.performance;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Parser for JMeter Throughput Report.
 *
 * @author Artem Stasiuk (artem.stasuk@gmail.com)
 */
public class JMeterThroughputParser extends JMeterParser {

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {

        @Override
        public String getDisplayName() {
            return "JMeter Throughput Report";
        }

    }

    @DataBoundConstructor
    public JMeterThroughputParser(String glob) {
        super(glob);
    }

}
