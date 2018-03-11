package suite.math.numeric;

import java.util.Arrays;

import org.junit.Test;

import suite.primitive.DblPrimitives.Obj_Dbl;

public class BfgsTest {

	private Bfgs bfgs = new Bfgs();

	@Test
	public void test() {
		Obj_Dbl<float[]> id = x -> x[0];

		float[] xs = bfgs.bfgs(id, new float[] { 0f });

		System.out.println(Arrays.toString(xs));
	}

	// https://en.wikipedia.org/wiki/Rosenbrock_function
	@Test
	public void testRosenbrock() {
		double a = 1d;
		double b = 1d;

		// global minimum (a, a * a)
		Obj_Dbl<float[]> rosenbrock = xy -> {
			double x = xy[0];
			double y = xy[1];
			double a_x = a - x;
			double y_x = y - x;
			return a_x * a_x + b * y_x * y_x;
		};

		float[] xs = bfgs.bfgs(rosenbrock, new float[] { 0f, 0f });

		System.out.println(Arrays.toString(xs));
	}

}
