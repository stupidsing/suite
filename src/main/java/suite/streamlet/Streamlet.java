package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.primitive.PrimitiveFun.Obj_Double;
import suite.primitive.PrimitiveFun.Obj_Float;
import suite.primitive.PrimitiveFun.Obj_Int;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class Streamlet<T> implements Iterable<T> {

	private Source<Outlet<T>> in;

	@SafeVarargs
	public static <T> Streamlet<T> concat(Streamlet<T>... streamlets) {
		return streamlet(() -> {
			List<Source<T>> sources = new ArrayList<>();
			for (Streamlet<T> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return Outlet.of(FunUtil.concat(To.source(sources)));
		});
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

	public Streamlet<T> append(T t) {
		return streamlet(() -> spawn().append(t));
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

	public double collectAsDouble(Obj_Double<Outlet<T>> fun) {
		return fun.applyAsDouble(spawn());
	}

	public float collectAsFloat(Obj_Float<Outlet<T>> fun) {
		return fun.applyAsFloat(spawn());
	}

	public int collectAsInt(Obj_Int<Outlet<T>> fun) {
		return fun.applyAsInt(spawn());
	}

	public <O> Streamlet<O> concatMap(Fun<T, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Fun<T, Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public <O> IntObjStreamlet<O> concatMapIntObj(Fun<T, IntObjStreamlet<O>> fun) {
		return concatMapIntObj_(fun);
	}

	public Streamlet<T> cons(T t) {
		return streamlet(() -> spawn().cons(t));
	}

	public <U, R> Streamlet<R> cross(Streamlet<U> st1, ObjObj_Obj<T, U, R> fun) {
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

	public <R> R fold(R init, ObjObj_Obj<R, T, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<Streamlet<T>, U> fork0, Fun<Streamlet<T>, V> fork1, ObjObj_Obj<U, V, W> join) {
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

	public Streamlet2<Integer, T> index() {
		return new Streamlet2<>(() -> spawn().index());
	}

	public boolean isAll(Predicate<T> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(Predicate<T> pred) {
		return spawn().isAny(pred);
	}

	public <O> Streamlet2<T, O> join2(Streamlet<O> streamlet) {
		return concatMap2_(t -> streamlet.map2_(v -> t, v -> v));
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

	public <V1> IntObjStreamlet<V1> mapIntObj(Obj_Int<T> kf, Fun<T, V1> vf) {
		return mapIntObj_(kf, vf);
	}

	public <O> Streamlet<O> mapNonNull(Fun<T, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public T last() {
		return spawn().last();
	}

	public Streamlet<T> memoize() {
		List<T> list = toList();
		return streamlet(() -> Outlet.of(list));
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

	private <O> Streamlet<O> concatMap_(Fun<T, Streamlet<O>> fun) {
		return streamlet(() -> spawn().concatMap(t -> fun.apply(t).spawn()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Fun<T, Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).out()));
	}

	private <O> IntObjStreamlet<O> concatMapIntObj_(Fun<T, IntObjStreamlet<O>> fun) {
		return new IntObjStreamlet<>(() -> spawn().concatMapIntObj(t -> fun.apply(t).out()));
	}

	private <O> Streamlet<O> map_(Fun<T, O> fun) {
		return streamlet(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Fun<T, K> kf, Fun<T, V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> IntObjStreamlet<V1> mapIntObj_(Obj_Int<T> kf, Fun<T, V1> vf) {
		return new IntObjStreamlet<>(() -> spawn().mapIntObj(kf, vf));
	}

	private Outlet<T> spawn() {
		return in.source();
	}

}
