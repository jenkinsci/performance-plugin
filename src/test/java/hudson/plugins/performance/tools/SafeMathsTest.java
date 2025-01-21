package hudson.plugins.performance.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeMathsTest {

    @Test
    void safeDivideDividendIsNaN() {
		final double expected = Double.NaN;
		final double actual = SafeMaths.safeDivide(Double.NaN, 10);
		assertEquals(expected, actual, 0);
	}

    @Test
    void safeDivideDivisorIsNaN() {
		final double expected = Double.NaN;
		final double actual = SafeMaths.safeDivide(10, Double.NaN);
		assertEquals(expected, actual, 0);
	}

    @Test
    void safeDivideDivisorIsNullPositivePositive() {
		final double expected = Double.POSITIVE_INFINITY;
		final double actual = SafeMaths.safeDivide(10, 0);
		assertEquals(expected, actual, 0);
	}

    @Test
    void safeDivideDivisorIsNullNegativePositive() {
		final double expected = Double.NEGATIVE_INFINITY;
		final double actual = SafeMaths.safeDivide(-10, 0);
		assertEquals(expected, actual, 0);
	}

    @Test
    void safeDivideHappyPath() {
		final double expected = 2;
		final double actual = SafeMaths.safeDivide(10, 5);
		assertEquals(expected, actual, 0);
	}
}
