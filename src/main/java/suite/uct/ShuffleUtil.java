package suite.uct;

import java.util.List;

import suite.math.XorShiftRandom;

public class ShuffleUtil {

	private static XorShiftRandom random = new XorShiftRandom();

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

	public static <T> void add(List<T> list, T t) {
		int size = list.size();

		if (0 < size) {

			// quickly finds a value smaller than size
			int position = (int) random.nextLong() & size - 1;
			list.add(list.get(position));
			list.set(position, t);
		} else
			list.add(t);
	}

}
