package suite.immutable;

import java.util.function.BiFunction;

/**
 * Bitmap list.
 * 
 * @author ywsing
 */
public class Bl<T> {

	private static Object empty[] = new Object[0];
	private int bitmap;
	private Object ts[];

	public static <T> Bl<T> merge(Bl<T> bl0, Bl<T> bl1, BiFunction<T, T, T> merger) {
		if (bl0 != null) {
			if (bl1 != null) {
				int bitmap0 = bl0.bitmap;
				int bitmap1 = bl1.bitmap;
				int bitCount = 0;
				int bitCount0 = 0;
				int bitCount1 = 0;
				int bitmap = 0;
				Object ts[] = new Object[Integer.bitCount(bitmap0 | bitmap1)];

				for (int bit = 1; bit != 0; bit <<= 1) {
					T t0 = (bitmap0 & bit) != 0 ? bl0.get(bitCount0++) : null;
					T t1 = (bitmap1 & bit) != 0 ? bl1.get(bitCount1++) : null;
					T t = t0 != null ? t1 != null ? merger.apply(t0, t1) : t0 : t1;

					if (t != null) {
						bitmap |= bit;
						ts[bitCount++] = t;
					}
				}

				return new Bl<>(bitmap, ts);
			} else
				return bl0;
		} else
			return bl1;
	}

	public static <T> Bl<T> add(Bl<T> bl, int index, T t) {
		int bitmap0;
		Object ts0[];

		if (bl != null) {
			bitmap0 = bl.bitmap;
			ts0 = bl.ts;
		} else {
			bitmap0 = 0;
			ts0 = empty;
		}

		int bit = 1 << index;
		int mask = bit - 1;
		int bits0 = bitmap0 & mask;
		int bits1 = bitmap0 & ~mask;
		int bitCount = Integer.bitCount(bits0);
		int bitmap1 = (bits1 << 1) + bit + bits0;
		Object ts1[] = new Object[ts0.length + 1];
		for (int i = 0; i < bitCount; i++)
			ts1[i] = ts0[i];
		ts1[bitCount] = t;
		for (int i = bitCount + 1; i < ts1.length; i++)
			ts1[i] = ts0[i - 1];

		return new Bl<>(bitmap1, ts1);
	}

	public Bl(int bitmap, Object ts[]) {
		this.bitmap = bitmap;
		this.ts = ts;
	}

	public static <T> T get(Bl<T> bl, int index) {
		int bitmap = bl != null ? bl.bitmap : 0;
		int bit = 1 << index;

		if ((bitmap & bit) != 0) {
			int mask = bit - 1;
			int bitCount = Integer.bitCount(bitmap & mask);
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
