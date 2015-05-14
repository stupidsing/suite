package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import suite.adt.Pair;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
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

	private static <T> Outlet<T> empty() {
		return outlet(() -> null);
	}

	private static <T> Outlet<T> outlet(Iterable<T> iter) {
		return outlet(To.source(iter));
	}

	private static <T> Outlet<T> outlet(Source<T> source) {
		return new Outlet<>(source);
	}

	public Outlet(Source<T> source) {
		this.source = source;
	}

	@Override
	public Iterator<T> iterator() {
		return FunUtil.iterator(source);
	}

	public <R extends Collection<? super T>> R collect(Source<R> source) {
		R r = source.source();
		T t;
		while ((t = next()) != null)
			r.add(t);
		return r;
	}

	public <R> R collect(Pair<Sink<T>, Source<R>> pair) {
		T t;
		while ((t = next()) != null)
			pair.t0.sink(t);
		return pair.t1.source();
	}

	public <O> Outlet<O> concatMap(Fun<T, Outlet<O>> fun) {
		return outlet(FunUtil.concat(FunUtil.map(t -> fun.apply(t).source, source)));
	}

	public Outlet<T> closeAtEnd(Closeable c) {
		return outlet(() -> {
			T next = next();
			if (next == null)
				Util.closeQuietly(c);
			return next;
		});
	}

	public Outlet<T> cons(T t) {
		return outlet(FunUtil.cons(t, source));
	}

	public int count() {
		int i = 0;
		while (next() != null)
			i++;
		return i;
	}

	public <U, R> Outlet<R> cross(List<U> list, BiFunction<T, U, R> fun) {
		return outlet(new Source<R>() {
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
		return outlet(() -> {
			T t;
			while ((t = next()) != null && !set.add(t))
				;
			return t;
		});
	}

	public Outlet<T> drop(int n) {
		boolean isAvailable = true;
		while (n > 0 && (isAvailable &= next() != null))
			n--;
		return isAvailable ? this : Outlet.empty();
	}

	public <R> R fold(R init, BiFunction<R, T, R> fun) {
		T t;
		while ((t = next()) != null)
			init = fun.apply(init, t);
		return init;
	}

	public void sink(Sink<T> sink) {
		T t;
		while ((t = next()) != null)
			sink.sink(t);
	}

	public <R> Outlet<R> index(BiFunction<Integer, T, R> fun) {
		return outlet(new Source<R>() {
			private int i = 0;

			public R source() {
				T t = next();
				return t != null ? fun.apply(i++, t) : null;
			}
		});
	}

	public boolean isAll(Predicate<T> pred) {
		T t;
		while ((t = source.source()) != null)
			if (!pred.test(t))
				return false;
		return true;
	}

	public boolean isAny(Predicate<T> pred) {
		T t;
		while ((t = source.source()) != null)
			if (pred.test(t))
				return true;
		return false;
	}

	public <O> Outlet<O> map(Fun<T, O> fun) {
		return outlet(FunUtil.map(fun, source));
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
				if (comparator.compare(t, t1) > 0)
					t = t1;
			return t;
		} else
			return null;
	}

	public Outlet<T> filter(Fun<T, Boolean> fun) {
		return outlet(FunUtil.filter(fun, source));
	}

	public <K, V> Outlet<Pair<K, List<T>>> groupBy(Fun<T, K> keyFun) {
		return groupBy(keyFun, value -> value);
	}

	public <K, V> Outlet<Pair<K, List<V>>> groupBy(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, List<V>> map = new HashMap<>();
		T t;
		while ((t = next()) != null)
			map.computeIfAbsent(keyFun.apply(t), k_ -> new ArrayList<>()).add(valueFun.apply(t));
		return outlet(map.entrySet()).map(e -> Pair.of(e.getKey(), e.getValue()));
	}

	public T next() {
		return source.source();
	}

	public Outlet<T> reverse() {
		return outlet(Util.reverse(toList()));
	}

	public Outlet<T> sort(Comparator<T> comparator) {
		return outlet(Util.sort(toList(), comparator));
	}

	public Outlet<Outlet<T>> split(Fun<T, Boolean> fun) {
		return outlet(FunUtil.map(Outlet<T>::new, FunUtil.split(source, fun)));
	}

	public Outlet<T> take(int n) {
		return outlet(new Source<T>() {
			private int count = n;

			public T source() {
				return count-- > 0 ? next() : null;
			}
		});
	}

	public List<T> toList() {
		return collect(() -> new ArrayList<>());
	}

	public <K, V> Map<K, List<V>> toListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, List<V>> map = new HashMap<>();
		T t;
		while ((t = next()) != null)
			map.computeIfAbsent(keyFun.apply(t), k_ -> new ArrayList<>()).add(valueFun.apply(t));
		return map;
	}

	public <K, V> Map<K, V> toMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, V> map = new HashMap<>();
		T t;
		while ((t = next()) != null) {
			K key = keyFun.apply(t);
			if (map.put(key, valueFun.apply(t)) != null)
				throw new RuntimeException("Duplicate value for key " + key);
		}
		return map;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, Set<V>> map = new HashMap<>();
		T t;
		while ((t = next()) != null)
			map.computeIfAbsent(keyFun.apply(t), k_ -> new HashSet<>()).add(valueFun.apply(t));
		return map;
	}

	public Set<T> toSet() {
		return collect(() -> new HashSet<>());
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
