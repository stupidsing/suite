package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.DblFunUtil;
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
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive double key and primitive int value. Integer.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class DblIntMap {

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
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(DblIntSink sink) {
		DblIntPair pair = DblIntPair.of((double) 0, (int) 0);
		DblIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public int get(double key) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		int v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(double key, int v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			double[] ks0 = ks;
			int[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(double key, Int_Int fun) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		int v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
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

	private int put_(double key, int v1) {
		int mask = vs.length - 1;
		int index = Double.hashCode(key) & mask;
		int v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblIntSource source_() {
		return new DblIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblIntPair pair) {
				int v;
				while ((v = vs[index]) == DblFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.update(ks[index++], v);
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new double[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, IntFunUtil.EMPTYVALUE);
	}

}
