package hudson.plugins.performance.data;

public class PerformanceReportPosition {
    private String performanceReportPosition;
    private String summarizerReportType;
    private String summarizerTrendUri;

    public String getPerformanceReportPosition() {
        return performanceReportPosition;
    }

    public String getSummarizerReportType() {
        return summarizerReportType;
    }

    public String getSummarizerTrendUri() {
        return summarizerTrendUri;
    }

    public void setPerformanceReportPosition(String performanceReportPosition) {
        this.performanceReportPosition = performanceReportPosition;
    }

    public void setSummarizerReportType(String summarizerReportType) {
        this.summarizerReportType = summarizerReportType;
    }

    public void setSummarizerTrendUri(String summarizerTrendUri) {
        this.summarizerTrendUri = summarizerTrendUri;
    }

}
