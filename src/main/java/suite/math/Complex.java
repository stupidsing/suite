package suite.math;

import suite.util.Util;

public class Complex {

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

	public Complex inverse() {
		float f = abs2();
		return of(r / f, -i / f);
	}

	public static Complex of(float r, float i) {
		return new Complex(r, i);
	}

	private Complex(float r, float i) {
		this.r = r;
		this.i = i;
	}

	public float abs2() {
		return r * r + i * i;
	}

	@Override
	public String toString() {
		return r + " + " + i + " i";
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Complex.class) {
			Complex other = (Complex) object;
			return r == other.r && i == other.i;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Float.hashCode(r) * 31 + Float.hashCode(i);
	}

}
