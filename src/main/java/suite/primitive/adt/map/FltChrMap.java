package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.FltChrSink;
import suite.primitive.FltChrSource;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Chr;
import suite.primitive.adt.pair.FltChrPair;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.streamlet.FltObjOutlet;
import suite.primitive.streamlet.FltObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

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

	public static <T> Fun<Outlet<T>, FltChrMap> collect(Obj_Flt<T> kf0, Obj_Chr<T> vf0) {
		Obj_Flt<T> kf1 = kf0.rethrow();
		Obj_Chr<T> vf1 = vf0.rethrow();
		return outlet -> {
			FltChrMap map = new FltChrMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public FltChrMap() {
		this(8);
	}

	public FltChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(float key, Flt_Chr fun) {
		char v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof FltChrMap) {
			FltChrMap other = (FltChrMap) object;
			boolean b = size == other.size;
			for (FltObjPair<Character> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(FltChrSink sink) {
		FltChrPair pair = FltChrPair.of((float) 0, (char) 0);
		FltChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (FltObjPair<Character> pair : streamlet()) {
			h = h * 31 + Float.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public char get(float key) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
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
			float[] ks0 = ks;
			char[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (FltObjPair<Character> pair : streamlet())
			sb.append(pair.t0 + ":" + pair.t1 + ",");
		return sb.toString();
	}

	public void update(float key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		size += ((vs[index] = fun.apply(v)) != ChrFunUtil.EMPTYVALUE ? 1 : 0) - (v != ChrFunUtil.EMPTYVALUE ? 1 : 0);
	}

	public int size() {
		return size;
	}

	public FltChrSource source() {
		return source_();
	}

	public FltObjStreamlet<Character> streamlet() {
		return new FltObjStreamlet<>(() -> FltObjOutlet.of(new FltObjSource<Character>() {
			private FltChrSource source0 = source_();
			private FltChrPair pair0 = FltChrPair.of((float) 0, (char) 0);

			public boolean source2(FltObjPair<Character> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private char put_(float key, char v1) {
		int mask = vs.length - 1;
		int index = Float.hashCode(key) & mask;
		char v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
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
				while (index < capacity)
					if ((v = vs[index]) == ChrFunUtil.EMPTYVALUE)
						index++;
					else {
						pair.update(ks[index++], v);
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new float[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

}
