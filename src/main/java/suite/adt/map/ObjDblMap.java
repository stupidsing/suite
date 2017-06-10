package suite.adt.map;

import java.util.Arrays;

import suite.adt.pair.DblObjPair;
import suite.primitive.DblFunUtil;
import suite.primitive.DblPrimitives.DblObjSink;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.streamlet.DblObjOutlet;
import suite.streamlet.DblObjStreamlet;

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

	public double put(K key, double v) {
		int capacity = vs.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			Object[] ks0 = ks;
			double[] vs0 = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				double v_ = vs0[i];
				if (v_ != DblFunUtil.EMPTYVALUE)
					put_(ks0[i], v_);
			}
		}

		return put_(key, v);
	}

	public void update(K key, Dbl_Dbl fun) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		double v;
		while ((v = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				break;
		vs[index] = fun.apply(v);
	}

	public DblObjSource<K> source() {
		return source_();
	}

	public DblObjStreamlet<K> stream() {
		return new DblObjStreamlet<>(() -> DblObjOutlet.of(source_()));
	}

	private double put_(Object key, double v1) {
		int mask = vs.length - 1;
		int index = key.hashCode() & mask;
		double v0;
		while ((v0 = vs[index]) != DblFunUtil.EMPTYVALUE)
			if (!ks[index].equals(key))
				index = index + 1 & mask;
			else
				throw new RuntimeException("duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private DblObjSource<K> source_() {
		return new DblObjSource<K>() {
			private int capacity = vs.length;
			private int index = 0;

			public boolean source2(DblObjPair<K> pair) {
				double v;
				while ((v = vs[index]) == DblFunUtil.EMPTYVALUE)
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
		vs = new double[capacity];
		Arrays.fill(vs, DblFunUtil.EMPTYVALUE);
	}

	private K cast(Object o) {
		@SuppressWarnings("unchecked")
		K k = (K) o;
		return k;
	}

}
