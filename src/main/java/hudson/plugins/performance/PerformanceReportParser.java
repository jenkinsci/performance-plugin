package hudson.plugins.performance;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Parses performance result files into {@link PerformanceReport}s.
 * This object is persisted with {@link PerformancePublisher} into the project configuration.
 *
 * <p>
 * Subtypes can define additional parser-specific parameters as instance fields.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class PerformanceReportParser implements Describable<PerformanceReportParser>, ExtensionPoint {
    /**
     * GLOB patterns that specify the performance report.
     */
    public final String glob;

    @DataBoundConstructor
    protected PerformanceReportParser(String glob) {
        this.glob = (glob == null || glob.length() == 0) ? getDefaultGlobPattern() : glob;
    }

    public PerformanceReportParserDescriptor getDescriptor() {
        return (PerformanceReportParserDescriptor)Hudson.getInstance().getDescriptorOrDie(getClass());
    }

    /**
     * Parses the specified reports into {@link PerformanceReport}s.
     */
    public abstract Collection<PerformanceReport> parse(AbstractBuild<?,?> build,
        Collection<File> reports, TaskListener listener) throws IOException;
    
    public abstract String getDefaultGlobPattern();

    /**
     * All registered implementations.
     */
    public static ExtensionList<PerformanceReportParser> all() {
        return Hudson.getInstance().getExtensionList(PerformanceReportParser.class);
    }
    
    public String getReportName() {
      return this.getClass().getName().replaceAll("^.*\\.(\\w+)Parser.*$", "$1");
    }
}
