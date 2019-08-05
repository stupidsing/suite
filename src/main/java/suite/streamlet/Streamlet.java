package suite.streamlet;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Opt;
import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import primal.primitive.IntPrim.IntObjSource;
import primal.primitive.adt.pair.IntObjPair;
import primal.primitive.puller.IntObjPuller;
import primal.puller.Puller;
import primal.streamlet.StreamletDefaults;
import suite.adt.map.ListMultimap;
import suite.primitive.streamlet.IntObjStreamlet;

public class Streamlet<T> implements StreamletDefaults<T, Opt<T>, Predicate<T>, Puller<T>, Sink<T>, Source<T>> {

	private Source<Puller<T>> in;

	@SafeVarargs
	public static <T> Streamlet<T> concat(Streamlet<T>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return Puller.of(FunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <T> Streamlet<T> streamlet(Source<Puller<T>> in) {
		return new Streamlet<>(in);
	}

	public Streamlet(Source<Puller<T>> in) {
		this.in = in;
	}

	public <R> R apply(Fun<Streamlet<T>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<Puller<T>> chunk(int n) {
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
		return Get.clazz(object) == Streamlet.class ? Equals.ab(spawn(), ((Streamlet<?>) object).spawn()) : false;
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
		return new Streamlet2<>(() -> spawn().groupBy(keyFun).mapValue(list -> fun.apply(Read.from(list))));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public IntObjStreamlet<T> index() {
		return new IntObjStreamlet<>(() -> IntObjPuller.of(new IntObjSource<>() {
			private Puller<T> puller = spawn();
			private int i = 0;

			public boolean source2(IntObjPair<T> pair) {
				var t = puller.pull();
				boolean b = t != null;
				if (b)
					pair.update(i++, t);
				return b;
			}
		}));
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

	public Pair<Streamlet<T>, Streamlet<T>> partition(Predicate<T> pred) {
		return Pair.of(filter(pred), filter(pred.negate()));
	}

	public Puller<T> puller() {
		return spawn();
	}

	public Streamlet<T> reverse() {
		return streamlet(() -> spawn().reverse());
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

	public Streamlet<Puller<T>> split(Predicate<T> fun) {
		return streamlet(() -> spawn().split(fun));
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
		return toMap(keyFun, value -> value);
	}

	public <K, V> Map<K, V> toMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).uniqueResult()).toMap();
	}

	public <K> ListMultimap<K, T> toMultimap(Fun<T, K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public Set<T> toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	@Override
	public String toString() {
		return map_(t -> "\n" + t).collect(As::joined);
	}

	public T uniqueResult() {
		return spawn().opt().get();
	}

	public <U, V> Streamlet<V> zip(Iterable<U> list1, Fun2<T, U, V> fun) {
		return streamlet(() -> spawn().zip(Puller.of(list1), fun));
	}

	private <O> Streamlet<O> concatMap_(Fun<T, Streamlet<O>> fun) {
		return streamlet(() -> spawn().concatMap(t -> fun.apply(t).spawn()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Fun<T, Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).puller()));
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

	private Puller<T> spawn() {
		return in.g();
	}

}
