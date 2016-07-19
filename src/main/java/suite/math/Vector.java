package suite.math;

import suite.util.Util;

public class Vector {

	public static Vector origin = new Vector(0f, 0f, 0f);

	public final float x, y, z;

	public Vector(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static Vector add(Vector u, Vector v) {
		return new Vector(u.x + v.x, u.y + v.y, u.z + v.z);
	}

	public static Vector sub(Vector u, Vector v) {
		return Vector.add(u, Vector.neg(v));
	}

	public static Vector mul(Vector u, float f) {
		return new Vector(u.x * f, u.y * f, u.z * f);
	}

	public static Vector neg(Vector v) {
		return new Vector(-v.x, -v.y, -v.z);
	}

	public static float dot(Vector u, Vector v) {
		return u.x * v.x + u.y * v.y + u.z * v.z;
	}

	public static float abs2(Vector u) {
		return dot(u, u);
	}

	public static Vector cross(Vector u, Vector v) {
		return new Vector(u.y * v.z - u.z * v.y, u.z * v.x - u.x * v.z, u.x * v.y - u.y * v.x);
	}

	public static Vector norm(Vector v) {
		return mul(v, 1f / (float) Math.sqrt(abs2(v)));
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Vector.class) {
			Vector vector = (Vector) object;
			return x == vector.x && y == vector.y && z == vector.z;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + Float.floatToIntBits(x);
		result = 31 * result + Float.floatToIntBits(y);
		result = 31 * result + Float.floatToIntBits(z);
		return result;
	}

	@Override
	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

}
