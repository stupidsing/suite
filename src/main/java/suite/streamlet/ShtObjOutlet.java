package suite.streamlet;

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

import suite.adt.map.ListMultimap;
import suite.adt.map.ShtObjMap;
import suite.adt.pair.Pair;
import suite.adt.pair.ShtObjPair;
import suite.node.util.Mutable;
import suite.primitive.ShtFun.ShtObj_Obj;
import suite.primitive.ShtObjFunUtil;
import suite.primitive.ShtObj_Sht;
import suite.primitive.ShtPredicate.ShtObjPredicate;
import suite.primitive.ShtPredicate.ShtPredicate_;
import suite.primitive.ShtSource.ShtObjSource;
import suite.primitive.Sht_Sht;
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

public class ShtObjOutlet<V> implements Iterable<ShtObjPair<V>> {

	private ShtObjSource<V> shortObjSource;

	@SafeVarargs
	public static <V> ShtObjOutlet<V> concat(ShtObjOutlet<V>... outlets) {
		List<ShtObjSource<V>> sources = new ArrayList<>();
		for (ShtObjOutlet<V> outlet : outlets)
			sources.add(outlet.shortObjSource);
		return of(ShtObjFunUtil.concat(To.source(sources)));
	}

	public static <V> ShtObjOutlet<V> empty() {
		return of(ShtObjFunUtil.nullSource());
	}

	public static <V> ShtObjOutlet<List<V>> of(ListMultimap<Short, V> multimap) {
		Iterator<Pair<Short, List<V>>> iter = multimap.listEntries().iterator();
		return of(pair -> {
			boolean b = iter.hasNext();
			if (b) {
				Pair<Short, List<V>> pair1 = iter.next();
				pair.t0 = pair1.t0;
				pair.t1 = pair1.t1;
			}
			return b;
		});
	}

