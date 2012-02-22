package org.suite.node;

import org.util.Util;

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

	private Data data;
	private int start, end;

	private Vector(Data data, int start, int end) {
		this.data = data;
		this.start = start;
		this.end = end;
	}

	public static Vector concat(Vector u, Vector v) {
		int ulen = u.end - u.start, vlen = v.end - v.start;

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
		return new Vector(data, start + s, start + e);
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
