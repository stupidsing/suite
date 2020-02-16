package suite.math.linalg;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class GaussSeidelTest {

	private GaussSeidel gs = new GaussSeidel();
	private Vector vec = new Vector();

	@Test
	public void test() {
		float[][] a = { //
				{ 10f, -1f, 2f, 0f, }, //
				{ -1f, 11f, -1f, 3f, }, //
				{ 2f, -1f, 10f, -1f, }, //
				{ 0f, 3f, -1f, 8f, }, //
		};

		var actual = gs.solve(a, vec.of(6f, 25f, -11f, 15f));
		var expect = vec.of(1f, 2f, -1f, 1f);
		System.out.println(Arrays.toString(actual));
		vec.verifyEquals(actual, expect);
	}

}
