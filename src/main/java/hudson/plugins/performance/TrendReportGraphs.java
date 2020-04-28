package hudson.plugins.performance;

import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.plugins.performance.actions.PerformanceBuildAction;
import hudson.plugins.performance.reports.PerformanceReport;
import hudson.plugins.performance.data.PerformanceReportPosition;
import hudson.plugins.performance.reports.UriReport;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.ArrayList;

public class TrendReportGraphs implements ModelObject {

    private Run<?, ?> build;
    private String filename;
    private PerformanceReport performanceReport;
    private Job<?, ?> project;

    public TrendReportGraphs(final Job<?, ?> project,
                             final Run<?, ?> build, final StaplerRequest request,
                             String filename, PerformanceReport performanceReport) {
        this.build = build;
        this.filename = filename;
        this.performanceReport = performanceReport;
        this.project = project;
    }

    private UriReport getUriReportForRequest(StaplerRequest request) {

        PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
        request.bindParameters(performanceReportPosition);

        PerformanceBuildAction performanceBuildAction = build
                .getAction(PerformanceBuildAction.class);

        if (performanceBuildAction != null && performanceReport != null) {
            String uri = performanceReportPosition.getSummarizerTrendUri();
            if (uri != null) {
                return performanceReport.getUriReportMap().get(uri);
            }
        }
        return null;
    }

    public void doRespondingTimeGraph(StaplerRequest request,
                                      StaplerResponse response) throws IOException {
        UriReport uriReport = getUriReportForRequest(request);
        if (uriReport != null) {
            uriReport.doSummarizerTrendGraph(request, response);
        }
    }

    public void doPercentileGraph(StaplerRequest request,
                                  StaplerResponse response) throws IOException {
        UriReport uriReport = getUriReportForRequest(request);
        if (uriReport != null) {
            uriReport.doPercentileGraph(request, response);
        }
    }

    public void doThroughputGraph(StaplerRequest request,
                                  StaplerResponse response) throws IOException {
        UriReport uriReport = getUriReportForRequest(request);
        if (uriReport != null) {
            uriReport.doThroughputGraph(request, response);
        }
    }

    public void doErrorGraph(StaplerRequest request,
                                  StaplerResponse response) throws IOException {
        UriReport uriReport = getUriReportForRequest(request);
        if (uriReport != null) {
            uriReport.doErrorGraph(request, response);
        }
    }

    public ArrayList<String> getUris() {
        ArrayList<String> uriList = new ArrayList<>();
        PerformanceReport report = getPerformanceReport();

        if (report != null) {
            uriList.addAll(report.getUriReportMap().keySet());
        }
        return uriList;
    }

    public UriReport getUriReport(String uri) {
        if (performanceReport != null) {
            return performanceReport.getUriReportMap().get(uri);
        } else {
            return build.getAction(PerformanceBuildAction.class)
                    .getPerformanceReportMap().getUriReport(uri);
        }
    }

    public String getDisplayName() {
        return Messages.TrendReportDetail_DisplayName();
    }

    public String getFilename() {
        return filename;
    }

    public Job<?, ?> getProject() {
        return project;
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    public boolean hasSamples(String uri) {
        UriReport report = getUriReport(uri);
        return report != null && report.hasSamples();
    }

    public PerformanceReport getPerformanceReport() {
        return performanceReport;
    }

}