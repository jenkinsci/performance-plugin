package hudson.plugins.performance.constraints.blocks;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Holds the informations which builds get included into the evaluation of relative constraints.
 *
 * @author Rene Kugel
 */
public class PreviousResultsBlock extends AbstractDescribableImpl<PreviousResultsBlock> {
    /**
     * True: relative constraint includes a user defined number of builds into the evaluation False:
     * relative constraint includes all builds that have taken place in an user defined time frame
     * BASELINE: relative constraint includes baseline build defined in the PerformancePublisher settings.
     * True and false are retained for backward compatibility.
     */
    public static final String PREVIOUS = "true", TIMEFRAME = "false", BASELINE = "BASELINE";
    private String choicePreviousResults = TIMEFRAME; // keep field name for backward compatibility
    /**
     * Holds the user defined number of builds which are to include to the evaluation
     */
    private String previousResultsString;
    /**
     * Start of the time frame
     */
    private String timeframeStartString;
    /**
     * End of the time frame
     */
    private String timeframeEndString;

    @Symbol("previous")
    @Extension
    public static class DescriptorImpl extends Descriptor<PreviousResultsBlock> {
        @Override
        public String getDisplayName() {
            return "PreviousResultsBlock";
        }
    }

    @DataBoundConstructor
    public PreviousResultsBlock(String value, String previousResultsString, String timeframeStartString, String timeframeEndString) {
        this.setValue(value);
        this.previousResultsString = previousResultsString;
        this.timeframeStartString = timeframeStartString;
        this.timeframeEndString = timeframeEndString;
    }

    public boolean isChoicePreviousResults() {
        return PREVIOUS.equals(choicePreviousResults);
    }

    public void setChoicePreviousResults(boolean choicePreviousResults) {
        this.choicePreviousResults = choicePreviousResults ? PREVIOUS : TIMEFRAME; // backward compatibility
    }

    public boolean isChoiceTimeframe() {
        return TIMEFRAME.equals(choicePreviousResults);
    }

    public boolean isChoiceBaselineBuild() {
        return BASELINE.equals(choicePreviousResults);
    }

    // Workaround for radioBlock sending 'value' instead of field name (JENKINS-45988):
    public String getValue() {
        return choicePreviousResults;
    }

    public void setValue(String value) {
        this.choicePreviousResults = value;
    }

    public String getPreviousResultsString() {
        return previousResultsString;
    }

    public void setPreviousResultsString(String previousResultsString) {
        this.previousResultsString = previousResultsString;
    }

    public String getTimeframeStartString() {
        return timeframeStartString;
    }

    public void setTimeframeStartString(String timeframeStartString) {
        this.timeframeStartString = timeframeStartString;
    }

    public String getTimeframeEndString() {
        return timeframeEndString;
    }

    public void setTimeframeEndString(String timeframeEndString) {
        this.timeframeEndString = timeframeEndString;
    }

}