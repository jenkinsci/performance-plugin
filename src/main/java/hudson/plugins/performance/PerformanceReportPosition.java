package hudson.plugins.performance;

public class PerformanceReportPosition {
  private String performanceReportPosition;
  private String summarizerReportType;

  public String getPerformanceReportPosition() {
    return performanceReportPosition;
  }

  public String getSummarizerReportType() {
        return summarizerReportType;
  }

  public void setPerformanceReportPosition(String performanceReportPosition) {
    this.performanceReportPosition = performanceReportPosition;
  }

   public void setSummarizerReportType(String summarizerReportType) {
    this.summarizerReportType = summarizerReportType;
  }
}
