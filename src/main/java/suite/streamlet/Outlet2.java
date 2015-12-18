package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Source2;
import suite.util.To;
import suite.util.Util;

public class Outlet2<K, V> implements Iterable<Pair<K, V>> {

	private Source2<K, V> source;

	@SafeVarargs
	public static <K, V> Outlet2<K, V> concat(Outlet2<K, V>... outlets) {
		List<Source2<K, V>> sources = new ArrayList<>();
		for (Outlet2<K, V> outlet : outlets)
			sources.add(outlet.source);
		return from(FunUtil2.concat(To.source(sources)));
	}

	public static <K, V> Outlet2<K, V> empty() {
		return from(FunUtil2.nullSource());
	}

	@SafeVarargs
	public static <K, V> Outlet2<K, V> from(Pair<K, V>... col) {
		return from(Arrays.asList(col));
	}

	public static <K, V> Outlet2<K, V> from(Iterable<Pair<K, V>> col) {
		Iterator<Pair<K, V>> iter = col.iterator();
		return from(new Source2<K, V>() {
			public boolean source(Pair<K, V> pair) {
				if (iter.hasNext()) {
					Pair<K, V> pair1 = iter.next();
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
					return true;
				} else
					return false;
			}
		});
	}

	public static <K, V> Outlet2<K, V> from(Source2<K, V> source) {
		return new Outlet2<>(source);
	}

	public Outlet2(Source2<K, V> source) {
		this.source = source;
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return FunUtil2.iterator(source);
	}

	public <R> R collect(Fun<Outlet2<K, V>, R> fun) {
		return fun.apply(this);
	}

