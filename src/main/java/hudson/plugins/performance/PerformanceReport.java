package hudson.plugins.performance;

import hudson.model.AbstractBuild;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.text.DecimalFormat;

/**
 * Represents a single performance report, which consists of multiple {@link UriReport}s for
 * different URLs that was tested.
 *
 * This object belongs under {@link PerformanceReportMap}.
 */
public class PerformanceReport extends AbstractReport implements
    Comparable<PerformanceReport> {

  private PerformanceBuildAction buildAction;

  private HttpSample httpSample;

  private String reportFileName = null;
 
<<<<<<< HEAD
<<<<<<< HEAD

=======
>>>>>>> caebae8b6488d0daab52b6f459245df9e77c37e9
=======
>>>>>>> 4c3caefba927728339fe854dbd593d24de773a87

  /**
   * {@link UriReport}s keyed by their {@link UriReport#getStaplerUri()}.
   */
  private final Map<String, UriReport> uriReportMap = new LinkedHashMap<String, UriReport>();
  
  private PerformanceReport lastBuildReport;

  public void addSample(HttpSample pHttpSample) throws SAXException {
    String uri = pHttpSample.getUri();
    if (uri == null) {
      buildAction.getHudsonConsoleWriter().println(
          "label cannot be empty, please ensure your jmx file specifies name properly for each http sample: skipping sample");
      return;
    }
    String staplerUri = uri.replace("http:", "").replaceAll("/", "_");
    UriReport uriReport = uriReportMap.get(staplerUri);
    if (uriReport == null) {
      uriReport = new UriReport(this, staplerUri, uri);
      uriReportMap.put(staplerUri, uriReport);
    }
    uriReport.addHttpSample(pHttpSample);
    
  }

  public int compareTo(PerformanceReport jmReport) {
    if (this == jmReport) {
      return 0;
    }
    return getReportFileName().compareTo(jmReport.getReportFileName());
  }

  public int countErrors() {
    int nbError = 0;
    for (UriReport currentReport : uriReportMap.values()) {
<<<<<<< HEAD
<<<<<<< HEAD
        nbError += currentReport.countErrors();
=======
=======
>>>>>>> 4c3caefba927728339fe854dbd593d24de773a87
        if (buildAction.getPerformanceReportMap().ifSummarizerParserUsed(reportFileName))  {
            nbError += currentReport.getHttpSampleList().get(0).getSummarizerErrors();
        } else {
            nbError += currentReport.countErrors();
        }
<<<<<<< HEAD
>>>>>>> caebae8b6488d0daab52b6f459245df9e77c37e9
=======
>>>>>>> 4c3caefba927728339fe854dbd593d24de773a87
     }
    return nbError;
  }

  public double errorPercent() {
<<<<<<< HEAD
<<<<<<< HEAD
      if (ifSummarizerParserUsed(reportFileName))  {
          float nbError=0;
          for (UriReport currentReport : uriReportMap.values()) {
              nbError+=Float.valueOf(currentReport.getSummarizerErrors());
          }
          return (double) nbError/uriReportMap.size();

      } else {
            return size() == 0 ? 0 : ((double) countErrors()) / size() * 100;
      }
=======
=======
>>>>>>> 4c3caefba927728339fe854dbd593d24de773a87
      if (buildAction.getPerformanceReportMap().ifSummarizerParserUsed(reportFileName))  {
            return size() == 0 ? 0 : ((double) countErrors()) / size();
        } else {
            return size() == 0 ? 0 : ((double) countErrors()) / size() * 100;
        }
<<<<<<< HEAD
>>>>>>> caebae8b6488d0daab52b6f459245df9e77c37e9
=======
>>>>>>> 4c3caefba927728339fe854dbd593d24de773a87
  }

  public long getAverage() {
    long result = 0;
    int size = size();
    if (size != 0) {
      long average = 0;
      for (UriReport currentReport : uriReportMap.values()) {
        average += currentReport.getAverage() * currentReport.size();
      }
      double test = average / size;
      result = (int) test;
    }
    return result;
  }

    
  public long get90Line() {
    long result = 0;
    int size = size();
    if (size != 0) {
      long average = 0;
      List<HttpSample> allSamples = new ArrayList<HttpSample>();
      for (UriReport currentReport : uriReportMap.values()) {
        allSamples.addAll(currentReport.getHttpSampleList());
      }
      Collections.sort(allSamples);
      result = allSamples.get((int) (allSamples.size() * .9)).getDuration();
    }
    return result;
  }

  public long getMedian() {
    long result = 0;
    int size = size();
    if (size != 0) {
      long average = 0;
      List<HttpSample> allSamples = new ArrayList<HttpSample>();
      for (UriReport currentReport : uriReportMap.values()) {
        allSamples.addAll(currentReport.getHttpSampleList());
      }
      Collections.sort(allSamples);
      result = allSamples.get((int) (allSamples.size() * .5)).getDuration();
    }
    return result;
  }
      
  public String getHttpCode() {
    return "";
  }

  public AbstractBuild<?, ?> getBuild() {
    return buildAction.getBuild();
  }

  PerformanceBuildAction getBuildAction() {
    return buildAction;
  }

  public String getDisplayName() {
    return Messages.Report_DisplayName();
  }

  public UriReport getDynamic(String token) throws IOException {
    return getUriReportMap().get(token);
  }

  public HttpSample getHttpSample() {
    return httpSample;
  }

  public long getMax() {
    long max = Long.MIN_VALUE;
    for (UriReport currentReport : uriReportMap.values()) {
      max = Math.max(currentReport.getMax(), max);
    }
    return max;
  }

  public long getMin() {
    long min = Long.MAX_VALUE;
    for (UriReport currentReport : uriReportMap.values()) {
      min = Math.min(currentReport.getMin(), min);
    }
    return min;
  }

  public String getReportFileName() {
    return reportFileName;
  }

  public List<UriReport> getUriListOrdered() {
    Collection<UriReport> uriCollection = getUriReportMap().values();
    List<UriReport> UriReportList = new ArrayList<UriReport>(uriCollection);
    return UriReportList;
  }

  public Map<String, UriReport> getUriReportMap() {
    return uriReportMap;
  }

  void setBuildAction(PerformanceBuildAction buildAction) {
    this.buildAction = buildAction;
  }

  public void setHttpSample(HttpSample httpSample) {
    this.httpSample = httpSample;
  }

  public void setReportFileName(String reportFileName) {
    this.reportFileName = reportFileName;
  }

  public int size() {
    int size = 0;
    for (UriReport currentReport : uriReportMap.values()) {
      size += currentReport.size();
    }
    return size;
  }
  
  public void setLastBuildReport( PerformanceReport lastBuildReport ) {
    Map<String, UriReport> lastBuildUriReportMap = lastBuildReport.getUriReportMap();
    for (Map.Entry<String, UriReport> item : uriReportMap.entrySet()) {
        UriReport lastBuildUri = lastBuildUriReportMap.get( item.getKey() );
        if ( lastBuildUri != null ) {
            item.getValue().addLastBuildUriReport( lastBuildUri );
        } else {
        }
    }
    this.lastBuildReport = lastBuildReport;
  }
  
  public long getAverageDiff() {
      if ( lastBuildReport == null ) {
          return 0;
      }
      return getAverage() - lastBuildReport.getAverage();
  }
  
  public long getMedianDiff() {
      if ( lastBuildReport == null ) {
          return 0;
      }
      return getMedian() - lastBuildReport.getMedian();
  }
  
  public double getErrorPercentDiff() {
      if ( lastBuildReport == null ) {
          return 0;
      }
      return errorPercent() - lastBuildReport.errorPercent();
  }
  
  public String getLastBuildHttpCodeIfChanged() {
      return "";
  }
    
  public int getSizeDiff() {
      if ( lastBuildReport == null ) {
          return 0;
      }
      return size() - lastBuildReport.size();
  }

    
  public boolean ifSummarizerParserUsed(String filename) {
      boolean b = false;
      String  fileExt;
      List<PerformanceReportParser> list =  buildAction.getBuild().getProject().getPublishersList().get(PerformancePublisher.class).getParsers();

      for ( int i=0; i < list.size(); i++) {
          if (list.get(i).getDescriptor().getDisplayName()=="JmeterSummarizer") {
              fileExt = list.get(i).glob;
              String parts[] = fileExt.split("\\s*[;:,]+\\s*");
              for (String path : parts) {
                  if (filename.endsWith(path.substring(5))) {
                      b=true;
                      return b;
                  }
              }
          }
      }
    return b;
  }


}
