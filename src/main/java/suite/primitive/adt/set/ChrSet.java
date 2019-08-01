package suite.primitive.adt.set;

import java.util.Arrays;

import primal.primitive.ChrPrim;
import primal.primitive.ChrPrim.ChrSink;
import primal.primitive.ChrPrim.ChrSource;
import suite.primitive.Chars_;
import suite.primitive.streamlet.ChrPuller;
import suite.primitive.streamlet.ChrStreamlet;
import suite.util.To;

/**
 * Set with character elements. Character.MIN_VALUE is not allowed. Not
 * thread-safe.
 *
 * @author ywsing
 */
public class ChrSet {

	private static char empty = ChrPrim.EMPTYVALUE;

	private int size;
	private char[] vs;

	public static ChrSet intersect(ChrSet... sets) {
		return sets[0].streamlet().filter(c -> {
			var b = true;
			for (var set_ : sets)
				b &= set_.contains(c);
			return b;
		}).toSet();
	}

	public static ChrSet union(ChrSet... sets) {
		var set = new ChrSet();
		for (var set_ : sets)
			set_.streamlet().sink(set::add);
		return set;
	}

	public ChrSet() {
		this(8);
	}

	public ChrSet(int capacity) {
		allocate(capacity);
	}

	public boolean add(char c) {
		var capacity = vs.length;

		if (capacity * 3 / 4 < size) {
			var vs0 = vs;
			char v_;

			allocate(capacity * 2);

			for (var i = 0; i < capacity; i++)
				if ((v_ = vs0[i]) != empty)
					add_(v_);
		}

		var b = add_(c);
		size += b ? 1 : 0;
		return b;
	}

	public boolean contains(char c) {
		return vs[index(c)] == c;
	}

	public ChrSet clone() {
		var capacity = vs.length;
		var set = new ChrSet(capacity);
		set.size = size;
		Chars_.copy(vs, 0, set.vs, 0, capacity);
		return set;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof ChrSet) {
			var other = (ChrSet) object;
			var b = size == other.size;
			for (var c : streamlet())
				b &= other.contains(c);
			return b;
		} else
			return false;
	}

	public void forEach(ChrSink sink) {
		var source = source_();
		char c;
		while ((c = source.g()) != empty)
			sink.f(c);
	}

	@Override
	public int hashCode() {
		var h = 7;
		for (var c : streamlet())
			h = h * 31 + Character.hashCode(c);
		return h;
	}

	public boolean remove(char c) {
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

	public ChrSource source() {
		return source_();
	}

	public ChrStreamlet streamlet() {
		return new ChrStreamlet(() -> ChrPuller.of(source_()));
	}

	@Override
	public String toString() {
		return To.string(sb -> streamlet().forEach(sb::append));
	}

	private boolean add_(char c) {
		var index = index(c);
		var b = vs[index] != c;
		vs[index] = c;
		return b;
	}

	private int index(char c) {
		var mask = vs.length - 1;
		var index = Character.hashCode(c) & mask;
		char c0;
		while ((c0 = vs[index]) != empty && c0 != c)
			index = index + 1 & mask;
		return index;
	}

	private ChrSource source_() {
		return new ChrSource() {
			private int capacity = vs.length;
			private int index = 0;

			public char g() {
				char v;
				while (index < capacity)
					if ((v = vs[index++]) != empty)
						return v;
				return empty;
			}
		};
	}

	private void allocate(int capacity) {
		vs = new char[capacity];
		Arrays.fill(vs, empty);
	}

}
