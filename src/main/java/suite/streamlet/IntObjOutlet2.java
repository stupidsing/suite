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
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import suite.adt.IntObjPair;
import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.node.util.Mutable;
import suite.primitive.PrimitiveFun.IntObj_Int;
import suite.primitive.PrimitiveFun.IntObj_Obj;
import suite.primitive.PrimitiveFun.Int_Int;
import suite.primitive.PrimitivePredicate.IntObjPredicate2;
import suite.primitive.PrimitiveSource.IntObjSource2;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.IntObjFunUtil2;
import suite.util.NullableSynchronousQueue;
import suite.util.To;
import suite.util.Util;

public class IntObjOutlet2<V> implements Iterable<IntObjPair<V>> {

	private IntObjSource2<V> source2;

	@SafeVarargs
	public static <V> IntObjOutlet2<V> concat(IntObjOutlet2<V>... outlets) {
		List<IntObjSource2<V>> sources = new ArrayList<>();
		for (IntObjOutlet2<V> outlet : outlets)
			sources.add(outlet.source2);
		return of(IntObjFunUtil2.concat(To.source(sources)));
	}

	public static <V> IntObjOutlet2<V> empty() {
		return of(IntObjFunUtil2.nullSource());
	}

	public static <V> IntObjOutlet2<List<V>> of(ListMultimap<Integer, V> multimap) {
		Iterator<Pair<Integer, List<V>>> iter = multimap.listEntries().iterator();
		return of(pair -> {
			boolean b = iter.hasNext();
			if (b) {
				Pair<Integer, List<V>> pair1 = iter.next();
				pair.t0 = pair1.t0;
				pair.t1 = pair1.t1;
			}
			return b;
		});
	}

	public static <V> IntObjOutlet2<V> of(Map<Integer, V> map) {
		Iterator<Entry<Integer, V>> iter = map.entrySet().iterator();
		return of(pair -> {
			boolean b = iter.hasNext();
			if (b) {
				Entry<Integer, V> pair1 = iter.next();
				pair.t0 = pair1.getKey();
				pair.t1 = pair1.getValue();
			}
			return b;

		});
	}

	@SafeVarargs
	public static <V> IntObjOutlet2<V> of(IntObjPair<V>... kvs) {
		return of(new IntObjSource2<V>() {
			private int i;

			public boolean source2(IntObjPair<V> pair) {
				boolean b = i < kvs.length;
				if (b) {
					IntObjPair<V> kv = kvs[i];
					pair.t0 = kv.t0;
					pair.t1 = kv.t1;
				}
				return b;

			}
		});
	}

	public static <V> IntObjOutlet2<V> of(Iterable<IntObjPair<V>> col) {
		Iterator<IntObjPair<V>> iter = col.iterator();
		return of(new IntObjSource2<V>() {
			public boolean source2(IntObjPair<V> pair) {
				boolean b = iter.hasNext();
				if (b) {
					IntObjPair<V> pair1 = iter.next();
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
				}
				return b;
			}
		});
	}

	public static <V> IntObjOutlet2<V> of(IntObjSource2<V> source) {
		return new IntObjOutlet2<>(source);
	}

	private IntObjOutlet2(IntObjSource2<V> source) {
		this.source2 = source;
	}

	@Override
	public Iterator<IntObjPair<V>> iterator() {
		return IntObjFunUtil2.iterator(source2);
	}

