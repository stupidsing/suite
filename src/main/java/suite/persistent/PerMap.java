package suite.persistent;

import java.util.Iterator;

import primal.Ob;
import primal.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;

public class PerMap<K extends Comparable<K>, V> implements Iterable<Pair<K, V>> {

	private static PerMap<?, ?> empty = new PerMap<Integer, Object>();
	private PerTree<Pair<K, V>> tree = new PbTree<>(Pair.comparatorByFirst());

	public static <K extends Comparable<K>, V> PerMap<K, V> empty() {
		@SuppressWarnings("unchecked")
		var m = (PerMap<K, V>) empty;
		return m;
	}

	private PerMap() {
	}

	public PerMap(PerTree<Pair<K, V>> tree) {
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

	public PerMap<K, V> put(K k, V v) {
		return new PerMap<>(tree.add(Pair.of(k, v)));
	}

	public PerMap<K, V> replace(K k, V v) {
		return new PerMap<>(tree.replace(Pair.of(k, v)));
	}

	public PerMap<K, V> remove(K k) {
		return new PerMap<>(tree.remove(Pair.of(k, (V) null)));
	}

	@Override
	public boolean equals(Object object) {
		return Ob.clazz(object) == PerMap.class && Ob.equals(streamlet(), ((PerMap<?, ?>) object).streamlet());
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
