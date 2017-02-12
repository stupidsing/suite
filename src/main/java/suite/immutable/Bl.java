package suite.immutable;

import java.util.List;
import java.util.function.BiFunction;

import suite.adt.Pair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Copy;

/**
 * Bitmap list.
 * 
 * @author ywsing
 */
public class Bl<T> {

	private static Object empty[] = new Object[0];
	private long bitmap;
	private Object ts[];

	public static <T> Streamlet<T> stream(Bl<T> bl) {
		if (bl != null)
			return Read.each(bl.ts).map(o -> {
				@SuppressWarnings("unchecked")
				T t = (T) o;
				return t;
			});
		else
			return Read.empty();
	}

	public static <T> Bl<T> meld(Bl<T> bl0, Bl<T> bl1, BiFunction<T, T, T> f) {
		if (bl0 != null) {
			if (bl1 != null) {
				long bitmap0 = bl0.bitmap;
				long bitmap1 = bl1.bitmap;
				int bitCount = 0;
				int bitCount0 = 0;
				int bitCount1 = 0;
				long bitmap = bitmap0 | bitmap1;
				Object ts[] = new Object[Long.bitCount(bitmap)];

				for (long bit = 1; bit != 0; bit <<= 1) {
					T t0 = (bitmap0 & bit) != 0 ? bl0.get(bitCount0++) : null;
					T t1 = (bitmap1 & bit) != 0 ? bl1.get(bitCount1++) : null;
					T t = t0 != null ? t1 != null ? f.apply(t0, t1) : t0 : t1;

					if (t != null)
						ts[bitCount++] = t;
				}

				return new Bl<>(bitmap, ts);
			} else
				return bl0;
		} else
			return bl1;
	}

	public static <T> Bl<T> update(Bl<T> bl, int index, T t) {
		long bitmap0;
		Object ts0[];

		if (bl != null) {
			bitmap0 = bl.bitmap;
			ts0 = bl.ts;
		} else {
			bitmap0 = 0;
			ts0 = empty;
		}

		long bit = 1l << index;
		long bits0 = bitmap0 & bit - 1;
		long bits1 = bitmap0 & 0xFFFFFFFFFFFFFFFEl << index;

		int diff0 = (bitmap0 & bit) != 0 ? 1 : 0;
		int diff1 = t != null ? 1 : 0;
		int diff = diff1 - diff0;

		long bitmap1 = bits0 + ((long) diff1 << index) + bits1;

		if (bitmap1 != 0) {
			Object ts1[] = new Object[ts0.length + diff];
			int bitCount0 = Long.bitCount(bits0);
			int bitCount1 = Long.bitCount(bits1);
			Copy.primitiveArray(ts0, 0, ts1, 0, bitCount0);
			ts1[bitCount0] = t;
			Copy.primitiveArray(ts0, bitCount0 + diff0, ts1, bitCount0 + diff1, bitCount1);

			return new Bl<>(bitmap1, ts1);
		} else
			return null;
	}

	public static <T> Bl<T> of(List<Pair<Integer, T>> list) {
		if (!list.isEmpty()) {
			int size = list.size();
			long bitmap = 0;
			Object ts[] = new Object[size];
			for (int i = 0; i < size; i++) {
				Pair<Integer, T> pair = list.get(i);
				bitmap |= 1l << (pair.t0 & 0xFFFFFC00);
				ts[i] = pair.t1;
			}
			return new Bl<T>(bitmap, ts);
		} else
			return null;
	}

	private Bl(long bitmap, Object ts[]) {
		this.bitmap = bitmap;
		this.ts = ts;
	}

	public static <T> T get(Bl<T> bl, int index) {
		long bitmap = bl != null ? bl.bitmap : 0;
		long bit = 1l << index;

		if ((bitmap & bit) != 0) {
			long mask = bit - 1;
			int bitCount = Long.bitCount(bitmap & mask);
			return bl.get(bitCount);
		} else
			return null;
	}

	private T get(int bitCount) {
		@SuppressWarnings("unchecked")
		T t = (T) ts[bitCount];
		return t;
	}

}
