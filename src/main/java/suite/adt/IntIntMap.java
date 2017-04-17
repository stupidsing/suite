package suite.adt;

import java.util.Arrays;

import suite.primitive.PrimitiveFun.Int_Int;
import suite.primitive.PrimitiveSink.IntIntSink;
import suite.primitive.PrimitiveSource.IntIntSource;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.streamlet.IntObjOutlet;
import suite.streamlet.IntObjStreamlet;

/**
 * Map with integer key and integer object value. Integer.MIN_VALUE is not
 * allowed in values. Not thread-safe.
 * 
 * @author ywsing
 */
public class IntIntMap {

	private int size;
	private int ks[];
	private int vs[];

	public IntIntMap() {
		this(8);
	}

	public IntIntMap(int capacity) {
		allocate(capacity);
	}

	public int computeIfAbsent(int key, Int_Int fun) {
		int v = get(key);
		if (v == Integer.MIN_VALUE)
			put(key, v = fun.apply(key));
		return v;
	}

	public void forEach(IntIntSink sink) {
		IntIntPair pair = IntIntPair.of(0, 0);
		IntIntSource source = source_();
		while (source.source(pair))
			sink.sink(pair.t0, pair.t1);
	}

	public int get(int key) {
		int mask = ks.length - 1;
		int index = key & mask;
		int v_;
		while ((v_ = vs[index]) != Integer.MIN_VALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				break;
		return v_;
	}

	public int put(int key, int v) {
		return put_(key, v);

	}

	public IntIntSource source() {
		return source_();
	}

	public IntObjStreamlet<Integer> stream() {
		return new IntObjStreamlet<>(() -> {
			return IntObjOutlet.of(new IntObjSource<Integer>() {
				private IntIntSource source = source_();
				private IntIntPair pair0 = IntIntPair.of(0, 0);

				public boolean source2(IntObjPair<Integer> pair) {
					boolean b = source.source(pair0);
					if (b) {
						pair.t0 = pair0.t0;
						pair.t1 = pair0.t1;
					}
					return b;
				}
			});
		});
	}

	private int put_(int key, int v1) {
		int capacity = ks.length;
		size++;

		if (capacity * 3 / 4 < size) {
			int capacity1 = capacity * 2;
			int ks0[] = ks;
			int vs0[] = vs;
			allocate(capacity1);

			for (int i = 0; i < capacity; i++) {
				int v_ = vs0[i];
				if (v_ != Integer.MIN_VALUE)
					put_(ks0[i], v_);
			}
		}

		int mask = capacity - 1;
		int index = key & mask;
		int v0;
		while ((v0 = vs[index]) != Integer.MIN_VALUE)
			if (ks[index] != key)
				index = index + 1 & mask;
			else
				throw new RuntimeException("Duplicate key");
		ks[index] = key;
		vs[index] = v1;
		return v0;
	}

	private IntIntSource source_() {
		int capacity = ks.length;
		return new IntIntSource() {
			private int index = 0;

			public boolean source(IntIntPair pair) {
				boolean b;
				int v_ = Integer.MIN_VALUE;
				while ((b = index < capacity) && (v_ = vs[index]) == Integer.MIN_VALUE)
					index++;
				if (b) {
					pair.t0 = ks[index++];
					pair.t1 = v_;
				}
				return b;
			}
		};
	}

	private void allocate(int capacity) {
		ks = new int[capacity];
		vs = new int[capacity];
		Arrays.fill(vs, Integer.MIN_VALUE);
	}

}
