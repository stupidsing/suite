package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.IntChrSink;
import suite.primitive.IntChrSource;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Chr;
import suite.primitive.adt.pair.IntChrPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Fail;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive int key and primitive char value. Character.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class IntChrMap {

	private int size;
	private int[] ks;
	private char[] vs;

	public static <T> Fun<Outlet<T>, IntChrMap> collect(Obj_Int<T> kf0, Obj_Chr<T> vf0) {
		Obj_Int<T> kf1 = kf0.rethrow();
		Obj_Chr<T> vf1 = vf0.rethrow();
		return outlet -> {
			IntChrMap map = new IntChrMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public IntChrMap() {
		this(8);
	}

	public IntChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(int key, Int_Chr fun) {
		char v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof IntChrMap) {
			IntChrMap other = (IntChrMap) object;
			boolean b = size == other.size;
			for (IntObjPair<Character> pair : streamlet())
				b &= other.get(pair.t0) == pair.t1;
			return b;
		} else
			return false;
	}

	public void forEach(IntChrSink sink) {
		IntChrPair pair = IntChrPair.of((int) 0, (char) 0);
		IntChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (IntObjPair<Character> pair : streamlet()) {
			h = h * 31 + Integer.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public char get(int key) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public char put(int key, char v) {
		size++;
		char v0 = store(key, v);
		rehash();
		return v0;
	}

	@Override
	public String toString() {
		return streamlet().map((k, v) -> k + ":" + v + ",").collect(As::joined);
	}

	public void update(int key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		char v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		char v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != ChrFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != ChrFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == ChrFunUtil.EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					char v_ = vs[index1];
					if (v_ != ChrFunUtil.EMPTYVALUE) {
						int k = ks[index1];
						vs[index1] = ChrFunUtil.EMPTYVALUE;
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

	public IntChrSource source() {
		return source_();
	}

	public IntObjStreamlet<Character> streamlet() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(new IntObjSource<Character>() {
			private IntChrSource source0 = source_();
			private IntChrPair pair0 = IntChrPair.of((int) 0, (char) 0);

			public boolean source2(IntObjPair<Character> pair) {
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
			char[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				char v_ = vs0[i];
				if (v_ != ChrFunUtil.EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private char store(int key, char v1) {
		int mask = vs.length - 1;
		int index = Integer.hashCode(key) & mask;
		char v0;
		while ((v0 = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				Fail.t("duplicate key " + key);
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntChrSource source_() {
		return new IntChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntChrPair pair) {
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
		ks = new int[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

}