	public Outlet<IntObjOutlet2<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(IntObjOutlet2<V>::new, IntObjFunUtil2.chunk(n, source2)));
	}

	public IntObjOutlet2<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			boolean b = next(pair);
			if (!b)
				Util.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<IntObjOutlet2<V>, R> fun) {
		return fun.apply(this);
	}

	public <T> Outlet<T> concatMap(BiFunction<Integer, V, Outlet<T>> fun) {
		return Outlet.of(FunUtil.concat(IntObjFunUtil2.map((k, v) -> fun.apply(k, v).source(), source2)));
	}

	public <V1> IntObjOutlet2<V1> concatMap2(BiFunction<Integer, V, IntObjOutlet2<V1>> fun) {
		return of(IntObjFunUtil2.concat(IntObjFunUtil2.map((k, v) -> fun.apply(k, v).source2, source2)));
	}

	public <V1> IntObjOutlet2<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(IntObjFunUtil2.concat(IntObjFunUtil2.map((k, v) -> {
			Source<V1> source = fun.apply(v).source();
			return pair -> {
				V1 value1 = source.source();
				boolean b = value1 != null;
				if (b) {
					pair.t0 = k;
					pair.t1 = value1;
				}
				return b;
			};
		}, source2)));
	}

	public IntObjOutlet2<V> cons(Integer key, V value) {
		return of(IntObjFunUtil2.cons(key, value, source2));
	}

	public IntObjOutlet2<V> distinct() {
		Set<IntObjPair<V>> set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(IntObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public IntObjOutlet2<V> drop(int n) {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Util.clazz(object) == IntObjOutlet2.class) {
			@SuppressWarnings("unchecked")
			IntObjOutlet2<V> outlet = (IntObjOutlet2<V>) (IntObjOutlet2<?>) object;
			IntObjSource2<V> source2 = outlet.source2;
			boolean b, b0, b1;
			IntObjPair<V> pair0 = IntObjPair.of(0, null);
			IntObjPair<V> pair1 = IntObjPair.of(0, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public IntObjOutlet2<V> filter(IntObjPredicate2<V> fun) {
		return of(IntObjFunUtil2.filter(fun, source2));
	}

	public IntObjOutlet2<V> filterKey(IntPredicate fun) {
		return of(IntObjFunUtil2.filterKey(fun, source2));
	}

	public IntObjOutlet2<V> filterValue(Predicate<V> fun) {
		return of(IntObjFunUtil2.filterValue(fun, source2));
	}

	public IntObjPair<V> first() {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		return next(pair) ? pair : null;
	}

	public Outlet2<Integer, List<V>> groupBy() {
		Map<Integer, List<V>> map = toListMap();
		return Outlet.of(map.entrySet()).map2(Entry::getKey, Entry::getValue);
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		IntObjPair<V> pair = IntObjPair.of(0, null);
		while (next(pair))
			hashCode = hashCode * 31 + pair.hashCode();
		return hashCode;
	}

	public boolean isAll(IntObjPredicate2<V> pred) {
		return IntObjFunUtil2.isAll(pred, source2);
	}

	public boolean isAny(IntObjPredicate2<V> pred) {
		return IntObjFunUtil2.isAny(pred, source2);
	}

	public IntObjPair<V> last() {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <T> Outlet<T> map(IntObj_Obj<V, T> fun0) {
		return Outlet.of(IntObjFunUtil2.map(fun0, source2));
	}

	public <K1, V1> Outlet2<K1, V1> mapEntry(IntObj_Obj<V, K1> kf, IntObj_Obj<V, V1> vf) {
		return Outlet2.of(FunUtil2.map2(kf, vf, source2));
	}

	public <V1> IntObjOutlet2<V1> mapIntObj(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return of(IntObjFunUtil2.map2(kf, vf, source2));
	}

	public IntObjOutlet2<V> mapKey(Int_Int fun) {
		return mapIntObj((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <V1> IntObjOutlet2<V1> mapValue(Fun<V, V1> fun) {
		return mapIntObj((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public IntObjPair<V> min(Comparator<IntObjPair<V>> comparator) {
		IntObjPair<V> pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			throw new RuntimeException("No result");
	}

	public IntObjPair<V> minOrNull(Comparator<IntObjPair<V>> comparator) {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		IntObjPair<V> pair1 = IntObjPair.of(0, null);
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

	public IntObjOutlet2<V> nonBlocking(Integer k0, V v0) {
		NullableSynchronousQueue<IntObjPair<V>> queue = new NullableSynchronousQueue<>();

		new Thread(() -> {
			boolean b;
			do {
				IntObjPair<V> pair = IntObjPair.of(0, null);
				b = source2.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new IntObjOutlet2<>(pair -> {
			Mutable<IntObjPair<V>> mutable = Mutable.nil();
			boolean b = queue.poll(mutable);
			if (b) {
				IntObjPair<V> p = mutable.get();
				pair.t0 = p.t0;
				pair.t1 = p.t1;
			} else {
				pair.t0 = k0;
				pair.t1 = v0;
			}
			return b;
		});
	}

	public Outlet<IntObjPair<V>> pairs() {
		return Outlet.of(() -> {
			IntObjPair<V> pair = IntObjPair.of(0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<IntObjOutlet2<V>, IntObjOutlet2<V>> partition(IntObjPredicate2<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public IntObjOutlet2<V> reverse() {
		return of(Util.reverse(toList()));
	}

	public void sink(BiConsumer<Integer, V> sink) {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		while (next(pair))
			sink.accept(pair.t0, pair.t1);
	}

	public int size() {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public IntObjOutlet2<V> skip(int n) {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source2) : empty();
	}

	public IntObjOutlet2<V> sort(Comparator<IntObjPair<V>> comparator) {
		List<IntObjPair<V>> list = new ArrayList<>();
		IntObjPair<V> pair;
		while (next(pair = IntObjPair.of(0, null)))
			list.add(pair);
		return of(Util.sort(list, comparator));
	}

	public IntObjOutlet2<V> sortByKey(Comparator<Integer> comparator) {
		return sort((p0, p1) -> comparator.compare(p0.t0, p1.t0));
	}

	public IntObjSource2<V> source2() {
		return source2;
	}

	public Outlet<IntObjOutlet2<V>> split(IntObjPredicate2<V> fun) {
		return Outlet.of(FunUtil.map(IntObjOutlet2<V>::new, IntObjFunUtil2.split(fun, source2)));
	}

	public IntObjOutlet2<V> take(int n) {
		return of(new IntObjSource2<V>() {
			private int count = n;

			public boolean source2(IntObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public IntObjPair<V>[] toArray() {
		List<IntObjPair<V>> list = toList();
		@SuppressWarnings("unchecked")
		IntObjPair<V> array[] = (IntObjPair[]) Array.newInstance(Pair.class, list.size());
		return list.toArray(array);
	}

	public List<IntObjPair<V>> toList() {
		List<IntObjPair<V>> list = new ArrayList<>();
		IntObjPair<V> pair;
		while (next(pair = IntObjPair.of(0, null)))
			list.add(pair);
		return list;
	}

	public Map<Integer, List<V>> toListMap() {
		Map<Integer, List<V>> map = new HashMap<>();
		IntObjPair<V> pair = IntObjPair.of(0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public Map<Integer, V> toMap() {
		Map<Integer, V> map = new HashMap<>();
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink(map::put);
		return map;
	}

	public ListMultimap<Integer, V> toMultimap() {
		ListMultimap<Integer, V> map = new ListMultimap<>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public Set<IntObjPair<V>> toSet() {
		Set<IntObjPair<V>> set = new HashSet<>();
		IntObjPair<V> pair;
		while (next(pair = IntObjPair.of(0, null)))
			set.add(pair);
		return set;

	}

	public Map<Integer, Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public IntObjPair<V> uniqueResult() {
		IntObjPair<V> pair = IntObjPair.of(0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				throw new RuntimeException("More than one result");
		else
			throw new RuntimeException("No result");
	}

	private boolean next(IntObjPair<V> pair) {
		return source2.source2(pair);
	}

}
