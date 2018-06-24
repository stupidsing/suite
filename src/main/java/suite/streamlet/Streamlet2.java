package suite.streamlet;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Pair;
import suite.util.Fail;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Fun2;
import suite.util.FunUtil2.Sink2;
import suite.util.FunUtil2.Source2;
import suite.util.Object_;

public class Streamlet2<K, V> implements StreamletDefaults<Pair<K, V>, Outlet2<K, V>> {

	private Source<Outlet2<K, V>> in;

	@SafeVarargs
	public static <K, V> Streamlet2<K, V> concat(Streamlet2<K, V>... streamlets) {
		return streamlet2(() -> {
			var source = Read.from(streamlets).outlet().source();
			return Outlet2.of(FunUtil2.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <K, V> Streamlet2<K, V> streamlet2(Source<Outlet2<K, V>> in) {
		return new Streamlet2<>(in);
	}

	public Streamlet2(Source<Outlet2<K, V>> in) {
		this.in = in;
	}

	public <T> Streamlet<T> apply(Fun<Streamlet2<K, V>, T> fun) {
		return Read.each(fun.apply(this));
	}

	public Streamlet<Outlet2<K, V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public Streamlet2<K, V> closeAtEnd(Closeable c) {
		return streamlet2(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public Streamlet2<K, V> collect() {
		return Read.from2(toList_());
	}

	public <O> Streamlet<O> concatMap(Fun2<K, V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(Fun2<K, V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> Streamlet2<K, V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return streamlet2(() -> Outlet2.of(spawn().concatMapValue(f)));
	}

	public Streamlet2<K, V> cons(K key, V value) {
		return cons_(key, value);
	}

	public Streamlet2<K, V> cons(Pair<K, V> pair) {
		return pair.map(this::cons_);
	}

	public Streamlet2<K, V> distinct() {
		return streamlet2(() -> spawn().distinct());
	}

	public Streamlet2<K, V> drop(int n) {
		return streamlet2(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == Streamlet2.class ? Objects.equals(spawn(), ((Streamlet2<?, ?>) object).spawn()) : false;
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

	public <O> Streamlet<O> flatMap(Fun2<K, V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, FixieFun3<R, K, V, R> fun) {
		return spawn().fold(init, fun);
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

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return spawn().iterator();
	}

	public Streamlet<K> keys() {
		return new Streamlet<>(() -> spawn().keys());
	}

	public Pair<K, V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(Fun2<K, V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<K, V1> map2(Fun2<K, V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(Fun2<K, V, K1> kf, Fun2<K, V, V1> vf) {
		return map2_(kf, vf);
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

	public Pair<K, V> opt() {
		return spawn().opt();
	}

	public Outlet2<K, V> outlet() {
		return spawn();
	}

	public Streamlet<Pair<K, V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<Streamlet2<K, V>, Streamlet2<K, V>> partition(BiPredicate<K, V> pred) {
		return Pair.of(filter(pred), filter(pred.negate()));
	}

	public Streamlet2<K, V> reverse() {
		return streamlet2(() -> spawn().reverse());
	}

	public void sink(Sink2<K, V> sink) {
		spawn().sink(sink);
	}

	public Streamlet2<K, V> skip(int n) {
		return streamlet2(() -> spawn().skip(n));
	}

	public Streamlet2<K, V> snoc(K key, V value) {
		return streamlet2(() -> spawn().snoc(key, value));
	}

	public Streamlet2<K, V> sort(Comparator<Pair<K, V>> comparator) {
		return streamlet2(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> Streamlet2<K, V> sortBy(Fun2<K, V, O> fun) {
		return streamlet2(() -> spawn().sortBy(fun));
	}

	public Streamlet2<K, V> sortByKey(Comparator<K> comparator) {
		return streamlet2(() -> spawn().sortByKey(comparator));
	}

	public Streamlet2<K, V> sortByValue(Comparator<V> comparator) {
		return streamlet2(() -> spawn().sortByValue(comparator));
	}

	public Source2<K, V> source() {
		return spawn().source();
	}

	public Streamlet2<K, V> take(int n) {
		return streamlet2(() -> spawn().take(n));
	}

	public Pair<K, V>[] toArray() {
		return spawn().toArray();
	}

	public List<Pair<K, V>> toList() {
		return toList_();
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
		var pair = spawn().opt();
		return pair.t0 != null ? pair : Fail.t("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(Fun2<K, V, Streamlet<T>> fun) {
		Fun2<K, V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <K1, V1> Streamlet2<K1, V1> concatMap2_(Fun2<K, V, Streamlet2<K1, V1>> fun) {
		Fun2<K, V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return streamlet2(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private Streamlet2<K, V> cons_(K key, V value) {
		return streamlet2(() -> spawn().cons(key, value));
	}

	private <T> Streamlet<T> map_(Fun2<K, V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(Fun2<K, V, K1> kf, Fun2<K, V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private List<Pair<K, V>> toList_() {
		return spawn().toList();
	}

	private Outlet2<K, V> spawn() {
		return in.source();
	}

}
