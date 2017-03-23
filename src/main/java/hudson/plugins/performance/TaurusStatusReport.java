package hudson.plugins.performance;

import java.io.Serializable;

public class TaurusStatusReport implements Serializable, Comparable<TaurusStatusReport> {

    public static final String DEFAULT_TAURUS_LABEL = "SUMMARY";

    @Override
    public int compareTo(TaurusStatusReport o) {
        return 0;
    }

    private String label;

    private int succ;
    private int fail;
    private double avg_rt;

    private long bytes;

    private double perc0;
    private double perc50;
    private double perc90;
    private double perc100;


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

    public double getAvg_rt() {
        return avg_rt;
    }

    public void setAvg_rt(double avg_rt) {
        this.avg_rt = avg_rt;
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
}
