package suite.streamlet;

import java.io.Closeable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import suite.adt.Pair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class Streamlet<T> implements Iterable<T> {

	private Source<Outlet<T>> in;

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

	public <R extends Collection<? super T>> R collect(Source<R> source) {
		return spawn().collect(source);
	}

	public <R> R collect(Pair<Sink<T>, Source<R>> pair) {
		return spawn().collect(pair);
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

	public int count() {
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

	public <R> R fold(R init, BiFunction<R, T, R> fun) {
		return spawn().fold(init, fun);
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

	public T min(Comparator<T> comparator) {
		return spawn().min(comparator);
	}

	public T minOrNull(Comparator<T> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Streamlet<T> filter(Fun<T, Boolean> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public <K, V> Streamlet<Pair<K, List<T>>> groupBy(Fun<T, K> keyFun) {
		return groupBy(keyFun, value -> value);
	}

	public <K, V> Streamlet<Pair<K, List<V>>> groupBy(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return streamlet(() -> spawn().groupBy(keyFun, valueFun));
	}

	public T next() {
		return spawn().next();
	}

	public Outlet<T> outlet() {
		return spawn();
	}

	public Streamlet<T> reverse() {
		return streamlet(() -> spawn().reverse());
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

	public <K, V> Map<K, List<V>> toListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().toListMap(keyFun, valueFun);
	}

	public <K, V> Map<K, V> toMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
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
