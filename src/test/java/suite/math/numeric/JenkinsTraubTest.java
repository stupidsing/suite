package suite.math.numeric;

import static java.lang.Math.max;
import static suite.util.Friends.fail;

import org.junit.Test;

import suite.math.Complex;

public class JenkinsTraubTest {

	private JenkinsTraub jt = new JenkinsTraub();

	@Test
	public void test() {
		var root = jt.jt(new Complex[] { //
				Complex.of(1f, 0f), //
				Complex.of(-2f, 0f), //
				Complex.of(1f, 0f), //
		});
		verifyEquals(root, Complex.of(1f, 0f));
	}

	@Test
	public void test4() {
		var root = jt.jt(new Complex[] { //
				Complex.of(1f, 0f), //
				Complex.of(4f, 0f), //
				Complex.of(6f, 0f), //
				Complex.of(4f, 0f), //
				Complex.of(1f, 0f), //
		});
		verifyEquals(root, Complex.of(-1f, 0f));
	}

	@Test
	public void test5() {
		var root = jt.jt(new Complex[] { //
				Complex.of(1f, 0f), //
				Complex.of(5f, 0f), //
				Complex.of(10f, 0f), //
				Complex.of(10f, 0f), //
				Complex.of(5f, 0f), //
				Complex.of(1f, 0f), //
		});
		verifyEquals(root, Complex.of(-1f, 0f));
	}

	private void verifyEquals(Complex a, Complex b) {
		if (.01d * max(a.abs2(), b.abs2()) < Complex.sub(a, b).abs2())
			fail();
	}

}
