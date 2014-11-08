package suite.streamlet;

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

import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.To;
import suite.util.Util;

/**
 * Implement functional structures using class methods (instead of static
 * methods in class FunUtil), just for easier code completion in source editor.
 * 
 * @author ywsing
 */
public class Streamlet<T> implements Iterable<T> {

	private Source<T> source;

	public Streamlet(Source<T> source) {
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

	public <O> Streamlet<O> concatMap(Fun<T, Streamlet<O>> fun) {
		return new Streamlet<>(FunUtil.concat(FunUtil.map(t -> fun.apply(t).source, source)));
	}

	public Streamlet<T> cons(T t) {
		return new Streamlet<>(FunUtil.cons(t, source));
	}

	public int count() {
		int i = 0;
		while (next() != null)
			i++;
		return i;
	}

	public Streamlet<T> distinct() {
		Set<T> set = new HashSet<>();
		return new Streamlet<T>(() -> {
			T t;
			while ((t = next()) != null && !set.add(t))
				;
			return t;
		});
	}

	public Streamlet<T> drop(int n) {
		boolean readable = true;
		while (n > 0 && (readable &= next() != null))
			n--;
		return readable ? this : Read.empty();
	}

	public <R> R fold(R init, BiFunction<T, R, R> fun) {
		T t;
		while ((t = next()) != null)
			init = fun.apply(t, init);
		return init;
	}

	public void foreach(Sink<T> sink) {
		T t;
		while ((t = next()) != null)
			sink.sink(t);
	}

	public <U, R> Streamlet<R> join(List<U> list, BiFunction<T, U, R> fun) {
		return new Streamlet<R>(new Source<R>() {
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

	public <O> Streamlet<O> map(Fun<T, O> fun) {
		return new Streamlet<>(FunUtil.map(fun, source));
	}

	public T min(Comparator<T> comparator) {
		T t = next(), t1;
		if (t != null) {
			while ((t1 = next()) != null)
				if (comparator.compare(t, t1) > 0)
					t = t1;
			return t;
		} else
			throw new RuntimeException("No result");
	}

	public Streamlet<T> filter(Fun<T, Boolean> fun) {
		return new Streamlet<>(FunUtil.filter(fun, source));
	}

	public T next() {
		return source.source();
	}

	public Streamlet<T> sort(Comparator<T> comparator) {
		return new Streamlet<>(To.source(Util.sort(toList(), comparator)));
	}

	public Streamlet<Streamlet<T>> split(Predicate<T> pred) {
		return new Streamlet<Streamlet<T>>(new Source<Streamlet<T>>() {
			private boolean isEnd = false;

			public Streamlet<T> source() {
				return !isEnd ? new Streamlet<T>(() -> {
					T t = next();
					return !(isEnd |= t == null) && !pred.test(t) ? t : null;
				}) : null;
			}
		});
	}

	public Streamlet<T> take(int n) {
		return new Streamlet<T>(new Source<T>() {
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
		while ((t = next()) != null)
			map.put(keyFun.apply(t), valueFun.apply(t));
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
