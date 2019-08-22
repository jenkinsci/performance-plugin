package hudson.plugins.performance.data;

import hudson.model.TaskListener;

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
    private transient TaskListener listener;
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

    /**
     * Relative constraints may need access to globally configured baseline build number to evaluate against
     */
    private int baselineBuild;

    public ConstraintSettings(TaskListener listener, boolean ignoreFailedBuilds, boolean ignoreUnstableBuilds, boolean persistConstraintLog,
                              int baselineBuild) {
        this.setListener(listener);
        this.setIgnoreFailedBuilds(ignoreFailedBuilds);
        this.setIgnoreUnstableBuilds(ignoreUnstableBuilds);
        this.setPersistConstraintLog(persistConstraintLog);
        this.setBaselineBuild(baselineBuild);
    }

    public TaskListener getListener() {
        return listener;
    }

    private void setListener(TaskListener listener) {
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

    public int getBaselineBuild() {
        return baselineBuild;
    }

    public void setBaselineBuild(int baselineBuild) {
        this.baselineBuild = baselineBuild;
    }
}
