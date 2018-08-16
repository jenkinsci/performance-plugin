package hudson.plugins.performance.tools;

import org.junit.Assert;
import org.junit.Test;

public class SafeMathsTest {

	@Test
	public void safeDivideDividendIsNaN() {
		final double expected = Double.NaN;
		final double actual = SafeMaths.safeDivide(Double.NaN, 10);
		Assert.assertEquals(actual, expected, 0);
	}

	@Test
	public void safeDivideDivisorIsNaN() {
		final double expected = Double.NaN;
		final double actual = SafeMaths.safeDivide(10, Double.NaN);
		Assert.assertEquals(actual, expected, 0);
	}

	@Test
	public void safeDivideDivisorIsNullPositivePositive() {
		final double expected = Double.POSITIVE_INFINITY;
		final double actual = SafeMaths.safeDivide(10, 0);
		Assert.assertEquals(actual, expected, 0);
	}

	@Test
	public void safeDivideDivisorIsNullNegativePositive() {
		final double expected = Double.NEGATIVE_INFINITY;
		final double actual = SafeMaths.safeDivide(-10, 0);
		Assert.assertEquals(actual, expected, 0);
	}

	@Test
	public void safeDivideHappyPath() {
		final double expected = 2;
		final double actual = SafeMaths.safeDivide(10, 5);
		Assert.assertEquals(actual, expected, 0);
	}
}
