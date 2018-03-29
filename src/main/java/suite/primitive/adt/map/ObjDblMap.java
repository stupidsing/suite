package suite.primitive.adt.map;

import java.util.Arrays;
import java.util.Objects;

import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.DblObjSink;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.streamlet.DblObjOutlet;
import suite.primitive.streamlet.DblObjStreamlet;
import suite.streamlet.As;
import suite.util.Fail;

/**
 * Map with generic object key and doubleacter object value. Double.MIN_VALUE
 * is not allowed in values. Not thread-safe.
 *
 * @author ywsing
 */
public class ObjDblMap<K> {

	private int size;
	private Object[] ks;
	private double[] vs;

	public static <K> ObjDblMap<K> collect(DblObjOutlet<K> outlet) {
		ObjDblMap<K> map = new ObjDblMap<>();
		DblObjPair<K> pair = DblObjPair.of((double) 0, null);
		while (outlet.source().source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public ObjDblMap() {
		this(8);
	}

	public ObjDblMap(int capacity) {
		allocate(capacity);
	}

	public double computeIfAbsent(K key, Obj_Dbl<K> fun) {
		double v = get(key);
		if (v == DblFunUtil.EMPTYVALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ObjDblMap) {
			@SuppressWarnings("unchecked")
			ObjDblMap<Object> other = (ObjDblMap<Object>) object;
			boolean b = size == other.size;
			for (DblObjPair<K> pair : streamlet())
				b &= other.get(pair.t1) == pair.t0;
			return b;
		} else
			return false;
	}

	public void forEach(DblObjSink<K> sink) {
		DblObjPair<K> pair = DblObjPair.of((double) 0, null);
		DblObjSource<K> source = source_();
		while (source.source2(pair))
			sink.sink2(pair.t0, pair.t1);
	}

	public double get(K key) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		return v;
	}

	@Override
	public int hashCode() {
		int h = 7;
		for (DblObjPair<K> pair : streamlet()) {
			h = h * 31 + Double.hashCode(pair.t0);
			h = h * 31 + Objects.hashCode(pair.t1);
		}
		return h;
	}

	public double put(K key, double v) {
		size++;
		double v0 = store(key, v);
		rehash();
		return v0;
	}

	public void update(K key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		double v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		double v1 = fun.apply(v0);
		ks[index] = key;
		size += ((vs[index] = v1) != DblFunUtil.EMPTYVALUE ? 1 : 0) - (v0 != DblFunUtil.EMPTYVALUE ? 1 : 0);
		if (v1 == DblFunUtil.EMPTYVALUE)
			new Object() {
				public void rehash(int index) {
					int index1 = (index + 1) & mask;
					double v = vs[index1];
					if (v != DblFunUtil.EMPTYVALUE) {
						Object k = ks[index1];
						vs[index1] = DblFunUtil.EMPTYVALUE;
						rehash(index1);
						store(k, v);
					}
				}
			}.rehash(index);

		rehash();
	}

	public int size() {
		return size;
	}

	public DblObjSource<K> source() {
		return source_();
	}

	public DblObjStreamlet<K> streamlet() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(source_()));
	}

	@Override
	public String toString() {
		return streamlet().map((v, k) -> k + ":" + v + ",").collect(As::joined);
	}

	private void rehash() {
		int capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			Object[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity * 2);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					store(ks0[i], v_);
			}
		}
	}

	private double store(Object key, double v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		double v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				Fail.t("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblObjSource<K> source_() {
		return new DblObjSource<>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblObjPair<K> pair) {
				double v;
				while (index < capacity)
					if ((v = vs[index]) == DblFunUtil.EMPTYVALUE)
						index++;
					else {
						pair.update(v, cast(ks[index++]));
						return true;
					}
				return false;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new Object[capacity];
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
