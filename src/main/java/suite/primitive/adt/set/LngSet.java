package suite.primitive.adt.set;

import java.util.Arrays;

import primal.Verbs.Build;
import primal.primitive.LngPrim;
import primal.primitive.LngPrim.LngSink;
import primal.primitive.LngPrim.LngSource;
import primal.primitive.LngVerbs.CopyLng;
import primal.primitive.puller.LngPuller;
import suite.primitive.streamlet.LngStreamlet;

/**
 * Set with longacter elements. Long.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class LngSet {

	private static long empty = LngPrim.EMPTYVALUE;

	private int size;
	private long[] vs;

	public static LngSet intersect(LngSet... sets) {
		return sets[0].streamlet().filter(c -> {
			var b = true;
			for (var set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static LngSet union(LngSet... sets) {
		var set = new LngSet();
		for (var set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public LngSet() {
		this(8);
	}

	public LngSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(long c) {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			var vs0 = vs;
			long v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != empty)
					add_(v_);
		}

		var b = add_(c);
		size += b ? 1 : 0;
		return b;
	}

	public boolean contains(long c) {
		return vs[index(c)] == c;
	}

	public LngSet clone() {
		var capacity = vs.length;
		var set = new LngSet(capacity);
		set.size = size;
		CopyLng.array(vs, 0, set.vs, 0, capacity);
		return set;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof LngSet) {
			var other = (LngSet) object;
			var b = size == other.size;
			for (var c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(LngSink sink) {
		var source = source_();
		long c;
		while ((c = source.g()) != empty)
			sink.f(c);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var c : streamlet())
			h = h * 31 + Long.hashCode(c);
		return h;
	}

	public boolean remove(long c) {
		var mask = vs.length - 1;
		var index = index(c);
		var b = vs[index] == c;
		if (b) {
			vs[index] = empty;
			size--;
			new Object() {
				private void rehash(int index) {
					var index1 = (index + 1) & mask;
					var v = vs[index1];
					if (v != empty) {
						vs[index1] = empty;
						rehash(index1);
						vs[index(v)] = v;
					}
				}
			}.rehash(index);
		}
		return b;
	}

	public int size() {
		return size;
	}

	public LngSource source() {
		return source_();
	}

	public LngStreamlet streamlet() {
		return new LngStreamlet(() -> LngPuller.of(source_()));
	}

	@Override
	public String toString() {
		return Build.string(sb -> streamlet().forEach(sb::append));
	}

	private boolean add_(long c) {
		var index = index(c);
		var b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(long c) {
		var mask = vs.length - 1;
		var index = Long.hashCode(c) & mask;
		long c0;
		while ((c0 = vs[index]) != empty && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private LngSource source_() {
		return new LngSource() {
			private int capacity = vs.length;
			private int index = 0;

			public long g() {
				long v;
				while (index < capacity)
					if ((v = vs[index++]) != empty)
						return v;
				return empty;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new long[capacity];
		Arrays.fill(vs, empty);
	}

}
