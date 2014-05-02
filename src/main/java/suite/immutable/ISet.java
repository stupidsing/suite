package suite.immutable;

import java.util.Iterator;

import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class ISet<V extends Comparable<V>> implements Iterable<V> {

	private ITree<V> tree = new I23Tree<V>(Util.comparator());

	public ISet() {
	}

	public ISet(ITree<V> tree) {
		this.tree = tree;
	}

	@Override
	public Iterator<V> iterator() {
		return FunUtil.iterator(source());
	}

	public Source<V> source() {
		return tree.source();
	}

	public V get(V k) {
		return tree.find(k);
	}

	public ISet<V> add(V v) {
		return new ISet<V>(tree.add(v));
	}

	public ISet<V> replace(V v) {
		return new ISet<V>(tree.replace(v));
	}

	public ISet<V> remove(V v) {
		return new ISet<V>(tree.remove(v));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");

		for (V v : this)
			sb.append(v + ", ");

		sb.append(")");
		return sb.toString();
	}

}
