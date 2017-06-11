package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.FltChrPair;
import suite.primitive.ChrFunUtil;
import suite.primitive.Chr_Chr;
import suite.primitive.FltChrSink;
import suite.primitive.FltChrSource;
import suite.primitive.FltFunUtil;
import suite.primitive.Flt_Chr;

/**
 * Map with primitive float key and primitive char value. Character.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class FltChrMap {

	private int size;
	private float[] ks;
	private char[] vs;

	public FltChrMap() {
		this(8);
	}

	public FltChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(float key, Flt_Chr fun) {
		char v = get(key);
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(FltChrSink sink) {
		FltChrPair pair = FltChrPair.of((float) 0, (char) 0);
		FltChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public char get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public char put(float key, char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			float[] ks0 = ks;
			char[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(float key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public FltChrSource source() {
		return source_();
	}

	// public FltChrStreamlet stream() {
	// return new FltChrStreamlet<>(() -> FltChrOutlet.of(source_()));
	// }

	private char put_(float key, char v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		char v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private FltChrSource source_() {
		return new FltChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(FltChrPair pair) {
				char v;
				while ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new float[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

}
