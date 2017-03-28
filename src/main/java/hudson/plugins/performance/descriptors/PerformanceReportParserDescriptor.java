package hudson.plugins.performance.descriptors;

import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.plugins.performance.parsers.PerformanceReportParser;
import jenkins.model.Jenkins;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class PerformanceReportParserDescriptor extends
        Descriptor<PerformanceReportParser> {

    /**
     * Internal unique ID that distinguishes a parser.
     */
    public final String getId() {
        return getClass().getName();
    }

    /**
     * Returns all the registered {@link PerformanceReportParserDescriptor}s.
     */
    public static DescriptorExtensionList<PerformanceReportParser, PerformanceReportParserDescriptor> all() {
        return Jenkins.getInstance().getDescriptorList(PerformanceReportParser.class);
    }

    public static PerformanceReportParserDescriptor getById(String id) {
        for (PerformanceReportParserDescriptor d : all())
            if (d.getId().equals(id))
                return d;
        return null;
    }
}