	public <T> Outlet<T> concatMap(BiFunction<K, V, Outlet<T>> fun) {
		return Outlet.from(FunUtil.concat(FunUtil2.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(BiFunction<K, V, Outlet2<K1, V1>> fun) {
		return Outlet2.from(FunUtil2.concat(FunUtil2.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public Outlet2<K, V> closeAtEnd(Closeable c) {
		return from(pair -> {
			boolean b = next(pair);
			if (!b)
				Util.closeQuietly(c);
			return b;
		});
	}

	public Outlet2<K, V> cons(K key, V value) {
		return from(FunUtil2.cons(key, value, source));
	}

	public int count() {
		Pair<K, V> pair = Pair.of(null, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public Outlet2<K, V> distinct() {
		Set<Pair<K, V>> set = new HashSet<>();
		return from(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(Pair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public Outlet2<K, V> drop(int n) {
		Pair<K, V> pair = Pair.of(null, null);
		boolean isAvailable = true;
		while (n > 0 && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : Outlet2.empty();
	}

	@Override
	public boolean equals(Object object) {
		if (object.getClass() == Outlet2.class) {
			@SuppressWarnings("unchecked")
			Outlet2<K, V> outlet = (Outlet2<K, V>) (Outlet2<?, ?>) object;
			Source2<K, V> source1 = outlet.source;
			boolean b, b0, b1;
			Pair<K, V> pair0 = Pair.of(null, null);
			Pair<K, V> pair1 = Pair.of(null, null);
			while ((b = (b0 = source.source(pair0)) == (b1 = source1.source(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public Outlet2<K, V> filter(BiPredicate<K, V> fun) {
		return from(FunUtil2.filter(fun, source));
	}

	public <R extends Collection<Pair<K, V>>> R form(Source<R> source) {
		R r = source.source();
		Pair<K, V> pair;
		while (next(pair = Pair.of(null, null)))
			r.add(pair);
		return r;
	}

	public Outlet2<K, List<V>> groupBy() {
		Map<K, List<V>> map = toListMap();
		return Outlet.from(map.entrySet()).map2(Entry::getKey, Entry::getValue);
	}

	public boolean isAll(BiPredicate<K, V> pred) {
		Pair<K, V> pair = Pair.of(null, null);
		while (next(pair))
			if (!pred.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public boolean isAny(BiPredicate<K, V> pred) {
		Pair<K, V> pair = Pair.of(null, null);
		while (next(pair))
			if (pred.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public <T> Outlet<T> map(BiFunction<K, V, T> fun) {
		Pair<K, V> pair = Pair.of(null, null);
		return Outlet.from(() -> source.source(pair) ? fun.apply(pair.t0, pair.t1) : null);
	}

	public <K1> Outlet2<K1, V> mapKey(Fun<K, K1> fun) {
		return mapKeyValue((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <V1> Outlet2<K, V1> mapValue(Fun<V, V1> fun) {
		return mapKeyValue((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public <K1, V1> Outlet2<K1, V1> mapKeyValue(BiFunction<K, V, K1> kf, BiFunction<K, V, V1> vf) {
		Pair<K, V> pair1 = Pair.of(null, null);
		return from(pair -> {
			if (source.source(pair1)) {
				pair.t0 = kf.apply(pair1.t0, pair1.t1);
				pair.t1 = vf.apply(pair1.t0, pair1.t1);
				return true;
			} else
				return false;
		});
	}

	public Pair<K, V> min(Comparator<Pair<K, V>> comparator) {
		Pair<K, V> pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			throw new RuntimeException("No result");
	}

	public Pair<K, V> minOrNull(Comparator<Pair<K, V>> comparator) {
		Pair<K, V> pair = Pair.of(null, null);
		Pair<K, V> pair1 = Pair.of(null, null);
		boolean b = next(pair);
		if (b) {
			while (next(pair1))
				if (comparator.compare(pair, pair1) > 0) {
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
				}
			return pair;
		} else
			return null;
	}

	public boolean next(Pair<K, V> pair) {
		return source.source(pair);
	}

	public Outlet<Pair<K, V>> pairs() {
		return Outlet.from(() -> {
			Pair<K, V> pair = Pair.of(null, null);
			return next(pair) ? pair : null;
		});
	}

	public Outlet2<K, V> reverse() {
		return from(Util.reverse(toList()));
	}

	public void sink(BiConsumer<K, V> sink) {
		Pair<K, V> pair = Pair.of(null, null);
		while (next(pair))
			sink.accept(pair.t0, pair.t1);
	}

	public Outlet2<K, V> skip(int n) {
		Pair<K, V> pair = Pair.of(null, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? from(source) : empty();
	}

	public Source2<K, V> source() {
		return source;
	}

	public Outlet2<K, V> sort(Comparator<Pair<K, V>> comparator) {
		List<Pair<K, V>> list = new ArrayList<>();
		Pair<K, V> pair;
		while (next(pair = Pair.of(null, null)))
			list.add(pair);
		return from(Util.sort(list, comparator));
	}

	public Outlet2<K, V> sortByKey(Comparator<K> comparator) {
		return sort((p0, p1) -> comparator.compare(p0.t0, p1.t0));
	}

	public Outlet<Outlet2<K, V>> split(BiPredicate<K, V> fun) {
		return Outlet.from(FunUtil.map(Outlet2<K, V>::new, FunUtil2.split(source, fun)));
	}

	public Outlet2<K, V> take(int n) {
		return from(new Source2<K, V>() {
			private int count = n;

			public boolean source(Pair<K, V> pair) {
				return count-- > 0 ? next(pair) : false;
			}
		});
	}

	public List<Pair<K, V>> toList() {
		return form(() -> new ArrayList<>());
	}

	public Map<K, List<V>> toListMap() {
		Map<K, List<V>> map = new HashMap<>();
		Pair<K, V> pair = Pair.of(null, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public Map<K, V> toMap() {
		Map<K, V> map = new HashMap<>();
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink((k, v) -> {
			if (map.put(k, v) != null)
				throw new RuntimeException("Duplicate key " + k);
		});
		return map;
	}

	public ListMultimap<K, V> toMultimap() {
		ListMultimap<K, V> map = new ListMultimap<>();
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink(map::put);
		return map;
	}

	public Map<K, Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Set<Pair<K, V>> toSet() {
		return form(() -> new HashSet<>());
	}

	public Pair<K, V> uniqueResult() {
		Pair<K, V> pair = Pair.of(null, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				throw new RuntimeException("More than one result");
		else
			throw new RuntimeException("No result");
	}

}
