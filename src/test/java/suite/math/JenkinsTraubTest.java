package suite.math;

import org.junit.Test;

public class JenkinsTraubTest {

	@Test
	public void test() {
		Complex root = new JenkinsTraub().jt(new Complex[] { //
				Complex.of(1f, 0f), //
				Complex.of(-2f, 0f), //
				Complex.of(1f, 0f), //
		});
		verifyEquals(root, Complex.of(1f, 0f));
	}

	@Test
	public void test4() {
		Complex root = new JenkinsTraub().jt(new Complex[] { //
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
		Complex root = new JenkinsTraub().jt(new Complex[] { //
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
		if (.01d * Math.max(a.abs2(), b.abs2()) < Complex.sub(a, b).abs2())
			throw new RuntimeException();
	}

}
