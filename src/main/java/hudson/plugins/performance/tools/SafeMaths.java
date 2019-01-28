package hudson.plugins.performance.tools;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SafeMaths {
	public static double safeDivide(double dividend, double divisor) {
		if (Double.compare(divisor, Double.NaN) == 0) {
			return Double.NaN;
		}
		if (Double.compare(dividend, Double.NaN) == 0) {
			return Double.NaN;
		}
		if (Double.compare(divisor, 0.0) == 0) {
			if (Double.compare(dividend, 0.0) == -1) {
				return Double.NEGATIVE_INFINITY;
			}
			return Double.POSITIVE_INFINITY;
		}
		if (Double.compare(divisor, -0.0) == 0) {
			if (Double.compare(dividend, -0.0) == 1) {
				return Double.NEGATIVE_INFINITY;
			}
			return Double.POSITIVE_INFINITY;
		}
		return dividend / divisor;
	}
	
	public static double roundTwoDecimals(double d) {
        BigDecimal bd = BigDecimal.valueOf(d);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
