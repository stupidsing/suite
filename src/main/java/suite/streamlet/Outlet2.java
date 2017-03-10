package suite.streamlet;

import java.io.Closeable;
import java.lang.reflect.Array;
import java.util.ArrayList;
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
import java.util.function.Predicate;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.node.util.Mutable;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSynchronousQueue;
import suite.util.To;
import suite.util.Util;

public class Outlet2<K, V> implements Iterable<Pair<K, V>> {

	private Source2<K, V> source2;

	@SafeVarargs
	public static <K, V> Outlet2<K, V> concat(Outlet2<K, V>... outlets) {
		List<Source2<K, V>> sources = new ArrayList<>();
		for (Outlet2<K, V> outlet : outlets)
			sources.add(outlet.source2);
		return from(FunUtil2.concat(To.source(sources)));
	}

	public static <K, V> Outlet2<K, V> empty() {
		return from(FunUtil2.nullSource());
	}

	public static <K, V> Outlet2<K, List<V>> from(ListMultimap<K, V> multimap) {
		Iterator<Pair<K, List<V>>> iter = multimap.listEntries().iterator();
		return from(pair -> {
			if (iter.hasNext()) {
				Pair<K, List<V>> pair1 = iter.next();
				pair.t0 = pair1.t0;
				pair.t1 = pair1.t1;
				return true;
			} else
				return false;
		});
	}

	public static <K, V> Outlet2<K, V> from(Map<K, V> map) {
		Iterator<Entry<K, V>> iter = map.entrySet().iterator();
		return from(pair -> {
			if (iter.hasNext()) {
				Entry<K, V> pair1 = iter.next();
				pair.t0 = pair1.getKey();
				pair.t1 = pair1.getValue();
				return true;
			} else
				return false;
		});
	}

	@SafeVarargs
	public static <K, V> Outlet2<K, V> from(Pair<K, V>... kvs) {
		return from(new Source2<K, V>() {
			private int i;

			public boolean source2(Pair<K, V> pair) {
				if (i < kvs.length) {
					Pair<K, V> kv = kvs[i];
					pair.t0 = kv.t0;
					pair.t1 = kv.t1;
					return true;
				}
				return false;
			}
		});
	}

