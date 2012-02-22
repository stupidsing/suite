package org.suite.node;

import org.util.Util;

/**
 * A list of nodes that can be easily expanded in left or right direction.
 */
public class Vector extends Node {

	private static class Data { // Immutable
		private Node nodes[];
		private int startUsed, endUsed;

		private Data() {
			this(16);
		}

		private Data(int len) {
			this(len, len * 3 / 4);
		}

		private Data(int len, int startUsed) {
			this.nodes = new Node[len];
			this.endUsed = this.startUsed = startUsed;
		}

		private void insertBefore(Node n[], int s, int e) {
			int l1 = e - s;
			startUsed -= l1;
			System.arraycopy(n, s, nodes, startUsed, l1);
		}

		private void insertAfter(Node n[], int s, int e) {
			int l1 = e - s;
			endUsed += l1;
			System.arraycopy(n, s, nodes, endUsed, l1);
		}
	}

	public static Vector EMPTY = new Vector(new Node[0]);

	private Data data;
	private int start, end;

	public Vector(Node node) {
		this(new Node[] { node });
	}

	public Vector(Node nodes[]) {
		this(new Data());
		data.insertBefore(nodes, 0, nodes.length);
	}

	private Vector(Data data) {
		this.data = data;
		this.start = data.startUsed;
		this.end = data.endUsed;
	}

	private Vector(Data data, int start, int end) {
		this.data = data;
		this.start = start;
		this.end = end;
	}

	public static Vector concat(Vector u, Vector v) {
		int ulen = u.length(), vlen = v.length();

		if (u.end == u.data.endUsed && u.data.nodes.length - u.end >= vlen) {
			u.data.insertAfter(v.data.nodes, v.start, v.end);
			return new Vector(u.data, u.start, u.end + vlen);
		} else if (v.start == v.data.startUsed && v.start >= ulen) {
			v.data.insertBefore(u.data.nodes, u.start, u.end);
			return new Vector(v.data, v.start - ulen, v.end);
		} else {
			Data data = new Data(ulen + vlen + 16, 0);
			data.insertAfter(u.data.nodes, u.start, u.end);
			data.insertAfter(v.data.nodes, v.start, v.end);
			return new Vector(data, data.startUsed, data.endUsed);
		}
	}

	public Vector subVector(int s, int e) {
		int length = length();
		while (s < 0)
			s += length;
		while (e <= 0)
			e += length;
		e = Math.min(e, length);
		return new Vector(data, start + s, start + e);
	}

	public int length() {
		return end - start;
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = start; i < end; i++) {
			int h = Util.hashCode(data.nodes[i + data.startUsed]);
			result = 31 * result + h;
		}
		return result;
	}

	@Override
	public boolean equals(Object object) {
		boolean result = false;

		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			if (node instanceof Vector) {
				Vector v = (Vector) node;
				result = end - start == v.end - v.start;
				int si = start, di = v.start;

				while (result && si < end)
					result &= Util.equals(data.nodes[si++], v.data.nodes[di++]);
			}
		}

		return result;
	}

}
