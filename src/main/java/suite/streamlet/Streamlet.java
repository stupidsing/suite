package suite.streamlet;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import suite.adt.Opt;
import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.streamlet.IntObjStreamlet;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
import suite.util.Object_;

public class Streamlet<T> implements StreamletDefaults<T, Outlet<T>> {

	private Source<Outlet<T>> in;

	@SafeVarargs
	public static <T> Streamlet<T> concat(Streamlet<T>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).outlet().source();
			return Outlet.of(FunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <T> Streamlet<T> streamlet(Source<Outlet<T>> in) {
		return new Streamlet<>(in);
	}

	public Streamlet(Source<Outlet<T>> in) {
		this.in = in;
	}

	public <R> Streamlet<R> apply(Fun<Streamlet<T>, R> fun) {
		return Read.each(fun.apply(this));
	}

	public Streamlet<Outlet<T>> chunk(int n) {
		return streamlet(() -> spawn().chunk(n));
	}

	public Streamlet<T> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public Streamlet<T> collect() {
		return Read.from(toList_());
	}

	public <O> Streamlet<O> concatMap(Fun<T, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Fun<T, Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public Streamlet<T> cons(T t) {
		return streamlet(() -> spawn().cons(t));
	}

	public <U, R> Streamlet<R> cross(Streamlet<U> st1, Fun2<T, U, R> fun) {
		return streamlet(() -> spawn().cross(st1.toList_(), fun));
	}

	public Streamlet<T> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public Streamlet<T> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == Streamlet.class ? Objects.equals(spawn(), ((Streamlet<?>) object).spawn()) : false;
	}

	public Streamlet<T> filter(Predicate<T> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public T first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(Fun<T, Iterable<O>> fun) {
		return streamlet(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, Fun2<R, T, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<Streamlet<T>, U> fork0, Fun<Streamlet<T>, V> fork1, Fun2<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <K, V> Streamlet2<K, List<T>> groupBy(Fun<T, K> keyFun) {
		return new Streamlet2<>(() -> spawn().groupBy(keyFun));
	}

	public <K, V1> Streamlet2<K, V1> groupBy(Fun<T, K> keyFun, Fun<Streamlet<T>, V1> fun) {
		return new Streamlet2<>(() -> spawn().groupBy(keyFun, fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public IntObjStreamlet<T> index() {
		return new IntObjStreamlet<>(() -> spawn().index());
	}

	public boolean isAll(Predicate<T> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(Predicate<T> pred) {
		return spawn().isAny(pred);
	}

	@Override
	public Iterator<T> iterator() {
		return spawn().iterator();
	}

	public <O> Streamlet2<T, O> join2(Streamlet<O> streamlet) {
		return concatMap2_(t -> streamlet.map2_(v -> t, v -> v));
	}

	public T last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(Fun<T, O> fun) {
		return map_(fun);
	}

	public <V> Streamlet2<T, V> map2(Fun<T, V> vf) {
		return map2_(k -> k, vf);
	}

	public <K, V> Streamlet2<K, V> map2(Fun<T, K> kf, Fun<T, V> vf) {
		return map2_(kf, vf);
	}

	public T min(Comparator<T> comparator) {
		return spawn().min(comparator);
	}

	public T minOrNull(Comparator<T> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Opt<T> opt() {
		return spawn().opt();
	}

	public Outlet<T> outlet() {
		return spawn();
	}

	public Pair<Streamlet<T>, Streamlet<T>> partition(Predicate<T> pred) {
		return Pair.of(filter(pred), filter(pred.negate()));
	}

	public Streamlet<T> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(Sink<T> sink) {
		spawn().sink(sink);
	}

	public Streamlet<T> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public Streamlet<T> snoc(T t) {
		return streamlet(() -> spawn().snoc(t));
	}

	public Streamlet<T> sort(Comparator<T> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> Streamlet<T> sortBy(Fun<T, O> fun) {
		return streamlet(() -> spawn().sortBy(fun));
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

	public String toLines() {
		return map_(t -> t + "\n").collect(As::joined);
	}

	public List<T> toList() {
		return toList_();
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

	@Override
	public String toString() {
		return map_(t -> "\n" + t).collect(As::joined);
	}

	public T uniqueResult() {
		return spawn().opt().get();
	}

	private <O> Streamlet<O> concatMap_(Fun<T, Streamlet<O>> fun) {
		return streamlet(() -> spawn().concatMap(t -> fun.apply(t).spawn()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Fun<T, Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).outlet()));
	}

	private <O> Streamlet<O> map_(Fun<T, O> fun) {
		return streamlet(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Fun<T, K> kf, Fun<T, V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private List<T> toList_() {
		return spawn().toList();
	}

	private Outlet<T> spawn() {
		return in.source();
	}

}
