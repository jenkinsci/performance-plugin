package hudson.plugins.performance.reports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.StringParameterValue;
import hudson.plugins.performance.constraints.AbsoluteConstraint;
import hudson.plugins.performance.constraints.AbstractConstraint;
import hudson.plugins.performance.constraints.ConstraintEvaluation;
import hudson.plugins.performance.constraints.RelativeConstraint;
import jenkins.model.Jenkins;

/**
 * Creates a report of the constraint evaluation and stores it into a consecutive log file, a build
 * environment variable and prints it to the Jenkins console output.
 *
 * @author Rene Kugel
 */
public class ConstraintReport {

    /**
     * Log file that is created and the log is printed to
     */
    private File performanceLog;
    /**
     * Newly created build object. Used to determine build number and build date
     */
    private Run<?, ?> newBuild;

    /**
     * Number of the build
     */
    private int buildNumber;
    /**
     * Date when the build was started
     */
    private Calendar buildDate;
    /**
     * Result of the build
     */
    private Result buildResult;
    /**
     * Link to the build with the build number
     */
    private String linkToBuild;
    /**
     * Number of all constraints in this build
     */
    private short allConstraints = 0;
    /**
     * Number of all relative constraints in this build
     */
    private short relativeConstraints = 0;
    /**
     * Number of all absolute constraints in this build
     */
    private short absoluteConstraints = 0;
    /**
     * Number of all successful constraints in this build
     */
    private short successfulConstraints = 0;
    /**
     * Number of all violated constraints in this build
     */
    private short violatedConstraints = 0;
    /**
     * Number of all violated constraints with the escalation level INFORMATION in this build
     */
    private short violatedInformation = 0;
    /**
     * Number of all violated constraints with the escalation level WARNING in this build
     */
    private short violatedUnstable = 0;
    /**
     * Number of all violated constraints with the escalation level ERROR in this build
     */
    private short violatedError = 0;
    /**
     * Logger message for console output
     */
    private String loggerMsg;
    /**
     * Logger message for log file and environment variable
     */
    private String loggerMsgAdv;
    /**
     * Buld result in JUnit report format
     */
    private String junitReport;

    public ConstraintReport(ArrayList<ConstraintEvaluation> ceList, Run<?, ?> globBuild, boolean persistConstraintLog) throws IOException {
        this.newBuild = globBuild;
        this.createMetaData(ceList, newBuild);
        this.createLoggerMsg(ceList);
        this.createLoggerMsgAdv();
        this.writeResultsToEnvVar();
        if (persistConstraintLog) {
            this.writeResultsToFile();
        }
        this.junitReport = this.createJunitReport(ceList);
    }

