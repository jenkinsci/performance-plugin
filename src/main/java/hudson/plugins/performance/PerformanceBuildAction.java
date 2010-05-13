package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.util.StreamTaskListener;
import org.apache.log4j.Logger;
import org.kohsuke.stapler.StaplerProxy;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.List;

public class PerformanceBuildAction implements Action, StaplerProxy {
	private final AbstractBuild<?, ?> build;

    /**
     * Configured parsers used to parse reports in this build.
     * For compatibility reasons, this can be null.
     */
    private final List<PerformanceReportParser> parsers;

	private transient final PrintStream hudsonConsoleWriter;

	private transient WeakReference<PerformanceReportMap> performanceReportMap;

	private static final Logger logger = Logger.getLogger(PerformanceBuildAction.class.getName());
	
	public PerformanceBuildAction(AbstractBuild<?, ?> pBuild, PrintStream logger, List<PerformanceReportParser> parsers) {
		build = pBuild;
		hudsonConsoleWriter = logger;
        this.parsers = parsers;
    }

    public PerformanceReportParser getParserById(String id) {
        if (parsers!=null)
            for (PerformanceReportParser parser : parsers)
                if (parser.getDescriptor().getId().equals(id))
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

	public AbstractBuild<?, ?> getBuild() {
		return build;
	}

	PrintStream getHudsonConsoleWriter() {
		return hudsonConsoleWriter;
	}

	public PerformanceReportMap getPerformanceReportMap() {
		PerformanceReportMap reportMap = null;
        WeakReference<PerformanceReportMap> wr = this.performanceReportMap;
        if (wr!=null) {
            reportMap = wr.get();
            if (reportMap!=null)
                return reportMap;
        }

        try {
			reportMap = new PerformanceReportMap(this, new StreamTaskListener(System.err));
		} catch (IOException e) {
			logger.error(e);
		}
        this.performanceReportMap = new WeakReference<PerformanceReportMap>(reportMap);
		return reportMap;
	}

	public void setPerformanceReportMap(WeakReference<PerformanceReportMap> performanceReportMap) {
		this.performanceReportMap = performanceReportMap;
	}
}
