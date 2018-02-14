package suite.math;

import suite.util.Object_;

public class Vector {

	public static Vector origin = new Vector(0d, 0d, 0d);

	public final float x, y, z;

	public Vector(double x, double y, double z) {
		this.x = (float) x;
		this.y = (float) y;
		this.z = (float) z;
	}

	public static double abs2(Vector u) {
		return dot_(u, u);
	}

	public static Vector add(Vector u, Vector v) {
		return new Vector(u.x + v.x, u.y + v.y, u.z + v.z);
	}

	public static double dot(Vector u, Vector v) {
		return dot_(u, v);
	}

	public static Vector cross(Vector u, Vector v) {
		return new Vector(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
	}

	public static Vector neg(Vector v) {
		return new Vector(-v.x, -v.y, -v.z);
	}

	public static Vector norm(Vector v) {
		return scale_(v, 1d / Math.sqrt(abs2(v)));
	}

	public static Vector scale(Vector u, double f) {
		return scale_(u, f);
	}

	public static Vector sub(Vector u, Vector v) {
		return Vector.add(u, Vector.neg(v));
	}

	private static double dot_(Vector u, Vector v) {
		return ((double) u.x) * v.x + ((double) u.y) * v.y + ((double) u.z) * v.z;
	}

	private static Vector scale_(Vector u, double f) {
		return new Vector(u.x * f, u.y * f, u.z * f);
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Vector.class) {
			Vector vector = (Vector) object;
			return x == vector.x && y == vector.y && z == vector.z;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		int h = 7;
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
