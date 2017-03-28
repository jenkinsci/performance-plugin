package hudson.plugins.performance.data;

import hudson.model.BuildListener;

/**
 * Holds the global settings for constraints.
 *
 * @author Rene Kugel
 */
public class ConstraintSettings {

    /**
     * Build listener which is used to print relevant information to the console while evaluating
     * constraints
     */
    private transient BuildListener listener;
    /**
     * If true: relative constraints won't include builds in the past with the status FAILURE into
     * the evaluation
     */
    private boolean ignoreFailedBuilds;
    /**
     * If true: relative constraints won't include builds in the past with the status UNSTABLE into
     * the evaluation
     */
    private boolean ignoreUnstableBuilds;
    /**
     * If true: the constraint log will get written into a log file
     */
    private boolean persistConstraintLog;

    public ConstraintSettings(BuildListener listener, boolean ignoreFailedBuilds, boolean ignoreUnstableBuilds, boolean persistConstraintLog) {
        this.setListener(listener);
        this.setIgnoreFailedBuilds(ignoreFailedBuilds);
        this.setIgnoreUnstableBuilds(ignoreUnstableBuilds);
        this.setPersistConstraintLog(persistConstraintLog);
    }

    public BuildListener getListener() {
        return listener;
    }

    private void setListener(BuildListener listener) {
        this.listener = listener;
    }

    public boolean isIgnoreFailedBuilds() {
        return ignoreFailedBuilds;
    }

    public void setIgnoreFailedBuilds(boolean ignoreFailedBuilds) {
        this.ignoreFailedBuilds = ignoreFailedBuilds;
    }

    public boolean isIgnoreUnstableBuilds() {
        return ignoreUnstableBuilds;
    }

    public void setIgnoreUnstableBuilds(boolean ignoreUnstableBuilds) {
        this.ignoreUnstableBuilds = ignoreUnstableBuilds;
    }

    public boolean isPersistConstraintLog() {
        return persistConstraintLog;
    }

    public void setPersistConstraintLog(boolean persistConstraintLog) {
        this.persistConstraintLog = persistConstraintLog;
    }
}
