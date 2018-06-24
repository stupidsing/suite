package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import suite.adt.Mutable;
import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.LngFunUtil;
import suite.primitive.LngObjFunUtil;
import suite.primitive.LngObj_Lng;
import suite.primitive.LngPrimitives.LngObjPredicate;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.LngObj_Obj;
import suite.primitive.LngPrimitives.LngTest;
import suite.primitive.adt.map.LngObjMap;
import suite.primitive.adt.map.ObjLngMap;
import suite.primitive.adt.pair.LngObjPair;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.OutletDefaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Array_;
import suite.util.Fail;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Sink2;
import suite.util.List_;
import suite.util.NullableSyncQueue;
import suite.util.Object_;
import suite.util.To;

public class LngObjOutlet<V> implements OutletDefaults<LngObjPair<V>> {

	private static long EMPTYVALUE = LngFunUtil.EMPTYVALUE;

	private LngObjSource<V> source;

	@SafeVarargs
	public static <V> LngObjOutlet<V> concat(LngObjOutlet<V>... outlets) {
		var sources = new ArrayList<LngObjSource<V>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(LngObjFunUtil.concat(To.source(sources)));
	}

	public static <V> LngObjOutlet<V> empty() {
		return of(LngObjFunUtil.nullSource());
	}

	public static <V> LngObjOutlet<List<V>> of(ListMultimap<Long, V> multimap) {
		var iter = multimap.listEntries().iterator();
		return of(pair -> {
			var b = iter.hasNext();
			if (b) {
				var pair1 = iter.next();
				pair.update(pair1.t0, pair1.t1);
			}
			return b;
		});
	}

