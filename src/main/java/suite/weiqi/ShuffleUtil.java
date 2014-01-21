package suite.weiqi;

import java.util.List;

import suite.math.XorShiftRandom;

public class ShuffleUtil {

	private static final XorShiftRandom random = new XorShiftRandom();

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

	public static <T> void add(List<T> list, T t) {
		int size = list.size();

		if (size > 0) {

			// Quickly finds a value smaller than size
			int position = ((int) random.nextLong()) & (size - 1);
			list.add(list.get(position));
			list.set(position, t);
		} else
			list.add(t);
	}

}
