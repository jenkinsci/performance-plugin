package hudson.plugins.performance.constraints;

/**
 * Holds the values of a evaluated constraint.
 *
 * @author Rene Kugel
 */
public class ConstraintEvaluation {

    private AbstractConstraint abstractConstraint;
    private double constraintValue;
    private double measuredValue;

    public ConstraintEvaluation(AbstractConstraint constraint, double result, double calculatedValue) {
        this.abstractConstraint = constraint;
        this.constraintValue = result;
        this.measuredValue = calculatedValue;
    }

    public ConstraintEvaluation() {
    }

    public double getConstraintValue() {
        return constraintValue;
    }

    public void setConstraintValue(double constraintValue) {
        this.constraintValue = constraintValue;
    }

    public double getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(double measuredValue) {
        this.measuredValue = measuredValue;
    }

    public AbstractConstraint getAbstractConstraint() {
        return abstractConstraint;
    }

    public void setAbstractConstraint(AbstractConstraint abstractConstraint) {
        this.abstractConstraint = abstractConstraint;
    }

}