    /**
     * Creates the head data for the report
     *
     * @param ceList   ArrayList of evaluated constraints
     * @param newBuild Newly created build
     */
    private void createMetaData(ArrayList<ConstraintEvaluation> ceList, Run<?, ?> newBuild) {
        this.buildNumber = newBuild.getNumber();
        this.buildDate = newBuild.getTimestamp();
        this.buildResult = determineBuildResult(ceList);

		/*
         * Jenkins cannot reliable resolve it's own root URL unless it is set in the Jenkins System
		 * Configuration. This will cause that getRootUrl() returns null
		 */
        if (Jenkins.getInstance().getRootUrl() == null) {
            this.linkToBuild = "Could not resolve URL - Please set the root URL in the Jenkins System Configuration";
        } else {
            this.linkToBuild = Jenkins.getInstance().getRootUrl() + newBuild.getUrl();
        }

        for (ConstraintEvaluation ce : ceList) {
            if (ce.getAbstractConstraint() instanceof AbsoluteConstraint) {
                this.allConstraints++;
                this.absoluteConstraints++;
                if (ce.getAbstractConstraint().getSuccess()) {
                    this.successfulConstraints++;
                } else {
                    this.violatedConstraints++;
                    switch (ce.getAbstractConstraint().getEscalationLevel().ordinal()) {
                        case 0:
                            this.violatedInformation++;
                            break;
                        case 1:
                            this.violatedUnstable++;
                            break;
                        case 2:
                            this.violatedError++;
                            break;
                        default:
                            break;
                    }
                }
            } else if (ce.getAbstractConstraint() instanceof RelativeConstraint) {
                this.allConstraints++;
                this.relativeConstraints++;
                if (ce.getAbstractConstraint().getSuccess()) {
                    this.successfulConstraints++;
                } else {
                    this.violatedConstraints++;
                    switch (ce.getAbstractConstraint().getEscalationLevel().ordinal()) {
                        case 0:
                            this.violatedInformation++;
                            break;
                        case 1:
                            this.violatedUnstable++;
                            break;
                        case 2:
                            this.violatedError++;
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * Determines the build result based on the violated constraint with the highest escalation
     * level
     *
     * @param ceList ArrayList of evaluated constraints
     * @return The determined build result. The build will be marked based on this result: SUCCESS,
     * UNSTABLE, FAILURE
     */
    private Result determineBuildResult(ArrayList<ConstraintEvaluation> ceList) {
        int highestViolatedEscalation = 0;
        for (ConstraintEvaluation ce : ceList) {
            if (ce.getAbstractConstraint().getEscalationLevel().ordinal() > highestViolatedEscalation && !ce.getAbstractConstraint().getSuccess()) {
                highestViolatedEscalation = ce.getAbstractConstraint().getEscalationLevel().ordinal();
            }
        }
        switch (highestViolatedEscalation) {
            case 0:
                return Result.SUCCESS;
            case 1:
                return Result.UNSTABLE;
            case 2:
                return Result.FAILURE;
            default:
                return Result.FAILURE;
        }
    }

    /**
     * Creates the body data for the report.
     *
     * @param ceList ArrayList of evaluated constraints
     */
    private void createLoggerMsg(ArrayList<ConstraintEvaluation> ceList) {
        loggerMsg = "----------------------------------------------------------- \n";
        if (relativeConstraints == 0) {
            loggerMsg += "There are no relative constraints to evaluate! \n-------------- \n";
        } else {
            loggerMsg += "Evaluating all relative constraints! \n-------------- \n";
            for (ConstraintEvaluation ce : ceList) {
                if (ce.getAbstractConstraint() instanceof RelativeConstraint) {
                    loggerMsg += ce.getAbstractConstraint().getResultMessage() + "\n-------------- \n";
                }
            }
        }

        if (absoluteConstraints == 0) {
            loggerMsg += "There are no absolute constraints to evaluate! \n-------------- \n";
        } else {
            loggerMsg += "Evaluating all absolute constraints! \n-------------- \n";
            for (ConstraintEvaluation ce : ceList) {
                if (ce.getAbstractConstraint() instanceof AbsoluteConstraint) {
                    loggerMsg += ce.getAbstractConstraint().getResultMessage() + "\n-------------- \n";
                }
            }
        }

        if (violatedConstraints == 0) {
            loggerMsg += "There were no failing Constraints! The build will be marked as SUCCESS";
        } else if (buildResult.equals(Result.SUCCESS)) {
            loggerMsg += "The highest escalation: Information! The build will be marked as SUCCESS";
        } else if (buildResult.equals(Result.UNSTABLE)) {
            loggerMsg += "The highest escalation: Warning! The build will be marked as UNSTABLE";
        } else if (buildResult.equals(Result.FAILURE)) {
            loggerMsg += "The highest escalation: Error! The build will be marked as FAILURE";
        }

        loggerMsg += "\n";

        if (violatedConstraints == 0)
            return;

        int maxUriColumnWidth = 8; // header column widths
        int maxReportColumnWidth = 6;

        for (ConstraintEvaluation ce : ceList) {
            AbstractConstraint c = ce.getAbstractConstraint();
            maxUriColumnWidth = Math.max(c.isSpecifiedTestCase() ? c.getTestCaseBlock().getTestCase().length() : 0, maxUriColumnWidth);
            maxReportColumnWidth = Math.max(c.getRelatedPerfReport().length(), maxReportColumnWidth);
        }
        String logFormat = "%1$-"+maxReportColumnWidth+"s %2$-"+maxUriColumnWidth+"s %3$-10s %4$-20s %5$10s %6$-20s\n";

        loggerMsg += "\nSummary of failed constraints:\n"+
            String.format(logFormat, "Report", "Testcase", "Metric", "Operator", "Value", "Level");

        for (ConstraintEvaluation ce : ceList) {
            AbstractConstraint c = ce.getAbstractConstraint();
            if (!c.getSuccess()) {
                loggerMsg += String.format(logFormat,
                    c.getRelatedPerfReport(),
                    c.isSpecifiedTestCase() ? c.getTestCaseBlock().getTestCase() : AbstractConstraint.ANY,
                    c.getMeteredValue().toString(),
                    c.getOperator().toString(),
                    c instanceof RelativeConstraint ? String.format("%9.3f%%", ((RelativeConstraint)c).getTolerance()) 
                        : String.format("%10d", ((AbsoluteConstraint)c).getValue()),
                    c.getEscalationLevel().toString());
            }
        }
        loggerMsg += "\n";
    }

    /**
     * Concatenates the body and the head of the message
     */
    private void createLoggerMsgAdv() {
        loggerMsgAdv = "----------------------------------------------------------- \n" + "Build Number: #" + this.getBuildNumber() + "\n" + "Build Date: " + this.getBuildDate().getTime() + "\n"
                + "Build State: " + this.getBuildResult().toString() + "\n" + "Link to build: " + this.linkToBuild + "\n" + "-------------- \n" + "Number of all constraints: "
                + this.getAllConstraints() + "\n" + "Relative constraints: " + this.getRelativeConstraints() + "\n" + "Absolute constraints: " + this.getAbsoluteConstraints() + "\n"
                + "Successful constraints: " + this.getSuccessfulConstraints() + "\n" + "Violated constraints: " + this.getViolatedConstraints() + "\n" + "->INFORMATION: "
                + this.getViolatedInformation() + "\n" + "->UNSTABLE: " + this.getViolatedUnstable() + "\n" + "->ERROR: " + this.getViolatedError() + "\n" + "-------------- \n" + loggerMsg + "\n";
    }

    /**
     * Generate JUnit XML output format for all evaluated constraints. 
     * Depends on results from previous createMetaData call.
     */
    private String createJunitReport(ArrayList<ConstraintEvaluation> ceList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<testsuite tests=\""+getAllConstraints()+"\" failures=\""+getViolatedConstraints()+"\" >\n");
        for (ConstraintEvaluation ce : ceList) {
            AbstractConstraint c = ce.getAbstractConstraint();
            sb.append(c.getJunitResult());
        }
        sb.append("</testsuite>"); 
        return sb.toString();
    }

    /**
     * Creates the log file if not present and writes the report to the log file. Only executed if
     * persistConstraintLog == true
     *
     * @throws IOException
     * @throws InterruptedException
     */
    public void writeResultsToFile() throws IOException {
        performanceLog = new File(newBuild.getRootDir() + File.separator + "performance-results" + File.separator + "performance.log");
        if (!performanceLog.exists()) {
            performanceLog.getParentFile().mkdirs();
            if (!performanceLog.createNewFile()) {
                throw new IOException("Cannot create new file "+performanceLog.getAbsolutePath());
            }
        }
        try (FileOutputStream outWriter = new FileOutputStream(performanceLog, true)){
            outWriter.write(getLoggerMsgAdv().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Writes the complete report to the environment variable: BUILD_CONSTRAINT_LOG
     */
    public void writeResultsToEnvVar() {
        List<ParameterValue> params = new ArrayList<>();
        params.add(new StringParameterValue("BUILD_CONSTRAINT_LOG", getLoggerMsgAdv()));
        newBuild.addAction(new ParametersAction(params));
    }

    public int getBuildNumber() {
        return buildNumber;
    }

    public Calendar getBuildDate() {
        return buildDate;
    }

    public Result getBuildResult() {
        return buildResult;
    }

    public String getLinkToBuild() {
        return linkToBuild;
    }

    public short getAllConstraints() {
        return allConstraints;
    }

    public short getRelativeConstraints() {
        return relativeConstraints;
    }

    public short getAbsoluteConstraints() {
        return absoluteConstraints;
    }

    public short getSuccessfulConstraints() {
        return successfulConstraints;
    }

    public short getViolatedConstraints() {
        return violatedConstraints;
    }

    public short getViolatedInformation() {
        return violatedInformation;
    }

    public short getViolatedUnstable() {
        return violatedUnstable;
    }

    public short getViolatedError() {
        return violatedError;
    }

    public String getLoggerMsg() {
        return loggerMsg;
    }

    public String getLoggerMsgAdv() {
        return loggerMsgAdv;
    }

    public String getJunitReport() {
        return junitReport;
    }

    public File getPerformanceLog() {
        return performanceLog;
    }

    public void setPerformanceLog(File performanceLog) {
        this.performanceLog = performanceLog;
    }
}