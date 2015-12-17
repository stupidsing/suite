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
import java.util.function.BiFunction;
import java.util.function.Predicate;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.To;

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

	public <K, V> Streamlet<Pair<K, V>> aggregate(Fun<T, K> keyFun, Fun<Streamlet<T>, V> valueFun) {
		return groupBy(keyFun).map(Pair.map1(list -> valueFun.apply(Read.from(list))));
	}

	public <R> R collect(Fun<Outlet<T>, R> fun) {
		return fun.apply(in.source());
	}

	public <O> Streamlet<O> concatMap(Fun<T, Streamlet<O>> fun) {
		return streamlet(() -> spawn().concatMap(t -> fun.apply(t).spawn()));
	}

	public Streamlet<T> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			Outlet<T> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public Streamlet<T> cons(T t) {
		return streamlet(() -> spawn().cons(t));
	}

	public int size() {
		return spawn().count();
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
		return object.getClass() == Streamlet.class ? Objects.equals(spawn(), ((Streamlet<?>) object).spawn()) : false;
	}

	public <R> R fold(R init, BiFunction<R, T, R> fun) {
		return spawn().fold(init, fun);
	}

	public <R extends Collection<? super T>> R form(Source<R> source) {
		return spawn().form(source);
	}

	public void sink(Sink<T> sink) {
		spawn().sink(sink);
	}

	public <R> Streamlet<R> index(BiFunction<Integer, T, R> fun) {
		return streamlet(() -> spawn().index(fun));
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

	public T min(Comparator<T> comparator) {
		return spawn().min(comparator);
	}

	public T minOrNull(Comparator<T> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Streamlet<T> filter(Fun<T, Boolean> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public T first() {
		return spawn().next();
	}

	public <K, V> Streamlet<Pair<K, List<T>>> groupBy(Fun<T, K> keyFun) {
		return groupBy(keyFun, value -> value);
	}

	public <K, V> Streamlet<Pair<K, List<V>>> groupBy(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return streamlet(() -> spawn().groupBy(keyFun, valueFun));
	}

	public Outlet<T> outlet() {
		return spawn();
	}

	public Streamlet<T> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public Streamlet<T> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public Source<T> source() {
		return spawn().source();
	}

	public Streamlet<T> sort(Comparator<T> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public Streamlet<T> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public List<T> toList() {
		return spawn().toList();
	}

	public <K> Map<K, List<T>> toListMap(Fun<T, K> keyFun) {
		return spawn().toListMap(keyFun, value -> value);
	}

	public <K, V> Map<K, List<V>> toListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().toListMap(keyFun, valueFun);
	}

	public <K> Map<K, T> toMap(Fun<T, K> keyFun) {
		return spawn().toMap(keyFun, value -> value);
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

	public <K, V> Map<K, Set<V>> toSetMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public Set<T> toSet() {
		return spawn().toSet();
	}

	public T uniqueResult() {
		return spawn().uniqueResult();
	}

	private Outlet<T> spawn() {
		return in.source();
	}

}