	public static <V> ShtObjOutlet<V> of(ShtObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> ShtObjOutlet<V> of(ShtObjPair<V>... kvs) {
		return of(new ShtObjSource<V>() {
			private int i;

			public boolean source2(ShtObjPair<V> pair) {
				boolean b = i < kvs.length;
				if (b) {
					ShtObjPair<V> kv = kvs[i];
					pair.t0 = kv.t0;
					pair.t1 = kv.t1;
				}
				return b;

			}
		});
	}

	public static <V> ShtObjOutlet<V> of(Iterable<ShtObjPair<V>> col) {
		Iterator<ShtObjPair<V>> iter = col.iterator();
		return of(new ShtObjSource<V>() {
			public boolean source2(ShtObjPair<V> pair) {
				boolean b = iter.hasNext();
				if (b) {
					ShtObjPair<V> pair1 = iter.next();
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
				}
				return b;
			}
		});
	}

	public static <V> ShtObjOutlet<V> of(ShtObjSource<V> source) {
		return new ShtObjOutlet<>(source);
	}

	private ShtObjOutlet(ShtObjSource<V> source) {
		this.shortObjSource = source;
	}

	@Override
	public Iterator<ShtObjPair<V>> iterator() {
		return ShtObjFunUtil.iterator(shortObjSource);
	}

	public ShtObjOutlet<V> append(Short key, V value) {
		return of(ShtObjFunUtil.append(key, value, shortObjSource));
	}

	public Outlet<ShtObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(ShtObjOutlet<V>::new, ShtObjFunUtil.chunk(n, shortObjSource)));
	}

	public ShtObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			boolean b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<ShtObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(ShtObj_Obj<V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(ShtObjFunUtil.map((k, v) -> fun.apply(k, v).source(), shortObjSource)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(ShtObj_Obj<V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(ShtObjFunUtil.map((k, v) -> fun.apply(k, v).source(), shortObjSource)));
	}

	public <V1> ShtObjOutlet<V1> concatMapShtObj(ShtObj_Obj<V, ShtObjOutlet<V1>> fun) {
		return of(ShtObjFunUtil.concat(ShtObjFunUtil.map((k, v) -> fun.apply(k, v).shortObjSource, shortObjSource)));
	}

	public <V1> ShtObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(ShtObjFunUtil.concat(ShtObjFunUtil.map((k, v) -> {
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
		}, shortObjSource)));
	}

	public ShtObjOutlet<V> cons(short key, V value) {
		return of(ShtObjFunUtil.cons(key, value, shortObjSource));
	}

	public ShtObjOutlet<V> distinct() {
		Set<ShtObjPair<V>> set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(ShtObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public ShtObjOutlet<V> drop(int n) {
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ShtObjOutlet.class) {
			@SuppressWarnings("unchecked")
			ShtObjOutlet<V> outlet = (ShtObjOutlet<V>) (ShtObjOutlet<?>) object;
			ShtObjSource<V> source2 = outlet.shortObjSource;
			boolean b, b0, b1;
			ShtObjPair<V> pair0 = ShtObjPair.of((short) 0, null);
			ShtObjPair<V> pair1 = ShtObjPair.of((short) 0, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public ShtObjOutlet<V> filter(ShtObjPredicate<V> fun) {
		return of(ShtObjFunUtil.filter(fun, shortObjSource));
	}

	public ShtObjOutlet<V> filterKey(ShtPredicate_ fun) {
		return of(ShtObjFunUtil.filterKey(fun, shortObjSource));
	}

	public ShtObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(ShtObjFunUtil.filterValue(fun, shortObjSource));
	}

	public ShtObjPair<V> first() {
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(ShtObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(ShtObjFunUtil.map(fun, shortObjSource)));
	}

	public ShtObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> ShtObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		while (next(pair))
			hashCode = hashCode * 31 + pair.hashCode();
		return hashCode;
	}

	public boolean isAll(ShtObjPredicate<V> pred) {
		return ShtObjFunUtil.isAll(pred, shortObjSource);
	}

	public boolean isAny(ShtObjPredicate<V> pred) {
		return ShtObjFunUtil.isAny(pred, shortObjSource);
	}

	public Outlet<Short> keys() {
		return map_((k, v) -> k);
	}

	public ShtObjPair<V> last() {
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Outlet<O> map(ShtObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Outlet2<K1, V1> map2(ShtObj_Obj<V, K1> kf, ShtObj_Obj<V, V1> vf) {
		return Outlet2.of(ShtObjFunUtil.map2(kf, vf, shortObjSource));
	}

	public <V1> ShtObjOutlet<V1> mapShtObj(ShtObj_Sht<V> kf, ShtObj_Obj<V, V1> vf) {
		return mapShtObj_(kf, vf);
	}

	public ShtObjOutlet<V> mapKey(Sht_Sht fun) {
		return mapShtObj_((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <O> Outlet<O> mapNonNull(ShtObj_Obj<V, O> fun) {
		return Outlet.of(ShtObjFunUtil.mapNonNull(fun, shortObjSource));
	}

	public <V1> ShtObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapShtObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public ShtObjPair<V> min(Comparator<ShtObjPair<V>> comparator) {
		ShtObjPair<V> pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			throw new RuntimeException("no result");
	}

	public ShtObjPair<V> minOrNull(Comparator<ShtObjPair<V>> comparator) {
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		ShtObjPair<V> pair1 = ShtObjPair.of((short) 0, null);
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

	public ShtObjOutlet<V> nonBlocking(Short k0, V v0) {
		NullableSyncQueue<ShtObjPair<V>> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			boolean b;
			do {
				ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
				b = shortObjSource.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new ShtObjOutlet<>(pair -> {
			Mutable<ShtObjPair<V>> mutable = Mutable.nil();
			boolean b = queue.poll(mutable);
			if (b) {
				ShtObjPair<V> p = mutable.get();
				pair.t0 = p.t0;
				pair.t1 = p.t1;
			} else {
				pair.t0 = k0;
				pair.t1 = v0;
			}
			return b;
		});
	}

	public Outlet<ShtObjPair<V>> pairs() {
		return Outlet.of(() -> {
			ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<ShtObjOutlet<V>, ShtObjOutlet<V>> partition(ShtObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public ShtObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(BiConsumer<Short, V> sink0) {
		BiConsumer<Short, V> sink1 = Rethrow.biConsumer(sink0);
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		while (next(pair))
			sink1.accept(pair.t0, pair.t1);
	}

	public int size() {
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public ShtObjOutlet<V> skip(int n) {
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(shortObjSource) : empty();
	}

	public ShtObjOutlet<V> sort(Comparator<ShtObjPair<V>> comparator) {
		List<ShtObjPair<V>> list = new ArrayList<>();
		ShtObjPair<V> pair;
		while (next(pair = ShtObjPair.of((short) 0, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> ShtObjOutlet<V> sortBy(ShtObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public ShtObjOutlet<V> sortByKey(Comparator<Short> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public ShtObjOutlet<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public ShtObjSource<V> source() {
		return shortObjSource;
	}

	public Outlet<ShtObjOutlet<V>> split(ShtObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(ShtObjOutlet<V>::new, ShtObjFunUtil.split(fun, shortObjSource)));
	}

	public ShtObjOutlet<V> take(int n) {
		return of(new ShtObjSource<V>() {
			private int count = n;

			public boolean source2(ShtObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public ShtObjPair<V>[] toArray() {
		List<ShtObjPair<V>> list = toList();
		@SuppressWarnings("unchecked")
		ShtObjPair<V>[] array = Array_.newArray(ShtObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<ShtObjPair<V>> toList() {
		List<ShtObjPair<V>> list = new ArrayList<>();
		ShtObjPair<V> pair;
		while (next(pair = ShtObjPair.of((short) 0, null)))
			list.add(pair);
		return list;
	}

	public ShtObjMap<List<V>> toListMap() {
		ShtObjMap<List<V>> map = new ShtObjMap<>();
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public ShtObjMap<V> toMap() {
		ShtObjMap<V> map = new ShtObjMap<>();
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink(map::put);
		return map;
	}

	public ListMultimap<Short, V> toMultimap() {
		ListMultimap<Short, V> map = new ListMultimap<>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public Set<ShtObjPair<V>> toSet() {
		Set<ShtObjPair<V>> set = new HashSet<>();
		ShtObjPair<V> pair;
		while (next(pair = ShtObjPair.of((short) 0, null)))
			set.add(pair);
		return set;

	}

	public ShtObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public ShtObjPair<V> uniqueResult() {
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				throw new RuntimeException("more than one result");
		else
			throw new RuntimeException("no result");
	}

	public Outlet<V> values() {
		return map((k, v) -> v);
	}

	private <O> Outlet<O> map_(ShtObj_Obj<V, O> fun0) {
		return Outlet.of(ShtObjFunUtil.map(fun0, shortObjSource));
	}

	private <V1> ShtObjOutlet<V1> mapShtObj_(ShtObj_Sht<V> kf, ShtObj_Obj<V, V1> vf) {
		return of(ShtObjFunUtil.mapShtObj(kf, vf, shortObjSource));
	}

	private boolean next(ShtObjPair<V> pair) {
		return shortObjSource.source2(pair);
	}

}
