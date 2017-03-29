package hudson.plugins.performance.constraints.blocks;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Holds the informations which builds get included into the evaluation of relative constraints.
 *
 * @author Rene Kugel
 */
public class PreviousResultsBlock extends AbstractDescribableImpl<PreviousResultsBlock> {

    @Extension
    public static class DescriptorImpl extends Descriptor<PreviousResultsBlock> {
        @Override
        public String getDisplayName() {
            return "PreviousResultsBlock";
        }
    }

    /**
     * True: relative constraint includes a user defined number of builds into the evaluation False:
     * relative constraint includes all builds that have taken place in an user defined time frame
     */
    private boolean choicePreviousResults;
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

    @DataBoundConstructor
    public PreviousResultsBlock(String value, String previousResultsString, String timeframeStartString, String timeframeEndString) {
        this.setChoicePreviousResults(Boolean.parseBoolean(value));
        this.previousResultsString = previousResultsString;
        this.timeframeStartString = timeframeStartString;
        this.timeframeEndString = timeframeEndString;
    }

    public boolean isChoicePreviousResults() {
        return choicePreviousResults;
    }

    public void setChoicePreviousResults(boolean choicePreviousResults) {
        this.choicePreviousResults = choicePreviousResults;
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