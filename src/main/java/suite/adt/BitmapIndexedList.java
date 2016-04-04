package suite.adt;

public class BitmapIndexedList<T> {

	private int bitmap;
	private Object ts[];

	public BitmapIndexedList() {
		this(0, new Object[0]);
	}

	public BitmapIndexedList(int bitmap, Object ts[]) {
		this.bitmap = bitmap;
		this.ts = ts;
	}

	public T get(int index) {
		int bit = 1 << index;
		int mask = bit - 1;
		int index1 = Integer.bitCount(bitmap & mask);
		@SuppressWarnings("unchecked")
		T t = (T) ts[index1];
		return t;
	}

	public BitmapIndexedList<T> put(int index, T t) {
		int bit = 1 << index;
		int mask = bit - 1;
		int index1 = Integer.bitCount(bitmap & mask);
		int bitmap1 = ((bitmap & ~mask) << 1) + bit + (bitmap & mask);
		Object ts1[] = new Object[ts.length + 1];
		for (int i = 0; i < index1; i++)
			ts1[i] = ts[i];
		ts1[index1] = t;
		for (int i = index1 + 1; i < ts1.length; i++)
			ts1[i] = ts[i - 1];
		return new BitmapIndexedList<>(bitmap1, ts1);
	}

}
