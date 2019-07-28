package suite.math.numeric;

import java.util.Arrays;

import org.junit.Test;

import suite.math.Math_;
import suite.math.linalg.Vector;
import suite.primitive.DblPrimitives.Obj_Dbl;

public class BfgsTest {

	private Bfgs bfgs = new Bfgs();
	Rosenbrock rb = new Rosenbrock();
	private Vector vec = new Vector();

	@Test
	public void test() {
		Obj_Dbl<float[]> id = x -> x[0] * x[0];
		var xs = bfgs.bfgs(id, vec.of(22f));
		System.out.println(Arrays.toString(xs));
		Math_.verifyEquals(xs[0], 0f, .05f);
	}

	// https://en.wikipedia.org/wiki/Rosenbrock_function
	@Test
	public void testRosenbrock() {
		var a = 1d;
		var b = 1d;

		// global minimum (a, a * a)
		var rosenbrock = rb.rosenbrock(a, b);
		var xs = bfgs.bfgs(rosenbrock, vec.of(0f, 0f));

		System.out.println(Arrays.toString(xs));
		vec.verifyEquals(xs, vec.of(a, a), .05f);
	}

}
