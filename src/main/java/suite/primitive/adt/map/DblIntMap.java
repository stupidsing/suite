package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.DblIntSink;
import suite.primitive.DblIntSource;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Int;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.adt.pair.DblIntPair;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.streamlet.DblObjOutlet;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive double key and primitive int value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblIntMap {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private int size;
	private double[] ks;
	private int[] vs;

	public static <T> Fun<Outlet<T>, DblIntMap> collect(Obj_Dbl<T> kf0, Obj_Int<T> vf0) {
		Obj_Dbl<T> kf1 = kf0.rethrow();
		Obj_Int<T> vf1 = vf0.rethrow();
		return outlet -> {
			DblIntMap map = new DblIntMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public DblIntMap() {
		this(8);
	}

	public DblIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(double key, Dbl_Int fun) {
		int v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof DblIntMap) {
			DblIntMap other = (DblIntMap) object;
			boolean b = size == other.size;
			for (DblObjPair<Integer> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(DblIntSink sink) {
		DblIntPair pair = DblIntPair.of((double) 0, (int) 0);
		DblIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (DblObjPair<Integer> pair : streamlet()) {
			h = h * 31 + Double.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public int get(double key) {
		int index = index(key);
		return ks[index] == key ? vs[index] : EMPTYVALUE;
	}

	public void put(double key, int v) {
		size++;
		store(key, v);
		rehash();
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(double key, Int_Int fun) {
		int mask = vs.length - 1;
		int index = index(key);
		int v0 = vs[index];
		int v1 = vs[index] = fun.apply(v0);
		ks[index] = key;
		size += (v1 != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					int v_ = vs[index1];
					if (v_ != EMPTYVALUE) {
						double k = ks[index1];
						vs[index1] = EMPTYVALUE;
						rehash(index1);
						store(k, v_);
					}
				}
			}.rehash(index);
		rehash();
	}

	public int size() {
		return size;
	}

	public DblIntSource source() {
		return source_();
	}

	public DblObjStreamlet<Integer> streamlet() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(new DblObjSource<Integer>() {
			private DblIntSource source0 = source_();
			private DblIntPair pair0 = DblIntPair.of((double) 0, (int) 0);

			public boolean source2(DblObjPair<Integer> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			double[] ks0 = ks;
			int[] vs0 = vs;
			int v_;

			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != EMPTYVALUE)
					store(ks0[i], v_);
		}
	}

	private void store(double key, int v1) {
		int index = index(key);
		if (vs[index] == EMPTYVALUE) {
			ks[index] = key;
			vs[index] = v1;
		} else
			Fail.t("duplicate key " + key);
	}

	private int index(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		while (vs[index] != EMPTYVALUE && ks[index] != key)
			index = index + 1 & mask;
		return index;
	}

	private DblIntSource source_() {
		return new DblIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblIntPair pair) {
				while (index < capacity) {
					double k = ks[index];
					int v = vs[index++];
					if (v != EMPTYVALUE) {
						pair.update(k, v);
						return true;
					}
				}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new double[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
