package suite.math;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import primal.Verbs.Get;

public class Complex {

	public static final Complex zero = Complex.of(0f, 0f);

	public final float r, i;

	public static Complex add(Complex u, Complex v) {
		return of(u.r + v.r, u.i + v.i);
	}

	public static Complex sub(Complex u, Complex v) {
		return of(u.r - v.r, u.i - v.i);
	}

	public static Complex mul(Complex u, Complex v) {
		return of(u.r * v.r - u.i * v.i, u.r * v.i + u.i * v.r);
	}

	public static Complex expi(double e) {
		return new Complex(cos(e), sin(e));
	}

	public static Complex of(float r, float i) {
		return new Complex(r, i);
	}

	public static void verifyEquals(Complex u, Complex v) {
		Math_.verifyEquals(u.r, v.r);
		Math_.verifyEquals(u.i, v.i);
	}

	private Complex(double r, double i) {
		this.r = (float) r;
		this.i = (float) i;
	}

	public double abs2() {
		return abs2_();
	}

	public Complex conjugate() {
		return Complex.of(r, -i);
	}

	public Complex inverse() {
		var iabs2 = 1d / abs2_();
		return new Complex(r * iabs2, -i * iabs2);
	}

	public Complex scale(double v) {
		return new Complex(r * v, i * v);
	}

	private double abs2_() {
		var r_ = r;
		var i_ = i;
		return r_ * r_ + i_ * i_;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == Complex.class) {
			var other = (Complex) object;
			return r == other.r && i == other.i;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(r) * 31 + Float.hashCode(i);
	}

	@Override
	public String toString() {
		return r + " + " + i + " i";
	}

}
