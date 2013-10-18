package hudson.plugins.performance;

import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.util.ChartUtil;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.*;
import java.text.DecimalFormat;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * A report about a particular tested URI.
 * 
 * This object belongs under {@link PerformanceReport}.
 */
public class UriReport extends AbstractReport implements  Serializable, ModelObject,
    Comparable<UriReport> {

  public final static String END_PERFORMANCE_PARAMETER = ".endperformanceparameter";

  /**
   * Individual HTTP invocations to this URI and how they went.
   */
  private final List<HttpSample> httpSampleList = new ArrayList<HttpSample>();

  /**
   * The parent object to which this object belongs.
   */
  private final PerformanceReport performanceReport;

  /**
   * Escaped {@link #uri} that doesn't contain any letters that cannot be used
   * as a token in URL.
   */
  private final String staplerUri;
  
  private UriReport lastBuildUriReport;

  private String uri;

  UriReport(PerformanceReport performanceReport, String staplerUri, String uri) {
    this.performanceReport = performanceReport;
    this.staplerUri = staplerUri;
    this.uri = uri;
  }

  public void addHttpSample(HttpSample httpSample) {
    httpSampleList.add(httpSample);
  }

  public int compareTo(UriReport uriReport) {
    if (uriReport == this) {
      return 0;
    }
    return uriReport.getUri().compareTo(this.getUri());
  }

  public int countErrors() {
    int nbError = 0;
    for (HttpSample currentSample : httpSampleList) {
      if (!currentSample.isSuccessful()) {
        nbError++;
      }
    }
    return nbError;
  }

  public double errorPercent() {
    return ((double) countErrors()) / size() * 100;
  }

  public long getAverage() {
    long average = 0;
    for (HttpSample currentSample : httpSampleList) {
      average += currentSample.getDuration();
    }
    return average / size();
  }
  
  public double getAverageSizeInKb(){ 
	  double average = 0 ; 
	  for (HttpSample currentSample : httpSampleList) {
	      average += currentSample.getSizeInKb();
	    }
	    return roundTwoDecimals(average / size());
  }

  public long get90Line() {
    long result = 0;
    Collections.sort(httpSampleList);
    if (httpSampleList.size() > 0) {
      result = httpSampleList.get((int) (httpSampleList.size() * .9)).getDuration();
    }
    return result;
  }
  
  public String getHttpCode() {
    String result = "";
    
    for (HttpSample currentSample : httpSampleList) {
      if ( !result.matches( ".*"+currentSample.getHttpCode()+".*" ) ) {
          result += ( result.length() > 1 ) ? ","+currentSample.getHttpCode() : currentSample.getHttpCode();
      }
    }
    
    return result;
  }

  public long getMedian() {
    long result = 0;
    Collections.sort(httpSampleList);
    if (httpSampleList.size() > 0) {
      result = httpSampleList.get((int) (httpSampleList.size() * .5)).getDuration();
    }
    return result;
  }

  public AbstractBuild<?, ?> getBuild() {
    return performanceReport.getBuild();
  }

  public String getDisplayName() {
    return getUri();
  }

  public List<HttpSample> getHttpSampleList() {
    return httpSampleList;
  }

  public PerformanceReport getPerformanceReport() {
    return performanceReport;
  }

  public long getMax() {
    long max = Long.MIN_VALUE;
    for (HttpSample currentSample : httpSampleList) {
      max = Math.max(max, currentSample.getDuration());
    }
    return max;
  }
  
  public double getTotalTrafficInKb(){ 
	  double traffic = 0 ; 
	  for (HttpSample currentSample : httpSampleList) {
		  traffic += currentSample.getSizeInKb();
	    }
	    return roundTwoDecimals(traffic);
  }

  public long getMin() {
    long min = Long.MAX_VALUE;
    for (HttpSample currentSample : httpSampleList) {
      min = Math.min(min, currentSample.getDuration());
    }
    return min;
  }

  public String getStaplerUri() {
    return staplerUri;
  }

  public String getUri() {
    return uri;
  }

  public String getShortUri() {
    if ( uri.length() > 130 ) {
        return uri.substring( 0, 129 );
    }
    return uri;
  }
  
  public boolean isFailed() {
    return countErrors() != 0;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public int size() {
    return httpSampleList.size();
  }

  public String encodeUriReport() throws UnsupportedEncodingException {
    StringBuilder sb = new StringBuilder(120);
    sb.append(performanceReport.getReportFileName()).append(
        GraphConfigurationDetail.SEPARATOR).append(getStaplerUri()).append(
        END_PERFORMANCE_PARAMETER);
    return URLEncoder.encode(sb.toString(), "UTF-8");
  }

  public void addLastBuildUriReport( UriReport lastBuildUriReport ) {
      this.lastBuildUriReport = lastBuildUriReport;
  }
  
  public long getAverageDiff() {
      if ( lastBuildUriReport == null ) {
          return 0;
      }
      return getAverage() - lastBuildUriReport.getAverage();
  }
  
  public long getMedianDiff() {
      if ( lastBuildUriReport == null ) {
          return 0;
      }
      return getMedian() - lastBuildUriReport.getMedian();
  }
  
  public double getErrorPercentDiff() {
      if ( lastBuildUriReport == null ) {
          return 0;
      }
      return errorPercent() - lastBuildUriReport.errorPercent();
  }
  
  public String getLastBuildHttpCodeIfChanged() {
      if ( lastBuildUriReport == null ) {
          return "";
      }
      
      if ( lastBuildUriReport.getHttpCode().equals(getHttpCode()) ) {
          return "";
      }
      
      return lastBuildUriReport.getHttpCode();
  }
  
  public int getSizeDiff() {
      if ( lastBuildUriReport == null ) {
          return 0;
      }
      return size() - lastBuildUriReport.size();
  }

  public long getSummarizerMax() {
    long max =  Long.MIN_VALUE;
    for (HttpSample currentSample : httpSampleList) {
        max = Math.max(max, currentSample.getSummarizerMax());
    }
    return max;
  }

  public long getSummarizerMin() {
    long min = Long.MAX_VALUE;
    for (HttpSample currentSample : httpSampleList) {
        min = Math.min(min, currentSample.getSummarizerMin());
    }
    return min;
  }

  public long getSummarizerSize() {
    long size=0;
    for (HttpSample currentSample : httpSampleList) {
        size+=currentSample.getSummarizerSamples();
    }
    return size;
  }

  public String getSummarizerErrors() {
    float nbError = 0;
    for (HttpSample currentSample : httpSampleList) {
        nbError+=currentSample.getSummarizerErrors();
    }
    return new DecimalFormat("#.##").format(nbError/getSummarizerSize()*100).replace(",", ".");     
  }


    public void doSummarizerTrendGraph(StaplerRequest request,
                                StaplerResponse response) throws IOException{

         ArrayList<XYDataset> dataset = new ArrayList<XYDataset> ();
         TimeSeriesCollection resp = new TimeSeriesCollection();
        // TimeSeriesCollection err  = new TimeSeriesCollection();
         TimeSeries responseTime = new TimeSeries("Response Time", FixedMillisecond.class);
        // TimeSeries errors = new TimeSeries("errors", Minute.class);
         
         for (int i=0; i<=this.httpSampleList.size()-1; i++) {
             RegularTimePeriod current = new FixedMillisecond(this.httpSampleList.get(i).getDate());
             responseTime.addOrUpdate(current,this.httpSampleList.get(i).getDuration());
             //errors.addOrUpdate(current,report.getHttpSampleList().get(i).getSummarizerErrors());
         }

       resp.addSeries(responseTime);
      // err.addSeries(errors);
       dataset.add(resp);
      // dataset.add(err);

            ChartUtil.generateGraph(request, response,
                                PerformanceProjectAction.createSummarizerTrend(dataset, uri),400, 200);
     
    }

    private double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
  	  return Double.valueOf(twoDForm.format(d));
  	}

}
