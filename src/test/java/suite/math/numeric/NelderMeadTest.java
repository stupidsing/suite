package suite.math.numeric;

import java.util.Arrays;

import org.junit.Test;

import suite.math.Math_;
import suite.math.linalg.Vector;

public class NelderMeadTest {

	private NelderMead nm = new NelderMead();
	private Rosenbrock rb = new Rosenbrock();
	private Vector vec = new Vector();

	@Test
	public void test() {
		var xs = nm.nm(x -> x[0] * x[0], 1);

		System.out.println(Arrays.toString(xs));
		Math_.verifyEquals(xs[0], 0f, .05f);
	}

	// https://en.wikipedia.org/wiki/Rosenbrock_function
	@Test
	public void testRosenbrock() {
		var a = 1d;
		var b = 1d;

		// global minimum (a, a * a)
		var xs = nm.nm(rb.rosenbrock(a, b), 2);

		System.out.println(Arrays.toString(xs));
		vec.verifyEquals(xs, vec.of(a, a), .05f);
	}

}
