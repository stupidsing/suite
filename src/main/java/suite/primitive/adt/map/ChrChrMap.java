package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.ChrChrSink;
import suite.primitive.ChrChrSource;
import suite.primitive.ChrFunUtil;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.adt.pair.ChrChrPair;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.streamlet.ChrObjOutlet;
import suite.primitive.streamlet.ChrObjStreamlet;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Fun;

/**
 * Map with primitive char key and primitive char value. Character.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ChrChrMap {

	private int size;
	private char[] ks;
	private char[] vs;

	public static <T> Fun<Outlet<T>, ChrChrMap> collect(Obj_Chr<T> kf0, Obj_Chr<T> vf0) {
		Obj_Chr<T> kf1 = kf0.rethrow();
		Obj_Chr<T> vf1 = vf0.rethrow();
		return outlet -> {
			ChrChrMap map = new ChrChrMap();
			T t;
			while ((t = outlet.source().source()) != null)
				map.put(kf1.apply(t), vf1.apply(t));
			return map;
		};
	}

	public ChrChrMap() {
		this(8);
	}

	public ChrChrMap(int capacity) {
		allocate(capacity);
	}

	public char computeIfAbsent(char key, Chr_Chr fun) {
		char v = get(key);
		if (v == ChrFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(ChrChrSink sink) {
		ChrChrPair pair = ChrChrPair.of((char) 0, (char) 0);
		ChrChrSource source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public char get(char key) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		char v;
		while ((v = vs[index]) != ChrFunUtil.EMPTYVALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public char put(char key, char v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			char[] ks0 = ks;
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

	public void update(char key, Chr_Chr fun) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
		char v;
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

	public ChrChrSource source() {
		return source_();
	}

	public ChrObjStreamlet<Character> streamlet() {
		return new ChrObjStreamlet<>(() -> ChrObjOutlet.of(new ChrObjSource<Character>() {
			private ChrChrSource source0 = source_();
			private ChrChrPair pair0 = ChrChrPair.of((char) 0, (char) 0);

			public boolean source2(ChrObjPair<Character> pair) {
				boolean b = source0.source2(pair0);
				pair.update(pair0.t0, pair0.t1);
				return b;
			}
		}));
	}

	private char put_(char key, char v1) {
		int mask = vs.length - 1;
		int index = Character.hashCode(key) & mask;
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

	private ChrChrSource source_() {
		return new ChrChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(ChrChrPair pair) {
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
		ks = new char[capacity];
		vs = new char[capacity];
		Arrays.fill(vs, ChrFunUtil.EMPTYVALUE);
	}

}
