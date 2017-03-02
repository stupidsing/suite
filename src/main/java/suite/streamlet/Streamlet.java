package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

public class Streamlet<T> implements Iterable<T> {

	private Source<Outlet<T>> in;

	@SafeVarargs
	public static <T> Streamlet<T> concat(Streamlet<T>... streamlets) {
		return streamlet(() -> {
			List<Source<T>> sources = new ArrayList<>();
			for (Streamlet<T> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return new Outlet<>(FunUtil.concat(To.source(sources)));
		});
	}

	public static <T> Streamlet<T> from(Source<T> source) {
		return streamlet(() -> new Outlet<>(source));
	}

	private static <T> Streamlet<T> streamlet(Source<Outlet<T>> in) {
		return new Streamlet<>(in);
	}

	public Streamlet(Source<Outlet<T>> in) {
		this.in = in;
	}

	@Override
	public Iterator<T> iterator() {
		return spawn().iterator();
	}

	public <K, V> Streamlet2<K, V> aggregate(Fun<T, K> keyFun, Fun<Streamlet<T>, V> valueFun) {
		return groupBy(keyFun).mapValue(list -> valueFun.apply(Read.from(list)));
	}

	public Streamlet<T> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			Outlet<T> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<Outlet<T>, R> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(Fun<T, Streamlet<O>> fun) {
		return streamlet(() -> spawn().concatMap(t -> fun.apply(t).spawn()));
	}

	public <K, V> Streamlet2<K, V> concatMap2(Fun<T, Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).outlet2()));
	}

	public Streamlet<T> cons(T t) {
		return streamlet(() -> spawn().cons(t));
	}

	public <U, R> Streamlet<R> cross(Streamlet<U> st1, BiFunction<T, U, R> fun) {
		return streamlet(() -> spawn().concatMap(t -> st1.spawn().map(t1 -> fun.apply(t, t1))));
	}

	public Streamlet<T> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public Streamlet<T> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == Streamlet.class ? Objects.equals(spawn(), ((Streamlet<?>) object).spawn()) : false;
	}

	public Streamlet<T> evaluate() {
		return Read.from(toList());
	}

	public Streamlet<T> filter(Predicate<T> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public T first() {
		return spawn().first();
	}

	public <R> R fold(R init, BiFunction<R, T, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<Streamlet<T>, U> fork0, Fun<Streamlet<T>, V> fork1, BiFunction<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <K, V> Streamlet2<K, List<T>> groupBy(Fun<T, K> keyFun) {
		return new Streamlet2<>(() -> spawn().groupBy(keyFun));
	}

	public <K, V> Streamlet2<K, List<V>> groupBy(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return new Streamlet2<>(() -> spawn().groupBy(keyFun, valueFun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public Streamlet2<Integer, T> index() {
		return new Streamlet2<>(() -> spawn().index());
	}

	public boolean isAll(Predicate<T> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(Predicate<T> pred) {
		return spawn().isAny(pred);
	}

	public <O> Streamlet<O> map(Fun<T, O> fun) {
		return streamlet(() -> spawn().map(fun));
	}

	public <K, V> Streamlet2<K, V> map2(Fun<T, K> kf, Fun<T, V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	public Streamlet<T> memoize() {
		List<T> list = toList();
		return streamlet(() -> Outlet.from(list));
	}

	public T min(Comparator<T> comparator) {
		return spawn().min(comparator);
	}

	public T minOrNull(Comparator<T> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Outlet<T> outlet() {
		return spawn();
	}

	public Pair<Streamlet<T>, Streamlet<T>> partition(Predicate<T> pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public Streamlet<T> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(Sink<T> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().count();
	}

	public Streamlet<T> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public Streamlet<T> sort(Comparator<T> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public Source<T> source() {
		return spawn().source();
	}

	public Streamlet<T> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public T[] toArray(Class<T> clazz) {
		return spawn().toArray(clazz);
	}

	public List<T> toList() {
		return spawn().toList();
	}

	public <K> Map<K, List<T>> toListMap(Fun<T, K> keyFun) {
		return spawn().toListMap(keyFun);
	}

	public <K, V> Map<K, List<V>> toListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().toListMap(keyFun, valueFun);
	}

	public <K> Map<K, T> toMap(Fun<T, K> keyFun) {
		return spawn().toMap(keyFun);
	}

	public <K, V> Map<K, V> toMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, T> toMultimap(Fun<T, K> keyFun) {
		return spawn().toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().toMultimap(keyFun, valueFun);
	}

	public Set<T> toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public T uniqueResult() {
		return spawn().uniqueResult();
	}

	private Outlet<T> spawn() {
		return in.source();
	}

}
