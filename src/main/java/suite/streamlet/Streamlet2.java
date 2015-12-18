package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Source2;
import suite.util.To;

public class Streamlet2<K, V> implements Iterable<Pair<K, V>> {

	private Source<Outlet2<K, V>> in;

	@SafeVarargs
	public static <K, V> Streamlet2<K, V> concat(Streamlet2<K, V>... streamlets) {
		return streamlet(() -> {
			List<Source2<K, V>> sources = new ArrayList<>();
			for (Streamlet2<K, V> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return new Outlet2<>(FunUtil2.concat(To.source(sources)));
		});
	}

	public static <K, V> Streamlet2<K, V> from(Source2<K, V> source) {
		return streamlet(() -> new Outlet2<>(source));
	}

	private static <K, V> Streamlet2<K, V> streamlet(Source<Outlet2<K, V>> in) {
		return new Streamlet2<>(in);
	}

	public Streamlet2(Source<Outlet2<K, V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return spawn().iterator();
	}

	public <V1> Streamlet2<K, V1> aggregate(Fun<Streamlet<V>, V1> valueFun) {
		return groupBy().mapValue(list -> valueFun.apply(Read.from(list)));
	}

	public <R> R collect(Fun<Outlet2<K, V>, R> fun) {
		return fun.apply(in.source());
	}

	public <T> Streamlet<T> concatMap(BiFunction<K, V, Streamlet<T>> fun) {
		BiFunction<K, V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.from(spawn().concatMap(bf)));
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(BiFunction<K, V, Streamlet2<K1, V1>> fun) {
		BiFunction<K, V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return streamlet(() -> Outlet2.from(spawn().concatMap2(bf)));
	}

	public Streamlet2<K, V> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			Outlet2<K, V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public Streamlet2<K, V> cons(K key, V value) {
		return streamlet(() -> spawn().cons(key, value));
	}

	public int size() {
		return spawn().count();
	}

	public Streamlet2<K, V> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public Streamlet2<K, V> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return object.getClass() == Streamlet2.class ? Objects.equals(spawn(), ((Streamlet2<?, ?>) object).spawn()) : false;
	}

	public <R extends Collection<Pair<K, V>>> R form(Source<R> source) {
		return spawn().form(source);
	}

	public void sink(BiConsumer<K, V> sink) {
		spawn().sink(sink);
	}

	public boolean isAll(BiPredicate<K, V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(BiPredicate<K, V> pred) {
		return spawn().isAny(pred);
	}

	public <T> Streamlet<T> map(BiFunction<K, V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	public <K1> Streamlet2<K1, V> mapKey(Fun<K, K1> fun) {
		return new Streamlet2<>(() -> spawn().mapKey(fun));
	}

	public <V1> Streamlet2<K, V1> mapValue(Fun<V, V1> fun) {
		return new Streamlet2<>(() -> spawn().mapValue(fun));
	}

	public <K1, V1> Streamlet2<K1, V1> mapKeyValue(Fun<K, K1> kf, Fun<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().mapKeyValue(kf, vf));
	}

	public Pair<K, V> min(Comparator<Pair<K, V>> comparator) {
		return spawn().min(comparator);
	}

	public Pair<K, V> minOrNull(Comparator<Pair<K, V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Streamlet2<K, V> filter(BiPredicate<K, V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public Pair<K, V> first() {
		Pair<K, V> pair = Pair.of(null, null);
		return spawn().next(pair) ? pair : null;
	}

	public Streamlet2<K, List<V>> groupBy() {
		return new Streamlet2<>(() -> spawn().groupBy());
	}

	public Outlet2<K, V> outlet() {
		return spawn();
	}

	public Streamlet2<K, V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public Streamlet2<K, V> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public Source2<K, V> source() {
		return spawn().source();
	}

	public Streamlet2<K, V> sort(Comparator<Pair<K, V>> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public Streamlet2<K, V> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public List<Pair<K, V>> toList() {
		return spawn().toList();
	}

	public Map<K, List<V>> toListMap() {
		return spawn().toListMap();
	}

	public Map<K, V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<K, V> toMultimap() {
		return spawn().toMultimap();
	}

	public Map<K, Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public Set<Pair<K, V>> toSet() {
		return spawn().toSet();
	}

	public Pair<K, V> uniqueResult() {
		return spawn().uniqueResult();
	}

	private Outlet2<K, V> spawn() {
		return in.source();
	}

}
