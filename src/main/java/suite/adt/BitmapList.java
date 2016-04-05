package suite.adt;

public class BitmapList<T> {

	private static Object empty[] = new Object[0];
	private int bitmap;
	private Object ts[];

	public BitmapList() {
		this(0, new Object[0]);
	}

	public BitmapList(BitmapList<T> bl, int index, T t) {
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
		this.bitmap = bitmap1;
		this.ts = ts1;
	}

	public BitmapList(int bitmap, Object ts[]) {
		this.bitmap = bitmap;
		this.ts = ts;
	}

	public static <T> T get(BitmapList<T> bl, int index) {
		int bitmap;
		Object ts[];

		if (bl != null) {
			bitmap = bl.bitmap;
			ts = bl.ts;
		} else {
			bitmap = 0;
			ts = empty;
		}

		int bit = 1 << index;

		if ((bitmap & bit) != 0) {
			int mask = bit - 1;
			int bitCount = Integer.bitCount(bitmap & mask);
			@SuppressWarnings("unchecked")
			T t = (T) ts[bitCount];
			return t;
		} else
			return null;
	}

}
