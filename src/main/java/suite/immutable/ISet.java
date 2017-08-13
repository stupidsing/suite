package suite.immutable;

import java.util.Iterator;
import java.util.Objects;

import suite.streamlet.Streamlet;
import suite.util.Object_;

public class ISet<V extends Comparable<V>> implements Iterable<V> {

	private ITree<V> tree = new IbTree<V>(Object_::compare);

	public ISet() {
	}

	public ISet(ITree<V> tree) {
		this.tree = tree;
	}

	@Override
	public Iterator<V> iterator() {
		return stream().iterator();
	}

	public Streamlet<V> stream() {
		return tree.stream();
	}

	public boolean contains(V v) {
		return tree.find(v) != null;
	}

	public V get(V v) {
		return tree.find(v);
	}

	public ISet<V> add(V v) {
		return new ISet<>(tree.add(v));
	}

	public ISet<V> replace(V v) {
		return new ISet<>(tree.replace(v));
	}

	public ISet<V> remove(V v) {
		return new ISet<>(tree.remove(v));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == ISet.class && Objects.equals(stream(), ((ISet<?>) object).stream());
	}

	@Override
	public int hashCode() {
		return tree.stream().hashCode();
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
