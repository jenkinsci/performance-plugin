package hudson.plugins.performance;

import hudson.model.ModelObject;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.IOException;
import java.util.ArrayList;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public class TrendReportGraphs implements ModelObject {

  private AbstractBuild<?, ?> build;
  private String filename;
  private PerformanceReport performanceReport;
  private AbstractProject<?, ?> project;

  public TrendReportGraphs(final AbstractProject<?, ?> project,
      final AbstractBuild<?, ?> build, final StaplerRequest request,
      String filename, PerformanceReport performanceReport) {
    this.build = build;
    this.filename = filename;
    this.performanceReport = performanceReport;
    this.project = project;
  }

  public void doRespondingTimeGraph(StaplerRequest request,
      StaplerResponse response) throws IOException {

    PerformanceReportPosition performanceReportPosition = new PerformanceReportPosition();
    request.bindParameters(performanceReportPosition);

    PerformanceBuildAction performanceBuildAction = build
        .getAction(PerformanceBuildAction.class);

    if (performanceBuildAction != null) {
      if (performanceReport != null) {
        String uri = performanceReportPosition.getSummarizerTrendUri();
        if (uri != null) {
          UriReport uriReport = performanceReport.getUriReportMap().get(uri);
          uriReport.doSummarizerTrendGraph(request, response);
        }
      }
    }
  }

  public ArrayList<String> getUris() {
    ArrayList<String> uriList = new ArrayList<String>();
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

  public AbstractProject<?, ?> getProject() {
    return project;
  }

  public AbstractBuild<?, ?> getBuild() {
    return build;
  }

  public PerformanceReport getPerformanceReport() {
    return performanceReport;
  }

}