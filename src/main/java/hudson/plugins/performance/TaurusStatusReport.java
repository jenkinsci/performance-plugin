package hudson.plugins.performance;

import java.io.Serializable;

/**
 *
 */
public class TaurusStatusReport implements Serializable, Comparable<TaurusStatusReport> {

    public static final String DEFAULT_TAURUS_LABEL = "SUMMARY";

    @Override
    public int compareTo(TaurusStatusReport o) {
        return 0;
    }

    private String label;
//    private int concurrency;
//    private int throughput;
    private int succ;
    private int fail;
    private double avg_rt;
//    private double avg_ct;
//    private double avg_lt;
//    private double stdev_rt;
    private long bytes;

    // TODO: rc_*** - counts for specific response codes


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
}
