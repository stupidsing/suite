package suite.immutable;

import java.util.Iterator;
import java.util.Objects;

import suite.streamlet.Streamlet;
import suite.util.Object_;

public class ISet<V extends Comparable<V>> implements Iterable<V> {

	private static ISet<?> empty = new ISet<Integer>();
	private ITree<V> tree = new IbTree<>(Object_::compare);

	public static <V extends Comparable<V>> ISet<V> empty() {
		@SuppressWarnings("unchecked")
		ISet<V> m = (ISet<V>) empty;
		return m;
	}

	private ISet() {
	}

	public ISet(ITree<V> tree) {
		this.tree = tree;
	}

	@Override
	public Iterator<V> iterator() {
		return streamlet().iterator();
	}

	public Streamlet<V> streamlet() {
		return tree.streamlet();
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
		return Object_.clazz(object) == ISet.class && Objects.equals(streamlet(), ((ISet<?>) object).streamlet());
	}

	@Override
	public int hashCode() {
		return tree.streamlet().hashCode();
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append("(");

		for (var v : this)
			sb.append(v + ", ");

		sb.append(")");
		return sb.toString();
	}

}
