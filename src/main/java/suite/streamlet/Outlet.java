package suite.streamlet;

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

import suite.adt.Mutable;
import suite.adt.Opt;
import suite.adt.map.ListMultimap;
import suite.adt.pair.Fixie;
import suite.adt.pair.Fixie_.FixieA;
import suite.adt.pair.Pair;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.streamlet.IntObjOutlet;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Fun2;
import suite.util.Array_;
import suite.util.Fail;
import suite.util.List_;
import suite.util.NullableSyncQueue;
import suite.util.Object_;
import suite.util.To;

/**
 * Implement functional structures using class methods (instead of static
 * methods in class FunUtil), just for easier code completion in source editor.
 *
 * @author ywsing
 */
public class Outlet<T> implements OutletDefaults<T> {

	private Source<T> source;

	@SafeVarargs
	public static <T> Outlet<T> concat(Outlet<T>... outlets) {
		var sources = new ArrayList<Source<T>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(FunUtil.concat(To.source(sources)));
	}

	public static <T> Outlet<T> empty() {
		return of(FunUtil.nullSource());
	}

	@SafeVarargs
	public static <T> Outlet<T> of(T... ts) {
		return of(To.source(ts));
	}

	public static <T> Outlet<T> of(Enumeration<T> en) {
		return of(To.source(en));
	}

	public static <T> Outlet<T> of(Iterable<T> col) {
		return of(To.source(col));
	}

	public static <T> Outlet<T> of(Source<T> source) {
		return new Outlet<>(source);
	}

	private Outlet(Source<T> source) {
		this.source = source;
	}

	public Outlet<Outlet<T>> chunk(int n) {
		return of(FunUtil.map(Outlet<T>::new, FunUtil.chunk(n, source)));
	}

