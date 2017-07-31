package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrLngSink;
import suite.primitive.ChrLngSource;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Lng;
import suite.primitive.LngFunUtil;
import suite.primitive.LngPrimitives.Obj_Lng;
import suite.primitive.Lng_Lng;
import suite.primitive.adt.pair.ChrLngPair;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive char key and primitive long value. Long.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrLngMap {

	private int size;
	private char[] ks;
	private long[] vs;

	public static <T> Fun<Outlet<T>, ChrLngMap> collect(Obj_Chr<T> kf0, Obj_Lng<T> vf0) {
		Obj_Chr<T> kf1 = kf0.rethrow();
		Obj_Lng<T> vf1 = vf0.rethrow();
		return outlet -> {
			ChrLngMap map = new ChrLngMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public ChrLngMap() {
		this(8);
	}

	public ChrLngMap(int capacity) {
		allocate(capacity);
	}

	public long computeIfAbsent(char key, Chr_Lng fun) {
		long v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ChrLngSink sink) {
		ChrLngPair pair = ChrLngPair.of((char) 0, (long) 0);
		ChrLngSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public long get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public long put(char key, long v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			char[] ks0 = ks;
			long[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				long v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(char key, Lng_Lng fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		long v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public int size() {
		return size;
	}

	public ChrLngSource source() {
		return source_();
	}

	public ChrObjStreamlet<Long> stream() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(new ChrObjSource<Long>() {
			private ChrLngSource source0 = source_();
			private ChrLngPair pair0 = ChrLngPair.of((char) 0, (long) 0);

			public boolean source2(ChrObjPair<Long> pair) {
				boolean b = source0.source2(pair0);
				pair.t0 = pair0.t0;
				pair.t1 = pair0.t1;
				return b;
			}
		}));
	}

	private long put_(char key, long v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		long v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrLngSource source_() {
		return new ChrLngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrLngPair pair) {
				long v;
				while ((v = vs[index]) == ChrFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t0 = ks[index++];
				pair.t1 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new char[capacity];
		vs = new long[capacity];
		Arrays.fill(vs, LngFunUtil.EMPTYVALUE);
	}

}
