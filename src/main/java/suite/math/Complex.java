package suite.math;

public class Complex {

	private float r, i;

	public Complex(float r, float i) {
		this.r = r;
		this.i = i;
	}

	public static Complex add(Complex u, Complex v) {
		return new Complex(u.r + v.r, u.i + v.i);
	}

	public static Complex sub(Complex u, Complex v) {
		return new Complex(u.r - v.r, u.i - v.i);
	}

	public static Complex mul(Complex u, Complex v) {
		return new Complex(u.r * v.r - u.i * v.i, u.r * v.i + u.i * v.r);
	}

	public Complex inverse() {
		float f = abs2();
		return new Complex(r / f, -i / f);
	}

	public float abs2() {
		return r * r + i * i;
	}

}
