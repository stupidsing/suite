package suite.fp;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;

public class ImmutableRbTree<T> implements Iterable<T> {

	private Node<T> root;
	private Comparator<T> comparator;

	private static class Node<T> {
		private boolean isBlack;
		private T pivot;
		private Node<T> left, right;

		private Node(boolean isBlack, T pivot, Node<T> left, Node<T> right) {
			this.isBlack = isBlack;
			this.pivot = pivot;
			this.left = left;
			this.right = right;
		}
	}

	public ImmutableRbTree(Comparator<T> comparator) {
		this(null, comparator);
	}

	private ImmutableRbTree(Node<T> root, Comparator<T> comparator) {
		this.root = root;
		this.comparator = comparator;
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private Deque<Node<T>> nodes = new ArrayDeque<>();

			{
				pushLefts(root);
			}

			public boolean hasNext() {
				return !nodes.isEmpty();
			}

			public T next() {
				Node<T> node = nodes.pop();
				T result = node.pivot;
				pushLefts(node.right);
				return result;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}

			private void pushLefts(Node<T> node) {
				while (node != null) {
					nodes.push(node);
					node = node.left;
				}
			}
		};
	}

	public T find(T t) {
		Node<T> node = root;

		while (node != null) {
			int c = comparator.compare(node.pivot, t);
			if (c < 0)
				node = node.left;
			else if (c > 0)
				node = node.right;
			else
				return node.pivot;
		}

		return null;
	}

	public ImmutableRbTree<T> add(T t) {
		return add(t, false);
	}

	/**
	 * Replaces a value with another. Mainly for dictionary cases to replace
	 * stored value for the same key.
	 * 
	 * Asserts comparator.compare(<original-value>, t) == 0.
	 */
	public ImmutableRbTree<T> replace(T t) {
		return add(t, true);
	}

	private ImmutableRbTree<T> add(T t, boolean isReplace) {
		Node<T> node = root;

		if (node != null && !node.isBlack) // Turns red node into black
			node = new Node<>(true, node.pivot, node.left, node.right);

		return new ImmutableRbTree<>(add(node, t, isReplace), comparator);
	}

	private Node<T> add(Node<T> node, T t, boolean isReplace) {
		Node<T> node1;

		if (node != null) {
			int c = comparator.compare(node.pivot, t);

			if (c < 0)
				node1 = new Node<>(node.isBlack, node.pivot, add(node.left, t, isReplace), node.right);
			else if (c > 0)
				node1 = new Node<>(node.isBlack, node.pivot, node.left, add(node.right, t, isReplace));
			else if (isReplace)
				node1 = new Node<>(node.isBlack, t, node.left, node.right);
			else
				throw new RuntimeException("Duplicate node " + t);
		} else
			node1 = new Node<>(false, t, null, null);

		return balance(node1);
	}

	private Node<T> balance(Node<T> n) {
		if (n.isBlack) {
			Node<T> ln = n.left, rn = n.right;

			if (ln != null && !ln.isBlack) {
				Node<T> lln = ln.left, lrn = ln.right;
				if (lln != null && !lln.isBlack)
					n = createBalancedNode(lln.left, lln.pivot, lln.right, ln.pivot, lrn, n.pivot, rn);
				else if (lrn != null && !lrn.isBlack)
					n = createBalancedNode(lln, ln.pivot, lrn.left, lrn.pivot, lrn.right, n.pivot, rn);
			} else if (rn != null && !rn.isBlack) {
				Node<T> rln = rn.left, rrn = rn.right;
				if (rln != null && !rln.isBlack)
					n = createBalancedNode(ln, n.pivot, rln.left, rln.pivot, rln.right, rn.pivot, rrn);
				else if (rrn != null && !rrn.isBlack)
					n = createBalancedNode(ln, n.pivot, rln, rn.pivot, rrn.left, rrn.pivot, rrn.right);
			}
		}

		return n;
	}

	private Node<T> createBalancedNode(Node<T> n0, T p0, Node<T> n1, T p1, Node<T> n2, T p2, Node<T> n3) {
		return new Node<T>(false, p1, new Node<T>(true, p0, n0, n1), new Node<T>(true, p2, n2, n3));

	}

}
