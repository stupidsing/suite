package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.DblChrSink;
import suite.primitive.DblChrSource;
import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Chr;
import suite.primitive.adt.pair.DblChrPair;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive double key and primitive char value. Character.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblChrMap {

	private int size;
	private double[] ks;
	private char[] vs;

	public static <T> Fun<Outlet<T>, DblChrMap> collect(Obj_Dbl<T> kf0, Obj_Chr<T> vf0) {
		return outlet -> {
			Obj_Dbl<T> kf1 = kf0.rethrow();
			Obj_Chr<T> vf1 = vf0.rethrow();
			DblChrMap map = new DblChrMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public DblChrMap() {
		this(8);
	}

	public DblChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(double key, Dbl_Chr fun) {
		char v = get(key);
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(DblChrSink sink) {
		DblChrPair pair = DblChrPair.of((double) 0, (char) 0);
		DblChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public char get(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public char put(double key, char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			double[] ks0 = ks;
			char[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(double key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public DblChrSource source() {
		return source_();
	}

	// public DblChrStreamlet stream() {
	// return new DblChrStreamlet<>(() -> DblChrOutlet.of(source_()));
	// }

	private char put_(double key, char v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		char v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblChrSource source_() {
		return new DblChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblChrPair pair) {
				char v;
				while ((v = vs[index]) == DblFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new double[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

}
