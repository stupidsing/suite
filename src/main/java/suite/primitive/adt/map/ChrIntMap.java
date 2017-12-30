package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrIntSink;
import suite.primitive.ChrIntSource;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Int;
import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.adt.pair.ChrIntPair;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive char key and primitive int value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrIntMap {

	private int size;
	private char[] ks;
	private int[] vs;

	public static <T> Fun<Outlet<T>, ChrIntMap> collect(Obj_Chr<T> kf0, Obj_Int<T> vf0) {
		Obj_Chr<T> kf1 = kf0.rethrow();
		Obj_Int<T> vf1 = vf0.rethrow();
		return outlet -> {
			ChrIntMap map = new ChrIntMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public ChrIntMap() {
		this(8);
	}

	public ChrIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(char key, Chr_Int fun) {
		int v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ChrIntSink sink) {
		ChrIntPair pair = ChrIntPair.of((char) 0, (int) 0);
		ChrIntSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public int get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		int v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(char key, int v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			char[] ks0 = ks;
			int[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(char key, Int_Int fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		int v;
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

	public ChrIntSource source() {
		return source_();
	}

	public ChrObjStreamlet<Integer> streamlet() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(new ChrObjSource<Integer>() {
			private ChrIntSource source0 = source_();
			private ChrIntPair pair0 = ChrIntPair.of((char) 0, (int) 0);

			public boolean source2(ChrObjPair<Integer> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private int put_(char key, int v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		int v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ChrIntSource source_() {
		return new ChrIntSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrIntPair pair) {
				int v;
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
		ks = new char[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, IntFunUtil.EMPTYVALUE);
	}

}
