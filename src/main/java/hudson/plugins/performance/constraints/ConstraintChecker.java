package hudson.plugins.performance.constraints;

import hudson.AbortException;
import hudson.model.Run;
import hudson.plugins.performance.data.ConstraintSettings;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks whether a list of constraints is fulfilled or violated
 *
 * @author Rene Kugel
 */
public class ConstraintChecker {

    /**
     * All builds that are saved in Jenkins
     */
    private List<? extends Run<?, ?>> builds;
    /**
     * Global constraint settings
     */
    private ConstraintSettings settings;

    public ConstraintChecker(ConstraintSettings settings, List<? extends Run<?, ?>> builds) {
        this.settings = settings;
        this.builds = builds;
    }

    /**
     * Evaluates a list of constraints defined by the user in the UI
     *
     * @param constraints constraints defined by the user
     * @return ArrayList of evaluated constraints
     * @throws AbortException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws ParseException
     */
    public ArrayList<ConstraintEvaluation> checkAllConstraints(List<? extends AbstractConstraint> constraints) throws AbortException, SecurityException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException, ParseException {
        ArrayList<ConstraintEvaluation> result = new ArrayList<ConstraintEvaluation>();
        for (AbstractConstraint c : constraints) {
            c.setSettings(settings);
            try {
                result.add(c.evaluate(builds));
            } catch (Exception e) {
                settings.getListener().getLogger().println(e.getMessage());
            }

        }
        return result;
    }

    public ConstraintSettings getSettings() {
        return settings;
    }

    public void setSettings(ConstraintSettings settings) {
        this.settings = settings;
    }

    public List<? extends Run<?, ?>> getBuilds() {
        return builds;
    }

    public void setBuilds(List<? extends Run<?, ?>> builds) {
        this.builds = builds;
    }

}
