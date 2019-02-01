package suite.persistent;

import static suite.util.Friends.min;

import java.util.Objects;

import suite.object.Object_;
import suite.util.Array_;

/**
 * A list of nodes that can be easily expanded in left or right direction.
 */
public class PerVector<T> {

	@SuppressWarnings("unchecked")
	public T[] emptyArray = (T[]) new Object[0];
	public PerVector<T> empty = new PerVector<>(emptyArray);

	private Data<T> data;
	private int start, end;

	private static class Data<T> {
		private T[] nodes;
		private int startUsed, endUsed;

		private Data() {
			this(16);
		}

		private Data(int len) {
			this(len, len * 3 / 4);
		}

		private Data(int len, int startUsed) {
			@SuppressWarnings("unchecked")
			var array = (T[]) new Object[len];
			nodes = array;
			endUsed = this.startUsed = startUsed;
		}

		private void insertBefore(T n) {
			nodes[--startUsed] = n;
		}

		private void insertAfter(T n) {
			nodes[++endUsed] = n;
		}

		private void insertBefore(T[] n, int s, int e) {
			var l1 = e - s;
			startUsed -= l1;
			Array_.copy(n, s, nodes, startUsed, l1);
		}

		private void insertAfter(T[] n, int s, int e) {
			var l1 = e - s;
			Array_.copy(n, s, nodes, endUsed, l1);
			endUsed += l1;
		}
	}

	public PerVector(T node) {
		this.data = new Data<>();
		data.insertBefore(node);
	}

	public PerVector(T[] nodes) {
		this.data = new Data<>();
		data.insertBefore(nodes, 0, nodes.length);
	}

	private PerVector(Data<T> data, int start, int end) {
		this.data = data;
		this.start = start;
		this.end = end;
	}

	public PerVector<T> cons(T n, PerVector<T> v) {
		var vlen = v.length();

		if (v.start == v.data.startUsed && 1 <= v.start) {
			v.data.insertBefore(n);
			return new PerVector<>(v.data, v.start - 1, v.end);
		} else {
			Data<T> data = new Data<>(vlen + 16, 0);
			data.insertAfter(n);
			data.insertAfter(v.data.nodes, v.start, v.end);
			return new PerVector<>(data, data.startUsed, data.endUsed);
		}
	}

	public PerVector<T> concat(PerVector<T> u, PerVector<T> v) {
		int ulen = u.length(), vlen = v.length();

		if (u.end == u.data.endUsed && vlen <= u.data.nodes.length - u.end) {
			u.data.insertAfter(v.data.nodes, v.start, v.end);
			return new PerVector<>(u.data, u.start, u.end + vlen);
		} else if (v.start == v.data.startUsed && ulen <= v.start) {
			v.data.insertBefore(u.data.nodes, u.start, u.end);
			return new PerVector<>(v.data, v.start - ulen, v.end);
		} else {
			var data = new Data<T>(ulen + vlen + 16, 0);
			data.insertAfter(u.data.nodes, u.start, u.end);
			data.insertAfter(v.data.nodes, v.start, v.end);
			return new PerVector<>(data, data.startUsed, data.endUsed);
		}
	}

	public T get(int i) {
		return data.nodes[start + i];
	}

	public PerVector<T> range(int s, int e) {
		var length = length();
		while (s < 0)
			s += length;
		while (e <= 0)
			e += length;
		e = min(e, length);
		return new PerVector<>(data, start + s, start + e);
	}

	public int length() {
		return end - start;
	}

	@Override
	public boolean equals(Object object) {
		var b = false;

		if (Object_.clazz(object) == PerVector.class) {
			@SuppressWarnings("unchecked")
			PerVector<T> v = (PerVector<T>) object;
			b = end - start == v.end - v.start;
			int si = start, di = v.start;

			while (b && si < end)
				b &= Objects.equals(data.nodes[si++], v.data.nodes[di++]);
		}

		return b;
	}

	@Override
	public int hashCode() {
		var hashCode = 7;
		for (var i = start; i < end; i++) {
			var h = Objects.hashCode(data.nodes[i]);
			hashCode = hashCode * 31 + h;
		}
		return hashCode;
	}

}
