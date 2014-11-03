package suite.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

/**
 * Implement functional structures using class methods (instead of static
 * methods in class FunUtil), just for easier code completion in source editor.
 * 
 * @author ywsing
 */
public class Streamlet<T> implements Iterable<T> {

	private Source<T> source;

	@SafeVarargs
	public Streamlet(T... array) {
		this(Arrays.asList(array));
	}

	public Streamlet(Collection<T> col) {
		this(To.source(col));
	}

	public static <T> Streamlet<T> empty() {
		return Streamlet.of(() -> null);
	}

	@SafeVarargs
	public static <T> Streamlet<T> of(T... array) {
		return Streamlet.of(Arrays.asList(array));
	}

	public static <T> Streamlet<T> of(Collection<T> col) {
		return Streamlet.of(To.source(col));
	}

	public static <T> Streamlet<T> of(Source<T> source) {
		return new Streamlet<>(source);
	}

	private Streamlet(Source<T> source) {
		this.source = source;
	}

	@Override
	public Iterator<T> iterator() {
		return FunUtil.iterator(source);
	}

	public List<T> asList() {
		List<T> list = new ArrayList<>();
		T t;
		while ((t = source.source()) != null)
			list.add(t);
		return list;
	}

	public <K> Map<K, List<T>> asListMap(Fun<T, K> keyFun) {
		return asListMap(keyFun, t -> t);
	}

	public <K, V> Map<K, List<V>> asListMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, List<V>> map = new HashMap<>();
		T t;
		while ((t = source.source()) != null)
			map.computeIfAbsent(keyFun.apply(t), k_ -> new ArrayList<>()).add(valueFun.apply(t));
		return map;
	}

	public <K> Map<K, T> asMap(Fun<T, K> keyFun) {
		return asMap(keyFun, t -> t);
	}

	public <K, V> Map<K, V> asMap(Fun<T, K> keyFun, Fun<T, V> valueFun) {
		Map<K, V> map = new HashMap<>();
		T t;
		while ((t = source.source()) != null)
			map.put(keyFun.apply(t), valueFun.apply(t));
		return map;
	}

	public <O> Streamlet<O> concatMap(Fun<T, Streamlet<O>> fun) {
		return new Streamlet<>(FunUtil.concat(FunUtil.map(t -> fun.apply(t).source, source)));
	}

	public int count() {
		int i = 0;
		while (source.source() != null)
			i++;
		return i;
	}

	public T first() {
		return source.source();
	}

	public <R> R fold(BiFunction<T, R, R> fun, R init) {
		T t;
		while ((t = source.source()) != null)
			init = fun.apply(t, init);
		return init;
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

	public T source() {
		return source.source();
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
