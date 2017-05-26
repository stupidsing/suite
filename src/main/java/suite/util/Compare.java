package suite.util;

public class Compare {

	public static int compare(byte a, byte b) {
		return Integer.compare(Byte.toUnsignedInt(a), Byte.toUnsignedInt(b));
	}

	public static int compare(char a, char b) {
		return Character.compare(a, b);
	}

	public static int compare(double a, double b) {
		return Double.compare(a, b);
	}

	public static int compare(float a, float b) {
		return Float.compare(a, b);
	}

	public static int compare(int a, int b) {
		return Integer.compare(a, b);
	}

	public static int compare(short a, short b) {
		return Short.compare(a, b);
	}

}
