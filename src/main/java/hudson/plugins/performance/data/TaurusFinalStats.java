package hudson.plugins.performance.data;

import java.io.Serializable;

public class TaurusFinalStats implements Serializable {

    public static final String DEFAULT_TAURUS_LABEL = "SUMMARY";

    private String label;

    private int succ;
    private int fail;
    private long bytes;

    private double averageResponseTime;

    private double perc0;
    private double perc50;
    private double perc90;
    private double perc100;

    private long throughput;


    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getSucc() {
        return succ;
    }

    public void setSucc(int succ) {
        this.succ = succ;
    }

    public int getFail() {
        return fail;
    }

    public void setFail(int fail) {
        this.fail = fail;
    }

    public double getAverageResponseTime() {
        return averageResponseTime;
    }

    public void setAverageResponseTime(double averageResponseTime) {
        this.averageResponseTime = averageResponseTime;
    }

    public long getBytes() {
        return bytes;
    }

    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    public double getPerc50() {
        return perc50;
    }

    public void setPerc50(double perc50) {
        this.perc50 = perc50;
    }

    public double getPerc90() {
        return perc90;
    }

    public void setPerc90(double perc90) {
        this.perc90 = perc90;
    }

    public double getPerc0() {
        return perc0;
    }

    public void setPerc0(double perc0) {
        this.perc0 = perc0;
    }

    public double getPerc100() {
        return perc100;
    }

    public void setPerc100(double perc100) {
        this.perc100 = perc100;
    }

    public long getThroughput() {
        return throughput;
    }

    public void setThroughput(long throughput) {
        this.throughput = throughput;
    }
}
