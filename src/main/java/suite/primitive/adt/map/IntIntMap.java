package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.IntFunUtil;
import suite.primitive.IntIntSink;
import suite.primitive.IntIntSource;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.adt.pair.IntIntPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive int key and primitive int value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntIntMap {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private int size;
	private int[] ks;
	private int[] vs;

	public static <T> Fun<Outlet<T>, IntIntMap> collect(Obj_Int<T> kf0, Obj_Int<T> vf0) {
		Obj_Int<T> kf1 = kf0.rethrow();
		Obj_Int<T> vf1 = vf0.rethrow();
		return outlet -> {
			IntIntMap map = new IntIntMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public IntIntMap() {
		this(8);
	}

	public IntIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(int key, Int_Int fun) {
		int v = get(key);
		if (v == EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntIntMap) {
			IntIntMap other = (IntIntMap) object;
			boolean b = size == other.size;
			for (IntObjPair<Integer> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(IntIntSink sink) {
		IntIntPair pair = IntIntPair.of((int) 0, (int) 0);
		IntIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (IntObjPair<Integer> pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public int get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		int v;
		while ((v = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(int key, int v) {
		size++;
		int v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(int key, Int_Int fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		int v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		int v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != EMPTYVALUE ? 1 : 0) - (v0 != EMPTYVALUE ? 1 : 0);
		if (v1 == EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					int v_ = vs[index1];
					if (v_ != EMPTYVALUE) {
						int k = ks[index1];
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

	public IntIntSource source() {
		return source_();
	}

	public IntObjStreamlet<Integer> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(new IntObjSource<Integer>() {
			private IntIntSource source0 = source_();
			private IntIntPair pair0 = IntIntPair.of((int) 0, (int) 0);

			public boolean source2(IntObjPair<Integer> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			int[] ks0 = ks;
			int[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private int store(int key, int v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		int v0;
		while ((v0 = vs[index]) != EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				Fail.t("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntIntSource source_() {
		return new IntIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntIntPair pair) {
				int v;
				while (index < capacity)
					if ((v = vs[index]) == EMPTYVALUE)
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
		ks = new int[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, EMPTYVALUE);
	}

}
