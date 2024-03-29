package hudson.plugins.performance.parsers;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.performance.PerformancePublisher;
import hudson.plugins.performance.descriptors.PerformanceReportParserDescriptor;
import hudson.plugins.performance.reports.PerformanceReport;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Parses performance result files into {@link PerformanceReport}s. This object
 * is persisted with {@link PerformancePublisher} into the project
 * configuration.
 * <p>
 * Subtypes can define additional parser-specific parameters as instance fields.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class PerformanceReportParser implements
        Describable<PerformanceReportParser>, ExtensionPoint {
    /**
     * GLOB patterns that specify the performance report.
     */
    public final String glob;
    public String reportURL;
    /**
     * Exclude response time of errored samples
     */
    protected boolean excludeResponseTime;
    protected boolean showTrendGraphs;
    protected int baselineBuild;

    protected PerformanceReportParser(String glob) {
        this.glob = (glob == null || glob.length() == 0) ? getDefaultGlobPattern()
                : glob;
    }

    public PerformanceReportParserDescriptor getDescriptor() {
        return (PerformanceReportParserDescriptor) Jenkins.get()
                .getDescriptorOrDie(getClass());
    }

    /**
     * Parses the specified reports into {@link PerformanceReport}s.
     */
    public abstract Collection<PerformanceReport> parse(
            Run<?, ?> build, Collection<File> reports, TaskListener listener)
            throws IOException;

    public abstract String getDefaultGlobPattern();

    /**
     * All registered implementations.
     */
    public static ExtensionList<PerformanceReportParser> all() {
        return Jenkins.get().getExtensionList(PerformanceReportParser.class);
    }

    public String getReportName() {
        return this.getClass().getName().replaceAll("^.*\\.(\\w+)Parser.*$", "$1");
    }

    public boolean isExcludeResponseTime() {
        return excludeResponseTime;
    }

    public void setExcludeResponseTime(boolean excludeResponseTime) {
        this.excludeResponseTime = excludeResponseTime;
    }

    public boolean isShowTrendGraphs() {
        return showTrendGraphs;
    }

    public void setShowTrendGraphs(boolean showTrendGraphs) {
        this.showTrendGraphs = showTrendGraphs;
    }

    public void setBaselineBuild(int baselineBuild) {
        this.baselineBuild = baselineBuild;
    }
}