	public static <K, V> Outlet2<K, V> from(Iterable<Pair<K, V>> col) {
		Iterator<Pair<K, V>> iter = col.iterator();
		return from(new Source2<K, V>() {
			public boolean source2(Pair<K, V> pair) {
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
		this.source2 = source;
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return FunUtil2.iterator(source2);
	}

	public Outlet<Outlet2<K, V>> chunk(int n) {
		return Outlet.from(FunUtil.map(Outlet2<K, V>::new, FunUtil2.chunk(n, source2)));
	}

	public Outlet2<K, V> closeAtEnd(Closeable c) {
		return from(pair -> {
			boolean b = next(pair);
			if (!b)
				Util.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<Outlet2<K, V>, R> fun) {
		return fun.apply(this);
	}

	public <T> Outlet<T> concatMap(BiFunction<K, V, Outlet<T>> fun) {
		return Outlet.from(FunUtil.concat(FunUtil2.map((k, v) -> fun.apply(k, v).source(), source2)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(BiFunction<K, V, Outlet2<K1, V1>> fun) {
		return Outlet2.from(FunUtil2.concat(FunUtil2.map((k, v) -> fun.apply(k, v).source2, source2)));
	}

	public <V1> Outlet2<K, V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return Outlet2.from(FunUtil2.concat(FunUtil2.map((k, v) -> {
			Source<V1> source = fun.apply(v).source();
			return pair -> {
				V1 value1 = source.source();
				if (value1 != null) {
					pair.t0 = k;
					pair.t1 = value1;
					return true;
				} else
					return false;
			};
		}, source2)));
	}

	public Outlet2<K, V> cons(K key, V value) {
		return from(FunUtil2.cons(key, value, source2));
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
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : Outlet2.empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == Outlet2.class) {
			@SuppressWarnings("unchecked")
			Outlet2<K, V> outlet = (Outlet2<K, V>) (Outlet2<?, ?>) object;
			Source2<K, V> source2 = outlet.source2;
			boolean b, b0, b1;
			Pair<K, V> pair0 = Pair.of(null, null);
			Pair<K, V> pair1 = Pair.of(null, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public Outlet2<K, V> filter(BiPredicate<K, V> fun) {
		return from(FunUtil2.filter(fun, source2));
	}

	public Outlet2<K, V> filterKey(Predicate<K> fun) {
		return from(FunUtil2.filterKey(fun, source2));
	}

	public Pair<K, V> first() {
		Pair<K, V> pair = Pair.of(null, null);
		return next(pair) ? pair : null;
	}

	public Outlet2<K, List<V>> groupBy() {
		Map<K, List<V>> map = toListMap();
		return Outlet.from(map.entrySet()).map2(Entry::getKey, Entry::getValue);
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		Pair<K, V> pair = Pair.of(null, null);
		while (next(pair))
			hashCode = hashCode * 31 + pair.hashCode();
		return hashCode;
	}

	public boolean isAll(BiPredicate<K, V> pred) {
		return FunUtil2.isAll(pred, source2);
	}

	public boolean isAny(BiPredicate<K, V> pred) {
		return FunUtil2.isAny(pred, source2);
	}

	public <T> Outlet<T> map(BiFunction<K, V, T> fun0) {
		return Outlet.from(FunUtil2.map(fun0, source2));
	}

	public <K1, V1> Outlet2<K1, V1> mapEntry(BiFunction<K, V, K1> kf, BiFunction<K, V, V1> vf) {
		return from(FunUtil2.map2(kf, vf, source2));
	}

	public <K1> Outlet2<K1, V> mapKey(Fun<K, K1> fun) {
		return mapEntry((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <V1> Outlet2<K, V1> mapValue(Fun<V, V1> fun) {
		return mapEntry((k, v) -> k, (k, v) -> fun.apply(v));
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
				if (0 < comparator.compare(pair, pair1)) {
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
				}
			return pair;
		} else
			return null;
	}

	public Outlet2<K, V> nonBlocking(K k0, V v0) {
		NullableSynchronousQueue<Pair<K, V>> queue = new NullableSynchronousQueue<>();

		new Thread(() -> {
			boolean b;
			do {
				Pair<K, V> pair = Pair.of(null, null);
				b = source2.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new Outlet2<>(pair -> {
			Mutable<Pair<K, V>> mutable = Mutable.nil();
			boolean b = queue.poll(mutable);
			if (b) {
				Pair<K, V> p = mutable.get();
				pair.t0 = p.t0;
				pair.t1 = p.t1;
			} else {
				pair.t0 = k0;
				pair.t1 = v0;
			}
			return b;
		});
	}

	public Outlet<Pair<K, V>> pairs() {
		return Outlet.from(() -> {
			Pair<K, V> pair = Pair.of(null, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<Outlet2<K, V>, Outlet2<K, V>> partition(BiPredicate<K, V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public Outlet2<K, V> reverse() {
		return from(Util.reverse(toList()));
	}

	public void sink(BiConsumer<K, V> sink) {
		Pair<K, V> pair = Pair.of(null, null);
		while (next(pair))
			sink.accept(pair.t0, pair.t1);
	}

	public int size() {
		Pair<K, V> pair = Pair.of(null, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public Outlet2<K, V> skip(int n) {
		Pair<K, V> pair = Pair.of(null, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? from(source2) : empty();
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

	public Source2<K, V> source2() {
		return source2;
	}

	public Outlet<Outlet2<K, V>> split(BiPredicate<K, V> fun) {
		return Outlet.from(FunUtil.map(Outlet2<K, V>::new, FunUtil2.split(fun, source2)));
	}

	public Outlet2<K, V> take(int n) {
		return from(new Source2<K, V>() {
			private int count = n;

			public boolean source2(Pair<K, V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public Pair<K, V>[] toArray() {
		List<Pair<K, V>> list = toList();
		@SuppressWarnings("unchecked")
		Pair<K, V> array[] = (Pair[]) Array.newInstance(Pair.class, list.size());
		return list.toArray(array);
	}

	public List<Pair<K, V>> toList() {
		List<Pair<K, V>> list = new ArrayList<>();
		Pair<K, V> pair;
		while (next(pair = Pair.of(null, null)))
			list.add(pair);
		return list;
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
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink(map::put);
		return map;
	}

	public ListMultimap<K, V> toMultimap() {
		ListMultimap<K, V> map = new ListMultimap<>();
		groupBy().concatMapValue(Outlet::from).sink(map::put);
		return map;
	}

	public Set<Pair<K, V>> toSet() {
		Set<Pair<K, V>> set = new HashSet<>();
		Pair<K, V> pair;
		while (next(pair = Pair.of(null, null)))
			set.add(pair);
		return set;

	}

	public Map<K, Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
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

	private boolean next(Pair<K, V> pair) {
		return source2.source2(pair);
	}

}
