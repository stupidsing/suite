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
import java.util.function.BiFunction;
import java.util.function.Predicate;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.node.util.Mutable;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSynchronousQueue;
import suite.util.To;
import suite.util.Util;

/**
 * Implement functional structures using class methods (instead of static
 * methods in class FunUtil), just for easier code completion in source editor.
 *
 * @author ywsing
 */
public class Outlet<T> implements Iterable<T> {

	private Source<T> source;

	@SafeVarargs
	public static <T> Outlet<T> concat(Outlet<T>... outlets) {
		List<Source<T>> sources = new ArrayList<>();
		for (Outlet<T> outlet : outlets)
			sources.add(outlet.source);
		return from(FunUtil.concat(To.source(sources)));
	}

	public static <T> Outlet<T> empty() {
		return from(FunUtil.nullSource());
	}

	@SafeVarargs
	public static <T> Outlet<T> from(T... ts) {
		return from(To.source(ts));
	}

	public static <T> Outlet<T> from(Enumeration<T> en) {
		return from(To.source(en));
	}

	public static <T> Outlet<T> from(Iterable<T> col) {
		return from(To.source(col));
	}

	public static <T> Outlet<T> from(Source<T> source) {
		return new Outlet<>(source);
	}

	public Outlet(Source<T> source) {
		this.source = source;
	}

	@Override
	public Iterator<T> iterator() {
		return FunUtil.iterator(source);
	}

	public Outlet<Outlet<T>> chunk(int n) {
		return from(FunUtil.map(Outlet<T>::new, FunUtil.chunk(n, source)));
	}

	public Outlet<T> closeAtEnd(Closeable c) {
		return from(() -> {
			T next = next();
			if (next == null)
				Util.closeQuietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<Outlet<T>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(Fun<T, Outlet<O>> fun) {
		return from(FunUtil.concat(FunUtil.map(t -> fun.apply(t).source, source)));
	}

	public <K, V> Outlet2<K, V> concatMap2(Fun<T, Outlet2<K, V>> fun) {
		return Outlet2.from(FunUtil2.concat(FunUtil.map(t -> fun.apply(t).source2(), source)));
	}

	public Outlet<T> cons(T t) {
		return from(FunUtil.cons(t, source));
	}

	public int count() {
		int i = 0;
		while (next() != null)
			i++;
		return i;
	}

	public <U, R> Outlet<R> cross(List<U> list, BiFunction<T, U, R> fun) {
		return from(new Source<R>() {
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
		Set<T> set = new HashSet<>();
		return from(() -> {
			T t;
			while ((t = next()) != null && !set.add(t))
				;
			return t;
		});
	}

	public Outlet<T> drop(int n) {
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next() != null))
			n--;
		return isAvailable ? this : Outlet.empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Outlet.class) {
			Source<?> source1 = ((Outlet<?>) object).source;
			Object o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == null && o1 == null)
					return true;
			return false;
		} else
			return false;
	}

	public Outlet<T> filter(Predicate<T> fun) {
		return from(FunUtil.filter(fun, source));
	}

	public T first() {
		return next();
	}

	public <R> R fold(R init, BiFunction<R, T, R> fun) {
		T t;
		while ((t = next()) != null)
			init = fun.apply(init, t);
		return init;
	}

	public <K, V> Outlet2<K, List<T>> groupBy(Fun<T, K> keyFun) {
		return groupBy(keyFun, value -> value);
	}

	public <K, V> Outlet2<K, List<V>> groupBy(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return map2(keyFun, valueFun).groupBy();
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		T t;
		while ((t = source.source()) != null)
			hashCode = hashCode * 31 + Objects.hashCode(t);
		return hashCode;
	}

	public Outlet2<Integer, T> index() {
		return Outlet2.from(new Source2<Integer, T>() {
			private int i = 0;

			public boolean source2(Pair<Integer, T> pair) {
				T t = next();
				if (t != null) {
					pair.t0 = i++;
					pair.t1 = t;
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(Predicate<T> pred) {
		return FunUtil.isAll(pred, source);
	}

	public boolean isAny(Predicate<T> pred) {
		return FunUtil.isAny(pred, source);
	}

	public <O> Outlet<O> map(Fun<T, O> fun) {
		return from(FunUtil.map(fun, source));
	}

	public <K, V> Outlet2<K, V> map2(Fun<T, K> kf0, Fun<T, V> vf0) {
		return Outlet2.from(FunUtil.map2(kf0, vf0, source));
	}

	public T min(Comparator<T> comparator) {
		T t = minOrNull(comparator);
		if (t != null)
			return t;
		else
			throw new RuntimeException("No result");
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

	public Outlet<T> nonBlock(T t0) {
		NullableSynchronousQueue<T> queue = new NullableSynchronousQueue<>();

		new Thread(() -> {
			T t;
			do
				queue.offerQuietly(t = source.source());
			while (t != null);
		}).start();

		return new Outlet<>(() -> {
			Mutable<T> mutable = Mutable.nil();
			return queue.poll(mutable) ? mutable.get() : t0;
		});
	}

	public Outlet<T> reverse() {
		return from(Util.reverse(toList()));
	}

	public void sink(Sink<T> sink) {
		T t;
		while ((t = next()) != null)
			sink.sink(t);
	}

	public Outlet<T> skip(int n) {
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next() == null;
		return !end ? from(source) : empty();
	}

	public Source<T> source() {
		return source;
	}

	public Outlet<T> sort(Comparator<T> comparator) {
		return from(Util.sort(toList(), comparator));
	}

	public Outlet<Outlet<T>> split(Predicate<T> fun) {
		return from(FunUtil.map(Outlet<T>::new, FunUtil.split(fun, source)));
	}

	public Outlet<T> take(int n) {
		return from(new Source<T>() {
			private int count = n;

			public T source() {
				return 0 < count-- ? next() : null;
			}
		});
	}

	public List<T> toList() {
		List<T> list = new ArrayList<>();
		T t;
		while ((t = next()) != null)
			list.add(t);
		return list;
	}

	public <K, V> Map<K, List<T>> toListMap(Fun<T, K> keyFun) {
		return toListMap(keyFun, value -> value);
	}

	public <K, V> Map<K, List<V>> toListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, List<V>> map = new HashMap<>();
		T t;
		while ((t = next()) != null)
			map.computeIfAbsent(keyFun.apply(t), k_ -> new ArrayList<>()).add(valueFun.apply(t));
		return map;
	}

	public <K, V> Map<K, T> toMap(Fun<T, K> keyFun) {
		return toMap(keyFun, value -> value);
	}

	public <K, V> Map<K, V> toMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).uniqueResult()).collect(As::map);
	}

	public <K, V> ListMultimap<K, T> toMultimap(Fun<T, K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public Set<T> toSet() {
		Set<T> set = new HashSet<>();
		T t;
		while ((t = next()) != null)
			set.add(t);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).collect(As::map);
	}

	public T uniqueResult() {
		T t = next();
		if (t != null)
			if (next() == null)
				return t;
			else
				throw new RuntimeException("More than one result");
		else
			throw new RuntimeException("No result");
	}

}
