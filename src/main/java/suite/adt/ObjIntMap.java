package suite.adt;

import java.util.Arrays;

import suite.primitive.PrimitiveFun.ObjInt_Obj;
import suite.primitive.PrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveSink.ObjIntSink2;
import suite.primitive.PrimitiveSource.ObjIntSource2;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil2.Source2;

/**
 * Map with generic object key and integer object value. Integer.MIN_VALUE is
 * not allowed in values. Not thread-safe.
 * 
 * @author ywsing
 */
public class ObjIntMap<K> {

	private int size;
	private Object ks[];
	private int vs[];

	public ObjIntMap() {
		this(8);
	}

	public ObjIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(K key, Obj_Int<K> fun) {
		int v = get(key);
		if (v == Integer.MIN_VALUE)
			put(key, v = fun.applyAsInt(key));
		return v;
	}

	public void forEach(ObjIntSink2<K> sink) {
		ObjIntPair<K> pair = ObjIntPair.of(null, 0);
		ObjIntSource2<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public int get(K key) {
		int mask = ks.length - 1;
		int index = key.hashCode() & mask;
		int v_;
		while ((v_ = vs[index]) != Integer.MIN_VALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v_;
	}

	public <T> Streamlet<T> map(ObjInt_Obj<K, T> fun) {
		ObjIntPair<K> pair = ObjIntPair.of(null, 0);
		ObjIntSource2<K> source = source_();
		return Read.from(() -> source.source2(pair) ? fun.apply(pair.t0, pair.t1) : null);
	}

	public int put(K key, int v) {
		return put_(key, v);

	}

	public ObjIntSource2<K> source() {
		return source_();
	}

	public Streamlet2<K, Integer> of() {
		return new Streamlet2<>(() -> {
			ObjIntSource2<K> source = source();
			return Outlet2.of(new Source2<K, Integer>() {
				private ObjIntPair<K> pair0 = ObjIntPair.of(null, 0);

				public boolean source2(Pair<K, Integer> pair) {
					boolean b = source.source2(pair0);
					if (b) {
						pair.t0 = pair0.t0;
						pair.t1 = pair0.t1;
					}
					return b;
				}
			});
		});
	}

	private int put_(Object key, int v1) {
		int capacity = ks.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object ks0[] = ks;
			int vs0[] = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != Integer.MIN_VALUE)
					put_(ks0[i], v_);
			}
		}

		int mask = capacity - 1;
		int index = key.hashCode() & mask;
		int v0;
		while ((v0 = vs[index]) != Integer.MIN_VALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private ObjIntSource2<K> source_() {
		int capacity = ks.length;
		return new ObjIntSource2<K>() {
			private int index = 0;

			public boolean source2(ObjIntPair<K> pair) {
				boolean b;
				int v_ = Integer.MIN_VALUE;
				while ((b = index < capacity) && (v_ = vs[index]) == Integer.MIN_VALUE)
					index++;
				if (b) {
					pair.t0 = cast(ks[index++]);
					pair.t1 = v_;
				}
				return b;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, Integer.MIN_VALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
