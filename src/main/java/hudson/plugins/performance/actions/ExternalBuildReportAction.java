package hudson.plugins.performance.actions;


import hudson.model.Action;
import org.kohsuke.stapler.StaplerProxy;

public class ExternalBuildReportAction implements Action, StaplerProxy {
    private final String reportURL;

    public ExternalBuildReportAction(String reportURL) {
        this.reportURL = reportURL;
    }

    @Override
    public String getIconFileName() {
        return "graph.gif";
    }

    @Override
    public String getDisplayName() {
        return "View External Report";
    }

    @Override
    public String getUrlName() {
        return reportURL;
    }

    @Override
    public Object getTarget() {
        return null;
    }

}
