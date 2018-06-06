package suite.immutable;

import java.util.Iterator;
import java.util.Objects;

import suite.adt.pair.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.util.Object_;

public class IMap<K extends Comparable<K>, V> implements Iterable<Pair<K, V>> {

	private static IMap<?, ?> empty = new IMap<Integer, Object>();
	private ITree<Pair<K, V>> tree = new IbTree<>(Pair.comparatorByFirst());

	public static <K extends Comparable<K>, V> IMap<K, V> empty() {
		@SuppressWarnings("unchecked")
		var m = (IMap<K, V>) empty;
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

	public Streamlet2<K, V> streamlet2() {
		return tree.streamlet().map2(Pair::fst, Pair::snd);
	}

	public V get(K k) {
		return Pair.snd(tree.find(Pair.of(k, (V) null)));
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
		return Read.from(this).map(e -> e + ", ").collect(As.joinedBy("{ ", "", "}"));
	}

}
