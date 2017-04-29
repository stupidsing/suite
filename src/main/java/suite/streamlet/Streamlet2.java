package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.primitive.PrimitiveFun.ObjObj_Int;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Source2;
import suite.util.To;
import suite.util.Util;

public class Streamlet2<K, V> implements Iterable<Pair<K, V>> {

	private Source<Outlet2<K, V>> in;

	@SafeVarargs
	public static <K, V> Streamlet2<K, V> concat(Streamlet2<K, V>... streamlets) {
		return streamlet2(() -> {
			List<Source2<K, V>> sources = new ArrayList<>();
			for (Streamlet2<K, V> streamlet : streamlets)
				sources.add(streamlet.in.source().source2());
			return Outlet2.of(FunUtil2.concat(To.source(sources)));
		});
	}

	private static <K, V> Streamlet2<K, V> streamlet2(Source<Outlet2<K, V>> in) {
		return new Streamlet2<>(in);
	}

	public Streamlet2(Source<Outlet2<K, V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return spawn().iterator();
	}

	public Streamlet2<K, V> closeAtEnd(Closeable c) {
		return streamlet2(() -> {
			Outlet2<K, V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<Outlet2<K, V>, R> fun) {
		return fun.apply(spawn());
	}

	public <T> Streamlet<T> concatMap(BiFunction<K, V, Streamlet<T>> fun) {
		BiFunction<K, V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(BiFunction<K, V, Streamlet2<K1, V1>> fun) {
		BiFunction<K, V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).outlet2();
		return streamlet2(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	public <V1> Streamlet2<K, V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return streamlet2(() -> Outlet2.of(spawn().concatMapValue(f)));
	}

	public Streamlet2<K, V> cons(K key, V value) {
		return streamlet2(() -> spawn().cons(key, value));
	}

	public Streamlet2<K, V> distinct() {
		return streamlet2(() -> spawn().distinct());
	}

	public Streamlet2<K, V> drop(int n) {
		return streamlet2(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == Streamlet2.class ? Objects.equals(spawn(), ((Streamlet2<?, ?>) object).spawn()) : false;
	}

	public Streamlet2<K, V> filter(BiPredicate<K, V> fun) {
		return streamlet2(() -> spawn().filter(fun));
	}

	public Streamlet2<K, V> filterKey(Predicate<K> fun) {
		return streamlet2(() -> spawn().filterKey(fun));
	}

	public Streamlet2<K, V> filterValue(Predicate<V> fun) {
		return streamlet2(() -> spawn().filterValue(fun));
	}

	public Pair<K, V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(BiFunction<K, V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public Streamlet2<K, List<V>> groupBy() {
		return new Streamlet2<>(() -> spawn().groupBy());
	}

	public <V1> Streamlet2<K, V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return new Streamlet2<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(BiPredicate<K, V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(BiPredicate<K, V> pred) {
		return spawn().isAny(pred);
	}

	public Pair<K, V> last() {
		return spawn().last();
	}

	public <T> Streamlet<T> map(BiFunction<K, V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	public <K1, V1> Streamlet2<K1, V1> map2(BiFunction<K, V, K1> kf, BiFunction<K, V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	public <V1> IntObjStreamlet<V1> mapIntObj(ObjObj_Int<K, V> kf, BiFunction<K, V, V1> vf) {
		return new IntObjStreamlet<>(() -> spawn().mapIntObj(kf, vf));
	}

	public <K1> Streamlet2<K1, V> mapKey(Fun<K, K1> fun) {
		return new Streamlet2<>(() -> spawn().mapKey(fun));
	}

	public <O> Streamlet<O> mapNonNull(BiFunction<K, V, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public <V1> Streamlet2<K, V1> mapValue(Fun<V, V1> fun) {
		return new Streamlet2<>(() -> spawn().mapValue(fun));
	}

	public Pair<K, V> min(Comparator<Pair<K, V>> comparator) {
		return spawn().min(comparator);
	}

	public Pair<K, V> minOrNull(Comparator<Pair<K, V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Outlet2<K, V> outlet2() {
		return spawn();
	}

	public Streamlet<Pair<K, V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<Streamlet2<K, V>, Streamlet2<K, V>> partition(BiPredicate<K, V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public Streamlet2<K, V> reverse() {
		return streamlet2(() -> spawn().reverse());
	}

	public void sink(BiConsumer<K, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public Streamlet2<K, V> skip(int n) {
		return streamlet2(() -> spawn().skip(n));
	}

	public Streamlet2<K, V> sort(Comparator<Pair<K, V>> comparator) {
		return streamlet2(() -> spawn().sort(comparator));
	}

	public Streamlet2<K, V> sortByKey(Comparator<K> comparator) {
		return streamlet2(() -> spawn().sortByKey(comparator));
	}

	public Streamlet2<K, V> sortByValue(Comparator<V> comparator) {
		return streamlet2(() -> spawn().sortByValue(comparator));
	}

	public Source2<K, V> source() {
		return spawn().source2();
	}

	public Streamlet2<K, V> take(int n) {
		return streamlet2(() -> spawn().take(n));
	}

	public Pair<K, V>[] toArray() {
		return spawn().toArray();
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

	public Set<Pair<K, V>> toSet() {
		return spawn().toSet();
	}

	public Map<K, Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public Pair<K, V> uniqueResult() {
		return spawn().uniqueResult();
	}

	private Outlet2<K, V> spawn() {
		return in.source();
	}

}
