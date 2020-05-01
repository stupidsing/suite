package suite.math;

import primal.Verbs.Get;

import static java.lang.Math.sqrt;

public class R3 {

	public static R3 origin = new R3(0d, 0d, 0d);

	public final float x, y, z;

	public R3(double x, double y, double z) {
		this.x = (float) x;
		this.y = (float) y;
		this.z = (float) z;
	}

	public static R3 add(R3 u, R3 v) {
		return new R3(u.x + v.x, u.y + v.y, u.z + v.z);
	}

	public static double dot(R3 u, R3 v) {
		return dot_(u, v);
	}

	public static R3 cross(R3 u, R3 v) {
		return new R3(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
	}

	public static R3 sub(R3 u, R3 v) {
		return R3.add(u, v.neg_());
	}

	private static double dot_(R3 u) {
		return dot_(u, u);
	}

	private static double dot_(R3 u, R3 v) {
		return ((double) u.x) * v.x + ((double) u.y) * v.y + ((double) u.z) * v.z;
	}

	public double abs2() {
		return abs2_();
	}

	public double mag() {
		return mag_();
	}

	public R3 neg() {
		return neg_();
	}

	public R3 norm() {
		return scale_(1d / mag_());
	}

	public R3 scale(double f) {
		return scale_(f);
	}

	private double mag_() {
		return sqrt(abs2_());
	}

	private double abs2_() {
		return dot_(this);
	}

	private R3 neg_() {
		return new R3(-x, -y, -z);
	}

	private R3 scale_(double f) {
		return new R3(x * f, y * f, z * f);
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == R3.class) {
			var vector = (R3) object;
			return x == vector.x && y == vector.y && z == vector.z;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		var h = 7;
		h = h * 31 + Float.floatToIntBits(x);
		h = h * 31 + Float.floatToIntBits(y);
		h = h * 31 + Float.floatToIntBits(z);
		return h;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

}