	public Outlet<T> closeAtEnd(Closeable c) {
		return of(() -> {
			var next = next();
			if (next == null)
				Object_.closeQuietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<Outlet<T>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(Fun<T, Outlet<O>> fun) {
		return of(FunUtil.concat(FunUtil.map(t -> fun.apply(t).source, source)));
	}

	public <K, V> Outlet2<K, V> concatMap2(Fun<T, Outlet2<K, V>> fun) {
		return Outlet2.of(FunUtil2.concat(FunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public Outlet<T> cons(T t) {
		return of(FunUtil.cons(t, source));
	}

	public int count() {
		var i = 0;
		while (next() != null)
			i++;
		return i;
	}

	public <U, R> Outlet<R> cross(List<U> list, Fun2<T, U, R> fun) {
		return of(new Source<>() {
			private T t;
			private int index = list.size();

			public R source() {
				if (index == list.size()) {
					index = 0;
					t = next();
				}
				return fun.apply(t, list.get(index++));
			}
		});
	}

	public Outlet<T> distinct() {
		var set = new HashSet<>();
		return of(() -> {
			T t;
			while ((t = next()) != null && !set.add(t))
				;
			return t;
		});
	}

	public Outlet<T> drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= next() != null))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Outlet.class) {
			var source1 = ((Outlet<?>) object).source;
			Object o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == null && o1 == null)
					return true;
			return false;
		} else
			return false;
	}

	public Outlet<T> filter(Predicate<T> fun) {
		return of(FunUtil.filter(fun, source));
	}

	public T first() {
		return next();
	}

	public <O> Outlet<O> flatMap(Fun<T, Iterable<O>> fun) {
		return of(FunUtil.flatten(FunUtil.map(fun, source)));
	}

	public <R> R fold(R init, Fun2<R, T, R> fun) {
		T t;
		while ((t = next()) != null)
			init = fun.apply(init, t);
		return init;
	}

	public <K, V> Outlet2<K, List<T>> groupBy(Fun<T, K> keyFun) {
		return map2_(keyFun, value -> value).groupBy();
	}

	public <K, V1> Outlet2<K, V1> groupBy(Fun<T, K> keyFun, Fun<Streamlet<T>, V1> fun) {
		return groupBy(keyFun).mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var h = 7;
		T t;
		while ((t = source.source()) != null)
			h = h * 31 + Objects.hashCode(t);
		return h;
	}

	public IntObjOutlet<T> index() {
		return IntObjOutlet.of(new IntObjSource<>() {
			private int i = 0;

			public boolean source2(IntObjPair<T> pair) {
				var t = next();
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
		while ((t = next()) != null)
			t1 = t;
		return t1;
	}

	public <O> Outlet<O> map(Fun<T, O> fun) {
		return of(FunUtil.map(fun, source));
	}

	public <K, V> Outlet2<K, V> map2(Fun<T, K> kf0, Fun<T, V> vf0) {
		return map2_(kf0, vf0);
	}

	public T min(Comparator<T> comparator) {
		var t = minOrNull(comparator);
		return t != null ? t : Fail.t("no result");
	}

	public T minOrNull(Comparator<T> comparator) {
		T t = next(), t1;
		if (t != null) {
			while ((t1 = next()) != null)
				if (0 < comparator.compare(t, t1))
					t = t1;
			return t;
		} else
			return null;
	}

	public T next() {
		return source.source();
	}

	public Outlet<T> nonBlocking(T t0) {
		var queue = new NullableSyncQueue<T>();

		new Thread(() -> {
			T t;
			do
				queue.offerQuietly(t = source.source());
			while (t != null);
		}).start();

		return new Outlet<>(() -> {
			var mutable = Mutable.<T> nil();
			return queue.poll(mutable) ? mutable.get() : t0;
		});
	}

	public Opt<T> opt() {
		var t = next();
		if (t != null)
			return next() == null ? Opt.of(t) : Fail.t("more than one result");
		else
			return Opt.none();
	}

	public Pair<Outlet<T>, Outlet<T>> partition(Predicate<T> pred) {
		return Pair.of(filter(pred), filter(pred.negate()));
	}

	public Outlet<T> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink<T> sink0) {
		var sink1 = sink0.rethrow();
		T t;
		while ((t = next()) != null)
			sink1.sink(t);
	}

	public Outlet<T> skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next() == null;
		return !end ? of(source) : empty();
	}

	public Outlet<T> snoc(T t) {
		return of(FunUtil.snoc(t, source));
	}

	public Source<T> source() {
		return source;
	}

	public Outlet<T> sort(Comparator<T> comparator) {
		return of(List_.sort(toList(), comparator));
	}

	public <O extends Comparable<? super O>> Outlet<T> sortBy(Fun<T, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0), fun.apply(e1)));
	}

	public Outlet<Outlet<T>> split(Predicate<T> fun) {
		return of(FunUtil.map(Outlet<T>::new, FunUtil.split(fun, source)));
	}

	public Outlet<T> take(int n) {
		return of(new Source<>() {
			private int count = n;

			public T source() {
				return 0 < count-- ? next() : null;
			}
		});
	}

	public T[] toArray(Class<T> clazz) {
		var list = toList();
		var array = Array_.newArray(clazz, list.size());
		return list.toArray(array);
	}

	public FixieA<T, T, T, T, T, T, T, T, T, T> toFixie() {
		var t0 = next();
		var t1 = t0 != null ? next() : null;
		var t2 = t1 != null ? next() : null;
		var t3 = t2 != null ? next() : null;
		var t4 = t3 != null ? next() : null;
		var t5 = t4 != null ? next() : null;
		var t6 = t5 != null ? next() : null;
		var t7 = t6 != null ? next() : null;
		var t8 = t7 != null ? next() : null;
		var t9 = t8 != null ? next() : null;
		return Fixie.of(t0, t1, t2, t3, t4, t5, t6, t7, t8, t9);
	}

	public List<T> toList() {
		var list = new ArrayList<T>();
		T t;
		while ((t = next()) != null)
			list.add(t);
		return list;
	}

	public <K, V> Map<K, List<T>> toListMap(Fun<T, K> keyFun) {
		return toListMap(keyFun, value -> value);
	}

	public <K, V> Map<K, List<V>> toListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		var map = new HashMap<K, List<V>>();
		T t;
		while ((t = next()) != null)
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
		while ((t = next()) != null)
			set.add(t);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	private <K, V> Outlet2<K, V> map2_(Fun<T, K> kf0, Fun<T, V> vf0) {
		return Outlet2.of(FunUtil.map2(kf0, vf0, source));
	}

}
