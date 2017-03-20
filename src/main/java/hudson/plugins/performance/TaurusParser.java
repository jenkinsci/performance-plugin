package hudson.plugins.performance;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;

/**
 * Parser for Taurus
 */
public class TaurusParser extends AbstractParser {

    @Extension
    public static class DescriptorImpl extends PerformanceReportParserDescriptor {
        @Override
        public String getDisplayName() {
            return "Taurus";
        }
    }

    @DataBoundConstructor
    public TaurusParser(String glob, String logDateFormat) {
        super(glob);
    }

    @Override
    public String getDefaultGlobPattern() {
        return "**/*.xml";
    }

    @Override
    PerformanceReport parse(File reportFile) throws Exception {
        return null;
    }
}
