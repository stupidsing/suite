package suite.primitive.adt.map;

import java.util.Arrays;

import suite.primitive.IntFunUtil;
import suite.primitive.IntPrimitives.IntObjSink;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.util.FunUtil.Fun;

/**
 * Map with generic object key and intacter object value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjIntMap<K> {

	private int size;
	private Object[] ks;
	private int[] vs;

	public static <K> Fun<IntObjOutlet<K>, ObjIntMap<K>> collect() {
		return outlet -> {
			ObjIntMap<K> map = new ObjIntMap<>();
			IntObjPair<K> pair = IntObjPair.of((int) 0, null);
			while (outlet.source().source2(pair))
				map.put(pair.t1, pair.t0);
			return map;
		};
	}

	public ObjIntMap() {
		this(8);
	}

	public ObjIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(K key, Obj_Int<K> fun) {
		int v = get(key);
		if (v == IntFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntObjSink<K> sink) {
		IntObjPair<K> pair = IntObjPair.of((int) 0, null);
		IntObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public int get(K key) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		int v;
		while ((v = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	public int put(K key, int v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object[] ks0 = ks;
			int[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != IntFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(K key, Int_Int fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		int v;
		while ((v = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public IntObjSource<K> source() {
		return source_();
	}

	public IntObjStreamlet<K> stream() {
		return new IntObjStreamlet<>(() -> IntObjOutlet.of(source_()));
	}

	private int put_(Object key, int v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		int v0;
		while ((v0 = vs[index]) != IntFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntObjSource<K> source_() {
		return new IntObjSource<K>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(IntObjPair<K> pair) {
				int v;
				while ((v = vs[index]) == IntFunUtil.EMPTYVALUE)
					if (capacity <= ++index)
						return false;
				pair.t1 = cast(ks[index++]);
				pair.t0 = v;
				return true;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, IntFunUtil.EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
