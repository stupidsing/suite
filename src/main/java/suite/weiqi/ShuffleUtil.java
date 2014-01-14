package suite.weiqi;

import java.util.List;
import java.util.Random;

public class ShuffleUtil {

	private static final Random random = new Random();

	public static void setSeed(long seed) {
		random.setSeed(seed);
	}

	public static <T> void add(List<T> list, T t) {
		int size = list.size();

		if (size > 0) {
			int position = random.nextInt(size);
			list.add(list.get(position));
			list.set(position, t);
		} else
			list.add(t);
	}

}
