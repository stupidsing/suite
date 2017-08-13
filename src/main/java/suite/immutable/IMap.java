package suite.immutable;

import java.util.Iterator;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.streamlet.Streamlet;
import suite.util.Object_;

public class IMap<K extends Comparable<K>, V> implements Iterable<Pair<K, V>> {

	private static IMap<?, ?> empty = new IMap<Integer, Object>();
	private ITree<Pair<K, V>> tree = new IbTree<>(Pair.<K, V> comparatorByFirst());

	public static <K extends Comparable<K>, V> IMap<K, V> empty() {
		@SuppressWarnings("unchecked")
		IMap<K, V> m = (IMap<K, V>) empty;
		return m;
	}

	private IMap() {
	}

	public IMap(ITree<Pair<K, V>> tree) {
		this.tree = tree;
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return streamlet().iterator();
	}

	public Streamlet<Pair<K, V>> streamlet() {
		return tree.streamlet();
	}

	public V get(K k) {
		return Pair.second(tree.find(Pair.of(k, (V) null)));
	}

	public IMap<K, V> put(K k, V v) {
		return new IMap<>(tree.add(Pair.of(k, v)));
	}

	public IMap<K, V> replace(K k, V v) {
		return new IMap<>(tree.replace(Pair.of(k, v)));
	}

	public IMap<K, V> remove(K k) {
		return new IMap<>(tree.remove(Pair.of(k, (V) null)));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == IMap.class && Objects.equals(streamlet(), ((IMap<?, ?>) object).streamlet());
	}

	@Override
	public int hashCode() {
		return tree.streamlet().hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");

		for (Pair<K, V> e : this)
			sb.append(e.t0 + " = " + e.t1 + ", ");

		sb.append("}");
		return sb.toString();
	}

}
