package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.ChrFltSink;
import suite.primitive.ChrFltSource;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Flt;
import suite.primitive.FltFunUtil;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.pair.ChrFltPair;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive char key and primitive float value. Float.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrFltMap {

	private int size;
	private char[] ks;
	private float[] vs;

	public static <T> Fun<Outlet<T>, ChrFltMap> collect(Obj_Chr<T> kf0, Obj_Flt<T> vf0) {
		Obj_Chr<T> kf1 = kf0.rethrow();
		Obj_Flt<T> vf1 = vf0.rethrow();
		return outlet -> {
			ChrFltMap map = new ChrFltMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public ChrFltMap() {
		this(8);
	}

	public ChrFltMap(int capacity) {
		allocate(capacity);
	}

	public float computeIfAbsent(char key, Chr_Flt fun) {
		float v = get(key);
		if (v == FltFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ChrFltMap) {
			ChrFltMap other = (ChrFltMap) object;
			boolean b = size == other.size;
			for (ChrObjPair<Float> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(ChrFltSink sink) {
		ChrFltPair pair = ChrFltPair.of((char) 0, (float) 0);
		ChrFltSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (ChrObjPair<Float> pair : streamlet()) {
			h = h * 31 + Character.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public float get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public float put(char key, float v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			char[] ks0 = ks;
			float[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				float v_ = vs0[i];
				if (v_ != FltFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(char key, Flt_Flt fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		float v;
		while ((v = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		size += ((vs[index] = fun.apply(v)) != FltFunUtil.EMPTYVALUE ? 1 : 0) - (v != FltFunUtil.EMPTYVALUE ? 1 : 0);
	}

	public int size() {
		return size;
	}

	public ChrFltSource source() {
		return source_();
	}

	public ChrObjStreamlet<Float> streamlet() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(new ChrObjSource<Float>() {
			private ChrFltSource source0 = source_();
			private ChrFltPair pair0 = ChrFltPair.of((char) 0, (float) 0);

			public boolean source2(ChrObjPair<Float> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private float put_(char key, float v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		float v0;
		while ((v0 = vs[index]) != FltFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrFltSource source_() {
		return new ChrFltSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrFltPair pair) {
				float v;
				while (index < capacity)
					if ((v = vs[index]) == FltFunUtil.EMPTYVALUE)
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
		ks = new char[capacity];
		vs = new float[capacity];
		Arrays.fill(vs, FltFunUtil.EMPTYVALUE);
	}

}
