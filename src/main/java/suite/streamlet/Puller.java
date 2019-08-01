package suite.streamlet;

import static primal.statics.Fail.fail;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import primal.NullableSyncQueue;
import primal.Verbs.Close;
import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.Verbs.New;
import primal.Verbs.Reverse;
import primal.Verbs.Sort;
import primal.adt.Fixie;
import primal.adt.Fixie_.FixieA;
import primal.adt.Mutable;
import primal.adt.Opt;
import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.FunUtil2;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import primal.primitive.IntPrim.IntObjSource;
import primal.primitive.adt.pair.IntObjPair;
import primal.streamlet.PullerDefaults;
import suite.adt.map.ListMultimap;
import suite.primitive.streamlet.IntObjPuller;
import suite.util.To;

/**
 * Implement functional structures using class methods (instead of static
 * methods in class FunUtil), just for easier code completion in source editor.
 *
 * @author ywsing
 */
public class Puller<T> implements PullerDefaults<T> {

	private Source<T> source;

	@SafeVarargs
	public static <T> Puller<T> concat(Puller<T>... outlets) {
		var sources = new ArrayList<Source<T>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(FunUtil.concat(To.source(sources)));
	}

	public static <T> Puller<T> empty() {
		return of(FunUtil.nullSource());
	}

	@SafeVarargs
	public static <T> Puller<T> of(T... ts) {
		return of(To.source(ts));
	}

	public static <T> Puller<T> of(Enumeration<T> en) {
		return of(To.source(en));
	}

	public static <T> Puller<T> of(Iterable<T> col) {
		return of(To.source(col));
	}

	public static <T> Puller<T> of(Source<T> source) {
		return new Puller<>(source);
	}

	private Puller(Source<T> source) {
		this.source = source;
	}

	public Puller<Puller<T>> chunk(int n) {
		return of(FunUtil.map(Puller<T>::new, FunUtil.chunk(n, source)));
	}