	public static <V> LngObjOutlet<V> of(LngObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> LngObjOutlet<V> of(LngObjPair<V>... kvs) {
		return of(new LngObjSource<>() {
			private int i;

			public boolean source2(LngObjPair<V> pair) {
				var b = i < kvs.length;
				if (b) {
					LngObjPair<V> kv = kvs[i];
					pair.update(kv.t0, kv.t1);
				}
				return b;

			}
		});
	}

	public static <V> LngObjOutlet<V> of(Iterable<LngObjPair<V>> col) {
		var iter = col.iterator();
		return of(new LngObjSource<>() {
			public boolean source2(LngObjPair<V> pair) {
				var b = iter.hasNext();
				if (b) {
					LngObjPair<V> pair1 = iter.next();
					pair.update(pair1.t0, pair1.t1);
				}
				return b;
			}
		});
	}

	public static <V> LngObjOutlet<V> of(LngObjSource<V> source) {
		return new LngObjOutlet<>(source);
	}

	private LngObjOutlet(LngObjSource<V> source) {
		this.source = source;
	}

	public Outlet<LngObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(LngObjOutlet<V>::new, LngObjFunUtil.chunk(n, source)));
	}

	public LngObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<LngObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(LngObj_Obj<V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(LngObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(LngObj_Obj<V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(LngObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> LngObjOutlet<V1> concatMapLngObj(LngObj_Obj<V, LngObjOutlet<V1>> fun) {
		return of(LngObjFunUtil.concat(LngObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> LngObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(LngObjFunUtil.concat(LngObjFunUtil.map((k, v) -> {
			var source = fun.apply(v).source();
			return pair -> {
				var value1 = source.source();
				var b = value1 != null;
				if (b)
					pair.update(k, value1);
				return b;
			};
		}, source)));
	}

	public LngObjOutlet<V> cons(long key, V value) {
		return of(LngObjFunUtil.cons(key, value, source));
	}

	public int count() {
		var pair = LngObjPair.<V> of((long) 0, null);
		var i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public LngObjOutlet<V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(LngObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public LngObjOutlet<V> drop(int n) {
		var pair = LngObjPair.<V> of((long) 0, null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngObjOutlet.class) {
			@SuppressWarnings("unchecked")
			var outlet = (LngObjOutlet<V>) (LngObjOutlet<?>) object;
			var source2 = outlet.source;
			boolean b, b0, b1;
			var pair0 = LngObjPair.<V> of((long) 0, null);
			var pair1 = LngObjPair.<V> of((long) 0, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public LngObjOutlet<V> filter(LngObjPredicate<V> fun) {
		return of(LngObjFunUtil.filter(fun, source));
	}

	public LngObjOutlet<V> filterKey(LngTest fun) {
		return of(LngObjFunUtil.filterKey(fun, source));
	}

	public LngObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(LngObjFunUtil.filterValue(fun, source));
	}

	public LngObjPair<V> first() {
		var pair = LngObjPair.<V> of((long) 0, null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(LngObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(LngObjFunUtil.map(fun, source)));
	}

	public LngObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> LngObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var pair = LngObjPair.<V> of((long) 0, null);
		var h = 7;
		while (next(pair))
			h = h * 31 + pair.hashCode();
		return h;
	}

	public boolean isAll(LngObjPredicate<V> pred) {
		return LngObjFunUtil.isAll(pred, source);
	}

	public boolean isAny(LngObjPredicate<V> pred) {
		return LngObjFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<LngObjPair<V>> iterator() {
		return LngObjFunUtil.iterator(source);
	}

	public LngOutlet keys() {
		return LngOutlet.of(() -> {
			var pair = LngObjPair.<V> of((long) 0, null);
			return next(pair) ? pair.t0 : EMPTYVALUE;
		});
	}

	public LngObjPair<V> last() {
		var pair = LngObjPair.<V> of((long) 0, null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Outlet<O> map(LngObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Outlet2<K1, V1> map2(LngObj_Obj<V, K1> kf, LngObj_Obj<V, V1> vf) {
		return Outlet2.of(LngObjFunUtil.map2(kf, vf, source));
	}

	public <V1> LngObjOutlet<V1> mapLngObj(LngObj_Lng<V> kf, LngObj_Obj<V, V1> vf) {
		return mapLngObj_(kf, vf);
	}

	public <V1> LngObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapLngObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public LngObjPair<V> min(Comparator<LngObjPair<V>> comparator) {
		var pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			return Fail.t("no result");
	}

	public LngObjPair<V> minOrNull(Comparator<LngObjPair<V>> comparator) {
		var pair = LngObjPair.<V> of((long) 0, null);
		var pair1 = LngObjPair.<V> of((long) 0, null);
		if (next(pair)) {
			while (next(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.t0, pair1.t1);
			return pair;
		} else
			return null;
	}

	public LngObjOutlet<V> nonBlocking(Long k0, V v0) {
		var queue = new NullableSyncQueue<LngObjPair<V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = LngObjPair.<V> of((long) 0, null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new LngObjOutlet<>(pair -> {
			var mutable = Mutable.<LngObjPair<V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.get();
				pair.update(p.t0, p.t1);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public LngObjPair<V> opt() {
		var pair = LngObjPair.<V> of((long) 0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				return Fail.t("more than one result");
		else
			return LngObjPair.none();
	}

	public Outlet<LngObjPair<V>> pairs() {
		return Outlet.of(() -> {
			var pair = LngObjPair.<V> of((long) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<LngObjOutlet<V>, LngObjOutlet<V>> partition(LngObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public LngObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink2<Long, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = LngObjPair.<V> of((long) 0, null);
		while (next(pair))
			sink1.sink2(pair.t0, pair.t1);
	}

	public LngObjOutlet<V> skip(int n) {
		var pair = LngObjPair.<V> of((long) 0, null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source) : empty();
	}

	public LngObjOutlet<V> snoc(Long key, V value) {
		return of(LngObjFunUtil.snoc(key, value, source));
	}

	public LngObjOutlet<V> sort(Comparator<LngObjPair<V>> comparator) {
		var list = new ArrayList<LngObjPair<V>>();
		LngObjPair<V> pair;
		while (next(pair = LngObjPair.of((long) 0, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> LngObjOutlet<V> sortBy(LngObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public LngObjOutlet<V> sortByKey(Comparator<Long> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public LngObjOutlet<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public LngObjSource<V> source() {
		return source;
	}

	public Outlet<LngObjOutlet<V>> split(LngObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(LngObjOutlet<V>::new, LngObjFunUtil.split(fun, source)));
	}

	public LngObjOutlet<V> take(int n) {
		return of(new LngObjSource<>() {
			private int count = n;

			public boolean source2(LngObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public LngObjPair<V>[] toArray() {
		var list = toList();
		@SuppressWarnings("unchecked")
		LngObjPair<V>[] array = Array_.newArray(LngObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<LngObjPair<V>> toList() {
		var list = new ArrayList<LngObjPair<V>>();
		LngObjPair<V> pair;
		while (next(pair = LngObjPair.of((long) 0, null)))
			list.add(pair);
		return list;
	}

	public LngObjMap<List<V>> toListMap() {
		var map = new LngObjMap<List<V>>();
		var pair = LngObjPair.<V> of((long) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public LngObjMap<V> toMap() {
		var map = new LngObjMap<V>();
		var pair = LngObjPair.<V> of((long) 0, null);
		while (source.source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ListMultimap<Long, V> toMultimap() {
		var map = new ListMultimap<Long, V>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public ObjLngMap<V> toObjLngMap() {
		var pair = LngObjPair.<V> of((long) 0, null);
		var map = new ObjLngMap<V>();
		while (source.source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public Set<LngObjPair<V>> toSet() {
		var set = new HashSet<LngObjPair<V>>();
		LngObjPair<V> pair;
		while (next(pair = LngObjPair.of((long) 0, null)))
			set.add(pair);
		return set;

	}

	public LngObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Outlet<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Outlet<O> map_(LngObj_Obj<V, O> fun0) {
		return Outlet.of(LngObjFunUtil.map(fun0, source));
	}

	private <V1> LngObjOutlet<V1> mapLngObj_(LngObj_Lng<V> kf, LngObj_Obj<V, V1> vf) {
		return of(LngObjFunUtil.mapLngObj(kf, vf, source));
	}

	private boolean next(LngObjPair<V> pair) {
		return source.source2(pair);
	}

}
