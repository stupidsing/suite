package suite.uct;

import suite.math.XorShiftRandom;

import java.util.List;

public class ShuffleUtil {

	private static XorShiftRandom random = new XorShiftRandom();

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

	public static <T> void add(List<T> list, T t) {
		var size = list.size();

		if (0 < size) {

			// quickly finds a value smaller than size
			var position = (int) random.nextLong() & size - 1;
			list.add(list.get(position));
			list.set(position, t);
		} else
			list.add(t);
	}

}
