package suite.streamlet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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

/**
 * Implement functional structures using class methods (instead of static
 * methods in class FunUtil), just for easier code completion in source editor.
 * 
 * @author ywsing
 */
public class Streamlet<T> implements Iterable<T> {

	private Source<T> source;

	public static <T> Streamlet<T> empty() {
		return new Streamlet<>(() -> null);
	}

	public Streamlet(Source<T> source) {
		this.source = source;
	}

	@Override
	public Iterator<T> iterator() {
		return FunUtil.iterator(source);
	}

	public <R> R collect(Pair<Sink<T>, Source<R>> pair) {
		T t;
		while ((t = source.source()) != null)
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
		while (source.source() != null)
			i++;
		return i;
	}

	public <R> R fold(BiFunction<T, R, R> fun, R init) {
		T t;
		while ((t = source.source()) != null)
			init = fun.apply(t, init);
		return init;
	}

	public void foreach(Sink<T> sink) {
		T t;
		while ((t = source.source()) != null)
			sink.sink(t);
	}

	public <O> Streamlet<O> map(Fun<T, O> fun) {
		return new Streamlet<>(FunUtil.map(fun, source));
	}

	public T min(Comparator<T> comparator) {
		T t = source.source(), t1;
		if (t != null) {
			while ((t1 = source.source()) != null)
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

	public Streamlet<Streamlet<T>> split(Predicate<T> pred) {
		return new Streamlet<Streamlet<T>>(new Source<Streamlet<T>>() {
			private boolean isEnd = false;

			public Streamlet<T> source() {
				return !isEnd ? new Streamlet<T>(() -> {
					T t = source.source();
					return !(isEnd |= t == null) && !pred.test(t) ? t : null;
				}) : null;
			}
		});
	}

	public List<T> toList() {
		return collect(As.list());
	}

	public <K, V> Map<K, List<V>> toListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, List<V>> map = new HashMap<>();
		T t;
		while ((t = source.source()) != null)
			map.computeIfAbsent(keyFun.apply(t), k_ -> new ArrayList<>()).add(valueFun.apply(t));
		return map;
	}

	public <K, V> Map<K, V> toMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, V> map = new HashMap<>();
		T t;
		while ((t = source.source()) != null)
			map.put(keyFun.apply(t), valueFun.apply(t));
		return map;
	}

	public Set<T> toSet() {
		return collect(As.set());
	}

	public T uniqueResult() {
		T t = source.source();
		if (t != null)
			if (source.source() == null)
				return t;
			else
				throw new RuntimeException("More than one result");
		else
			throw new RuntimeException("No result");
	}

}