	public Puller<T> closeAtEnd(Closeable c) {
		return of(() -> {
			var next = pull();
			if (next == null)
				Close.quietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<Puller<T>, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(Fun<T, Puller<O>> fun) {
		return of(FunUtil.concat(FunUtil.map(t -> fun.apply(t).source, source)));
	}

	public <K, V> Puller2<K, V> concatMap2(Fun<T, Puller2<K, V>> fun) {
		return Puller2.of(FunUtil2.concat(FunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public Puller<T> cons(T t) {
		return of(FunUtil.cons(t, source));
	}

	public int count() {
		var i = 0;
		while (pull() != null)
			i++;
		return i;
	}

	public <U, R> Puller<R> cross(List<U> list, Fun2<T, U, R> fun) {
		return of(new Source<>() {
			private T t;
			private int index = list.size();

			public R g() {
				if (index == list.size()) {
					index = 0;
					t = pull();
				}
				return fun.apply(t, list.get(index++));
			}
		});
	}

	public Puller<T> distinct() {
		var set = new HashSet<>();
		return of(() -> {
			T t;
			while ((t = pull()) != null && !set.add(t))
				;
			return t;
		});
	}

	public Puller<T> drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull() != null))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == Puller.class) {
			var source1 = ((Puller<?>) object).source;
			Object o0, o1;
			while (Equals.ab(o0 = source.g(), o1 = source1.g()))
				if (o0 == null && o1 == null)
					return true;
			return false;
		} else
			return false;
	}

	public Puller<T> filter(Predicate<T> fun) {
		return of(FunUtil.filter(fun, source));
	}

	public T first() {
		return pull();
	}

	public <O> Puller<O> flatMap(Fun<T, Iterable<O>> fun) {
		return of(FunUtil.flatten(FunUtil.map(fun, source)));
	}

	public <R> R fold(R init, Fun2<R, T, R> fun) {
		T t;
		while ((t = pull()) != null)
			init = fun.apply(init, t);
		return init;
	}

	public <K, V> Puller2<K, List<T>> groupBy(Fun<T, K> keyFun) {
		return map2_(keyFun, value -> value).groupBy();
	}

	public <K, V1> Puller2<K, V1> groupBy(Fun<T, K> keyFun, Fun<Streamlet<T>, V1> fun) {
		return groupBy(keyFun).mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var h = 7;
		T t;
		while ((t = source.g()) != null)
			h = h * 31 + Objects.hashCode(t);
		return h;
	}

	public IntObjPuller<T> index() {
		return IntObjPuller.of(new IntObjSource<>() {
			private int i = 0;

			public boolean source2(IntObjPair<T> pair) {
				var t = pull();
				boolean b = t != null;
				if (b)
					pair.update(i++, t);
				return b;
			}
		});
	}

	public boolean isAll(Predicate<T> pred) {
		return FunUtil.isAll(pred, source);
	}

	public boolean isAny(Predicate<T> pred) {
		return FunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<T> iterator() {
		return FunUtil.iterator(source);
	}

	public T last() {
		T t, t1 = null;
		while ((t = pull()) != null)
			t1 = t;
		return t1;
	}

	public <O> Puller<O> map(Fun<T, O> fun) {
		return of(FunUtil.map(fun, source));
	}

	public <K, V> Puller2<K, V> map2(Fun<T, K> kf0, Fun<T, V> vf0) {
		return map2_(kf0, vf0);
	}

	public T min(Comparator<T> comparator) {
		var t = minOrNull(comparator);
		return t != null ? t : fail("no result");
	}

	public T minOrNull(Comparator<T> comparator) {
		T t = pull(), t1;
		if (t != null) {
			while ((t1 = pull()) != null)
				if (0 < comparator.compare(t, t1))
					t = t1;
			return t;
		} else
			return null;
	}

	public Puller<T> nonBlocking(T t0) {
		var queue = new NullableSyncQueue<T>();

		new Thread(() -> {
			T t;
			do
				queue.offerQuietly(t = source.g());
			while (t != null);
		}).start();

		return new Puller<>(() -> {
			var mutable = Mutable.<T> nil();
			return queue.poll(mutable) ? mutable.value() : t0;
		});
	}

	public Opt<T> opt() {
		var t = pull();
		if (t != null)
			return pull() == null ? Opt.of(t) : fail("more than one result");
		else
			return Opt.none();
	}

	public Pair<Puller<T>, Puller<T>> partition(Predicate<T> pred) {
		return Pair.of(filter(pred), filter(pred.negate()));
	}

	public T pull() {
		return source.g();
	}

	public Puller<T> reverse() {
		return of(Reverse.of(toList()));
	}

	public void sink(Sink<T> sink0) {
		var sink1 = sink0.rethrow();
		T t;
		while ((t = pull()) != null)
			sink1.f(t);
	}

	public Puller<T> skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull() == null;
		return !end ? of(source) : empty();
	}

	public Puller<T> snoc(T t) {
		return of(FunUtil.snoc(t, source));
	}

	public Source<T> source() {
		return source;
	}

	public Puller<T> sort(Comparator<T> comparator) {
		return of(Sort.list(toList(), comparator));
	}

	public <O extends Comparable<? super O>> Puller<T> sortBy(Fun<T, O> fun) {
		return sort((e0, e1) -> Compare.objects(fun.apply(e0), fun.apply(e1)));
	}

	public Puller<Puller<T>> split(Predicate<T> fun) {
		return of(FunUtil.map(Puller<T>::new, FunUtil.split(fun, source)));
	}

	public Puller<T> take(int n) {
		return of(new Source<>() {
			private int count = n;

			public T g() {
				return 0 < count-- ? pull() : null;
			}
		});
	}

	public T[] toArray(Class<T> clazz) {
		var list = toList();
		var array = New.array(clazz, list.size());
		return list.toArray(array);
	}

	public FixieA<T, T, T, T, T, T, T, T, T, T> toFixie() {
		var t0 = pull();
		var t1 = t0 != null ? pull() : null;
		var t2 = t1 != null ? pull() : null;
		var t3 = t2 != null ? pull() : null;
		var t4 = t3 != null ? pull() : null;
		var t5 = t4 != null ? pull() : null;
		var t6 = t5 != null ? pull() : null;
		var t7 = t6 != null ? pull() : null;
		var t8 = t7 != null ? pull() : null;
		var t9 = t8 != null ? pull() : null;
		return Fixie.of(t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}

	public List<T> toList() {
		var list = new ArrayList<T>();
		T t;
		while ((t = pull()) != null)
			list.add(t);
		return list;
	}

	public <K, V> Map<K, List<T>> toListMap(Fun<T, K> keyFun) {
		return toListMap(keyFun, value -> value);
	}

	public <K, V> Map<K, List<V>> toListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		var map = new HashMap<K, List<V>>();
		T t;
		while ((t = pull()) != null)
			map.computeIfAbsent(keyFun.apply(t), k_ -> new ArrayList<>()).add(valueFun.apply(t));
		return map;
	}

	public <K, V> Map<K, T> toMap(Fun<T, K> keyFun) {
		return toMap(keyFun, value -> value);
	}

	public <K, V> Map<K, V> toMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).uniqueResult()).toMap();
	}

	public <K, V> ListMultimap<K, T> toMultimap(Fun<T, K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public Set<T> toSet() {
		var set = new HashSet<T>();
		T t;
		while ((t = pull()) != null)
			set.add(t);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public <U, R> Puller<R> zip(Puller<U> outlet1, Fun2<T, U, R> fun) {
		return of(() -> {
			var t = pull();
			var u = outlet1.pull();
			return t != null && u != null ? fun.apply(t, u) : null;
		});
	}

	private <K, V> Puller2<K, V> map2_(Fun<T, K> kf0, Fun<T, V> vf0) {
		return Puller2.of(FunUtil.map2(kf0, vf0, source));
	}

}
