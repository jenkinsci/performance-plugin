package hudson.plugins.performance.actions;

import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.performance.Messages;
import hudson.plugins.performance.PerformanceReportMap;
import hudson.plugins.performance.parsers.PerformanceReportParser;
import hudson.util.StreamTaskListener;
import org.kohsuke.stapler.StaplerProxy;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PerformanceBuildAction implements Action, StaplerProxy {
    private final Run<?, ?> build;

    /**
     * Configured parsers used to parse reports in this build.
     * For compatibility reasons, this can be null.
     */
    private final List<PerformanceReportParser> parsers;

    private transient final PrintStream hudsonConsoleWriter;

    private transient WeakReference<PerformanceReportMap> performanceReportMap;

    private static final Logger logger = Logger.getLogger(PerformanceBuildAction.class.getName());


    public PerformanceBuildAction(Run<?, ?> pBuild, PrintStream logger,
                                  List<PerformanceReportParser> parsers) {
        build = pBuild;
        hudsonConsoleWriter = logger;
        this.parsers = parsers;
    }

    public PerformanceReportParser getParserByDisplayName(String displayName) {
        if (parsers != null)
            for (PerformanceReportParser parser : parsers)
                if (parser.getDescriptor().getDisplayName().equals(displayName))
                    return parser;
        return null;
    }

    public String getDisplayName() {
        return Messages.BuildAction_DisplayName();
    }

    public String getIconFileName() {
        return "graph.gif";
    }

    public String getUrlName() {
        return "performance";
    }

    public PerformanceReportMap getTarget() {
        return getPerformanceReportMap();
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public PrintStream getHudsonConsoleWriter() {
        return hudsonConsoleWriter;
    }

    public PerformanceReportMap getPerformanceReportMap() {
        PerformanceReportMap reportMap = null;
        WeakReference<PerformanceReportMap> wr = this.performanceReportMap;
        if (wr != null) {
            reportMap = wr.get();
            if (reportMap != null)
                return reportMap;
        }

        try {
            reportMap = new PerformanceReportMap(this, StreamTaskListener.fromStderr());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error creating new PerformanceReportMap()", e);
        }
        this.performanceReportMap = new WeakReference<PerformanceReportMap>(
                reportMap);
        return reportMap;
    }

    public void setPerformanceReportMap(WeakReference<PerformanceReportMap> performanceReportMap) {
        this.performanceReportMap = performanceReportMap;
    }
}
