package suite.persistent;

import java.util.List;

import primal.fp.Funs2.BinOp;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Array_;

/**
 * Bitmap list.
 * 
 * @author ywsing
 */
public class Bl<T> {

	private static Object[] empty = new Object[0];
	private long bitmap;
	private Object[] ts;

	public static <T> Streamlet<T> stream(Bl<T> bl) {
		if (bl != null)
			return Read.from(bl.ts).map(o -> {
				@SuppressWarnings("unchecked")
				var t = (T) o;
				return t;
			});
		else
			return Read.empty();
	}

	public static <T> Bl<T> meld(Bl<T> bl0, Bl<T> bl1, BinOp<T> f) {
		if (bl0 != null) {
			if (bl1 != null) {
				var bitmap0 = bl0.bitmap;
				var bitmap1 = bl1.bitmap;
				var bitCount = 0;
				var bitCount0 = 0;
				var bitCount1 = 0;
				var bitmap = bitmap0 | bitmap1;
				var ts = new Object[Long.bitCount(bitmap)];

				for (var bit = 1; bit != 0; bit <<= 1) {
					var t0 = (bitmap0 & bit) != 0 ? bl0.get(bitCount0++) : null;
					var t1 = (bitmap1 & bit) != 0 ? bl1.get(bitCount1++) : null;
					var t = t0 != null ? t1 != null ? f.apply(t0, t1) : t0 : t1;

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
		Object[] ts0;

		if (bl != null) {
			bitmap0 = bl.bitmap;
			ts0 = bl.ts;
		} else {
			bitmap0 = 0;
			ts0 = empty;
		}

		var bit = 1l << index;
		var bits0 = bitmap0 & bit - 1;
		var bits1 = bitmap0 & 0xFFFFFFFFFFFFFFFEl << index;

		var diff0 = (bitmap0 & bit) != 0 ? 1 : 0;
		var diff1 = t != null ? 1 : 0;
		var diff = diff1 - diff0;

		var bitmap1 = bits0 + ((long) diff1 << index) + bits1;

		if (bitmap1 != 0) {
			var ts1 = new Object[ts0.length + diff];
			var bitCount0 = Long.bitCount(bits0);
			var bitCount1 = Long.bitCount(bits1);
			Array_.copy(ts0, 0, ts1, 0, bitCount0);
			ts1[bitCount0] = t;
			Array_.copy(ts0, bitCount0 + diff0, ts1, bitCount0 + diff1, bitCount1);

			return new Bl<>(bitmap1, ts1);
		} else
			return null;
	}

	public static <T> Bl<T> of(List<IntObjPair<T>> list) {
		if (!list.isEmpty()) {
			var size = list.size();
			var bitmap = 0;
			var ts = new Object[size];
			for (var i = 0; i < size; i++) {
				var pair = list.get(i);
				bitmap |= 1l << (pair.k & 0xFFFFFC00);
				ts[i] = pair.v;
			}
			return new Bl<>(bitmap, ts);
		} else
			return null;
	}

	private Bl(long bitmap, Object[] ts) {
		this.bitmap = bitmap;
		this.ts = ts;
	}

	public static <T> T get(Bl<T> bl, int index) {
		var bitmap = bl != null ? bl.bitmap : 0;
		var bit = 1l << index;

		if ((bitmap & bit) != 0) {
			var mask = bit - 1;
			var bitCount = Long.bitCount(bitmap & mask);
			return bl.get(bitCount);
		} else
			return null;
	}

	private T get(int bitCount) {
		@SuppressWarnings("unchecked")
		var t = (T) ts[bitCount];
		return t;
	}

}
