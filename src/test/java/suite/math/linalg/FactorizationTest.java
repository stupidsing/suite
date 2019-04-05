package suite.math.linalg;

import org.junit.Test;

public class FactorizationTest {

	private Factorization fac = new Factorization();
	private Matrix mtx = new Matrix();

	@Test
	public void test() {
		var m = mtx.identity(3);
		m[2][2] = 0f;

		var uv = fac.factor(m, 2).map((u, v) -> {
			System.out.println(mtx.toString(u));
			System.out.println(mtx.toString(v));
			return mtx.mul(u, v);
		});

		System.out.println(mtx.toString(uv));
		mtx.verifyEquals(m, uv, .05f);
	}

}
