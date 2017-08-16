package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import suite.adt.Mutable;
import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.IntObjFunUtil;
import suite.primitive.IntObj_Int;
import suite.primitive.IntPrimitives.IntObjPredicate;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.IntPredicate;
import suite.primitive.Int_Int;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.OutletDefaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Array_;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.List_;
import suite.util.NullableSyncQueue;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.To;

public class IntObjOutlet<V> implements OutletDefaults<IntObjPair<V>> {

	private IntObjSource<V> source;

	@SafeVarargs
	public static <V> IntObjOutlet<V> concat(IntObjOutlet<V>... outlets) {
		List<IntObjSource<V>> sources = new ArrayList<>();
		for (IntObjOutlet<V> outlet : outlets)
			sources.add(outlet.source);
		return of(IntObjFunUtil.concat(To.source(sources)));
	}

	public static <V> IntObjOutlet<V> empty() {
		return of(IntObjFunUtil.nullSource());
	}

	public static <V> IntObjOutlet<List<V>> of(ListMultimap<Integer, V> multimap) {
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

	public static <V> IntObjOutlet<V> of(IntObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> IntObjOutlet<V> of(IntObjPair<V>... kvs) {
		return of(new IntObjSource<V>() {
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

	public static <V> IntObjOutlet<V> of(Iterable<IntObjPair<V>> col) {
		Iterator<IntObjPair<V>> iter = col.iterator();
		return of(new IntObjSource<V>() {
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

	public static <V> IntObjOutlet<V> of(IntObjSource<V> source) {
		return new IntObjOutlet<>(source);
	}

	private IntObjOutlet(IntObjSource<V> source) {
		this.source = source;
	}

	@Override
	public Iterator<IntObjPair<V>> iterator() {
		return IntObjFunUtil.iterator(source);
	}

	public IntObjOutlet<V> append(Integer key, V value) {
		return of(IntObjFunUtil.append(key, value, source));
	}

	public Outlet<IntObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(IntObjOutlet<V>::new, IntObjFunUtil.chunk(n, source)));
	}

	public IntObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			boolean b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<IntObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(IntObj_Obj<V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(IntObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(IntObj_Obj<V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(IntObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> IntObjOutlet<V1> concatMapIntObj(IntObj_Obj<V, IntObjOutlet<V1>> fun) {
		return of(IntObjFunUtil.concat(IntObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> IntObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(IntObjFunUtil.concat(IntObjFunUtil.map((k, v) -> {
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
		}, source)));
	}

	public IntObjOutlet<V> cons(int key, V value) {
		return of(IntObjFunUtil.cons(key, value, source));
	}

	public int count() {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public IntObjOutlet<V> distinct() {
		Set<IntObjPair<V>> set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(IntObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public IntObjOutlet<V> drop(int n) {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntObjOutlet.class) {
			@SuppressWarnings("unchecked")
			IntObjOutlet<V> outlet = (IntObjOutlet<V>) (IntObjOutlet<?>) object;
			IntObjSource<V> source2 = outlet.source;
			boolean b, b0, b1;
			IntObjPair<V> pair0 = IntObjPair.of((int) 0, null);
			IntObjPair<V> pair1 = IntObjPair.of((int) 0, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public IntObjOutlet<V> filter(IntObjPredicate<V> fun) {
		return of(IntObjFunUtil.filter(fun, source));
	}

	public IntObjOutlet<V> filterKey(IntPredicate fun) {
		return of(IntObjFunUtil.filterKey(fun, source));
	}

	public IntObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(IntObjFunUtil.filterValue(fun, source));
	}

	public IntObjPair<V> first() {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(IntObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(IntObjFunUtil.map(fun, source)));
	}

	public IntObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> IntObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		while (next(pair))
			hashCode = hashCode * 31 + pair.hashCode();
		return hashCode;
	}

	public boolean isAll(IntObjPredicate<V> pred) {
		return IntObjFunUtil.isAll(pred, source);
	}

	public boolean isAny(IntObjPredicate<V> pred) {
		return IntObjFunUtil.isAny(pred, source);
	}

	public Outlet<Integer> keys() {
		return map_((k, v) -> k);
	}

	public IntObjPair<V> last() {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Outlet<O> map(IntObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Outlet2<K1, V1> map2(IntObj_Obj<V, K1> kf, IntObj_Obj<V, V1> vf) {
		return Outlet2.of(IntObjFunUtil.map2(kf, vf, source));
	}

	public <V1> IntObjOutlet<V1> mapIntObj(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return mapIntObj_(kf, vf);
	}

	public IntObjOutlet<V> mapKey(Int_Int fun) {
		return mapIntObj_((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <O> Outlet<O> mapNonNull(IntObj_Obj<V, O> fun) {
		return Outlet.of(IntObjFunUtil.mapNonNull(fun, source));
	}

	public <V1> IntObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapIntObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public IntObjPair<V> min(Comparator<IntObjPair<V>> comparator) {
		IntObjPair<V> pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			throw new RuntimeException("no result");
	}

	public IntObjPair<V> minOrNull(Comparator<IntObjPair<V>> comparator) {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		IntObjPair<V> pair1 = IntObjPair.of((int) 0, null);
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

	public IntObjOutlet<V> nonBlocking(Integer k0, V v0) {
		NullableSyncQueue<IntObjPair<V>> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			boolean b;
			do {
				IntObjPair<V> pair = IntObjPair.of((int) 0, null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new IntObjOutlet<>(pair -> {
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

	public IntObjPair<V> opt() {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				throw new RuntimeException("more than one result");
		else
			return IntObjPair.none();
	}

	public Outlet<IntObjPair<V>> pairs() {
		return Outlet.of(() -> {
			IntObjPair<V> pair = IntObjPair.of((int) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<IntObjOutlet<V>, IntObjOutlet<V>> partition(IntObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public IntObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(BiConsumer<Integer, V> sink0) {
		BiConsumer<Integer, V> sink1 = Rethrow.biConsumer(sink0);
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		while (next(pair))
			sink1.accept(pair.t0, pair.t1);
	}

	public IntObjOutlet<V> skip(int n) {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source) : empty();
	}

	public IntObjOutlet<V> sort(Comparator<IntObjPair<V>> comparator) {
		List<IntObjPair<V>> list = new ArrayList<>();
		IntObjPair<V> pair;
		while (next(pair = IntObjPair.of((int) 0, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> IntObjOutlet<V> sortBy(IntObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public IntObjOutlet<V> sortByKey(Comparator<Integer> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public IntObjOutlet<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public IntObjSource<V> source() {
		return source;
	}

	public Outlet<IntObjOutlet<V>> split(IntObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(IntObjOutlet<V>::new, IntObjFunUtil.split(fun, source)));
	}

	public IntObjOutlet<V> take(int n) {
		return of(new IntObjSource<V>() {
			private int count = n;

			public boolean source2(IntObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public IntObjPair<V>[] toArray() {
		List<IntObjPair<V>> list = toList();
		@SuppressWarnings("unchecked")
		IntObjPair<V>[] array = Array_.newArray(IntObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<IntObjPair<V>> toList() {
		List<IntObjPair<V>> list = new ArrayList<>();
		IntObjPair<V> pair;
		while (next(pair = IntObjPair.of((int) 0, null)))
			list.add(pair);
		return list;
	}

	public IntObjMap<List<V>> toListMap() {
		IntObjMap<List<V>> map = new IntObjMap<>();
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public IntObjMap<V> toMap() {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		IntObjMap<V> map = new IntObjMap<>();
		while (source.source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ListMultimap<Integer, V> toMultimap() {
		ListMultimap<Integer, V> map = new ListMultimap<>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public ObjIntMap<V> toObjIntMap() {
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		ObjIntMap<V> map = new ObjIntMap<>();
		while (source.source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public Set<IntObjPair<V>> toSet() {
		Set<IntObjPair<V>> set = new HashSet<>();
		IntObjPair<V> pair;
		while (next(pair = IntObjPair.of((int) 0, null)))
			set.add(pair);
		return set;

	}

	public IntObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Outlet<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Outlet<O> map_(IntObj_Obj<V, O> fun0) {
		return Outlet.of(IntObjFunUtil.map(fun0, source));
	}

	private <V1> IntObjOutlet<V1> mapIntObj_(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return of(IntObjFunUtil.mapIntObj(kf, vf, source));
	}

	private boolean next(IntObjPair<V> pair) {
		return source.source2(pair);
	}

}
