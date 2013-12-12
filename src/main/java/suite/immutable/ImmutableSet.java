package suite.immutable;

import java.util.Comparator;
import java.util.Iterator;

import suite.util.Util;

public class ImmutableSet<V extends Comparable<V>> implements Iterable<V> {

	private ImmutableTree<V> tree = new Tree23<V>(new Comparator<V>() {
		public int compare(V p0, V p1) {
			return Util.compare(p0, p1);
		}
	});

	public ImmutableSet() {
	}

	public ImmutableSet(ImmutableTree<V> tree) {
		this.tree = tree;
	}

	public V get(V k) {
		return tree.find(k);
	}

	@Override
	public Iterator<V> iterator() {
		return tree.iterator();
	}

	public ImmutableSet<V> add(V v) {
		return new ImmutableSet<V>(tree.add(v));
	}

	public ImmutableSet<V> remove(V v) {
		return new ImmutableSet<V>(tree.remove(v));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");

		for (V v : Util.iter(tree.iterator()))
			sb.append(v + ", ");

		sb.append(")");
		return sb.toString();
	}

}
