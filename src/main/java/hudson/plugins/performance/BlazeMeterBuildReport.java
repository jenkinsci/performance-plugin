package hudson.plugins.performance;


import hudson.model.Action;
import hudson.model.Run;
import hudson.util.StreamTaskListener;
import org.kohsuke.stapler.StaplerProxy;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BlazeMeterBuildReport implements Action, StaplerProxy {
    private final Run<?, ?> build;
    private final String reportURL;

//    private static final Logger logger = Logger.getLogger(PerformanceBuildAction.class.getName());
//    private transient WeakReference<PerformanceReportMap> blazeMeterBuildReportMap;


    public BlazeMeterBuildReport(Run<?, ?> build, String reportURL) {
        this.build = build;
        this.reportURL = reportURL;
    }

    @Override
    public String getIconFileName() {
        return "graph.gif";
    }

    @Override
    public String getDisplayName() {
        return "BlazeMeter Report";
    }

    @Override
    public String getUrlName() {
        return reportURL;
    }

    @Override
    public Object getTarget() {
        return null;
    }

//    public PerformanceReportMap getPerformanceReportMap() {
//        PerformanceReportMap reportMap = null;
//        WeakReference<PerformanceReportMap> wr = this.blazeMeterBuildReportMap;
//        if (wr != null) {
//            reportMap = wr.get();
//            if (reportMap != null)
//                return reportMap;
//        }
//
//        try {
//            reportMap = new PerformanceReportMap(this, StreamTaskListener.fromStderr());
//        } catch (IOException e) {
//            logger.log(Level.SEVERE, "Error creating new PerformanceReportMap()", e);
//        }
//        this.performanceReportMap = new WeakReference<PerformanceReportMap>(
//                reportMap);
//        return reportMap;
//    }
}
