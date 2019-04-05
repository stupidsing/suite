package suite.math.linalg;

import org.junit.Test;

public class FactorizationTest {

	private Factorization fac = new Factorization();
	private Matrix mtx = new Matrix();

	@Test
	public void testAls() {
		var m = mtx.identity(3);
		m[2][2] = 0f;

		var uv = fac.factor(m, 2).map(mtx::mul);
		System.out.println(mtx.toString(uv));
		mtx.verifyEquals(m, uv, .05f);
	}

	@Test
	public void testSgd() {
		var m = mtx.identity(3);
		m[2][2] = 0f;

		var uv = fac.sgd(m, 2).map(mtx::mul);
		System.out.println(mtx.toString(uv));
		mtx.verifyEquals(m, uv, .05f);
	}

}
