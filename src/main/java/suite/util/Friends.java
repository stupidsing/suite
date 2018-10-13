package suite.util;

import suite.primitive.streamlet.IntStreamlet;
import suite.util.Rethrow.SourceEx;

public class Friends {

	public static double abs(double a) {
		return Math.abs(a);
	}

	public static float abs(float a) {
		return Math.abs(a);
	}

	public static int abs(int a) {
		return Math.abs(a);
	}

	public static double exp(double a) {
		return Math.exp(a);
	}

	public static double expm1(double a) {
		return Math.expm1(a);
	}

	public static <T> T fail() {
		return fail(null, null);
	}

	public static <T> T fail(String m) {
		return fail(m, null);
	}

	public static <T> T fail(Throwable th) {
		return fail(null, th);
	}

	public static <T> T fail(String m, Throwable th) {
		return Fail.t(m, th);
	}

	public static IntStreamlet forInt(int n) {
		return forInt(n);
	}

	public static IntStreamlet forInt(int s, int e) {
		return forInt(s, e);
	}

	public static double log(double a) {
		return Math.log(a);
	}

	public static double log1p(double a) {
		return Math.log1p(a);
	}

	public static double max(double a, double b) {
		return Math.max(a, b);
	}

	public static float max(float a, float b) {
		return Math.max(a, b);
	}

	public static int max(int a, int b) {
		return Math.max(a, b);
	}

	public static long max(long a, long b) {
		return Math.max(a, b);
	}

	public static double min(double a, double b) {
		return Math.min(a, b);
	}

	public static float min(float a, float b) {
		return Math.min(a, b);
	}

	public static int min(int a, int b) {
		return Math.min(a, b);
	}

	public static long min(long a, long b) {
		return Math.min(a, b);
	}

	public static <T> T rethrow(SourceEx<T, Exception> source) {
		return Rethrow.ex(source);
	}

	public static double sqrt(double a) {
		return Math.sqrt(a);
	}

}
