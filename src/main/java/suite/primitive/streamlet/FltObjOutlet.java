package suite.primitive.streamlet;

import static suite.util.Friends.fail;

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
import suite.object.Object_;
import suite.primitive.FltFunUtil;
import suite.primitive.FltObjFunUtil;
import suite.primitive.FltObj_Flt;
import suite.primitive.FltPrimitives.FltObjPredicate;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.FltPrimitives.FltTest;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.adt.map.ObjFltMap;
import suite.primitive.adt.pair.FltObjPair;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil2;
import suite.streamlet.FunUtil2.Sink2;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.OutletDefaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Array_;
import suite.util.List_;
import suite.util.NullableSyncQueue;
import suite.util.To;

public class FltObjOutlet<V> implements OutletDefaults<FltObjPair<V>> {

	private static float empty = FltFunUtil.EMPTYVALUE;

	private FltObjSource<V> source;

	@SafeVarargs
	public static <V> FltObjOutlet<V> concat(FltObjOutlet<V>... outlets) {
		var sources = new ArrayList<FltObjSource<V>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(FltObjFunUtil.concat(To.source(sources)));
	}

	public static <V> FltObjOutlet<V> empty() {
		return of(FltObjFunUtil.nullSource());
	}

	public static <V> FltObjOutlet<List<V>> of(ListMultimap<Float, V> multimap) {
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

	public static <V> FltObjOutlet<V> of(FltObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> FltObjOutlet<V> of(FltObjPair<V>... kvs) {
		return of(new FltObjSource<>() {
			private int i;

			public boolean source2(FltObjPair<V> pair) {
				var b = i < kvs.length;
				if (b) {
					FltObjPair<V> kv = kvs[i];
					pair.update(kv.t0, kv.t1);
				}
				return b;

			}
		});
	}

	public static <V> FltObjOutlet<V> of(Iterable<FltObjPair<V>> col) {
		var iter = col.iterator();
		return of(new FltObjSource<>() {
			public boolean source2(FltObjPair<V> pair) {
				var b = iter.hasNext();
				if (b) {
					FltObjPair<V> pair1 = iter.next();
					pair.update(pair1.t0, pair1.t1);
				}
				return b;
			}
		});
	}

	public static <V> FltObjOutlet<V> of(FltObjSource<V> source) {
		return new FltObjOutlet<>(source);
	}

	private FltObjOutlet(FltObjSource<V> source) {
		this.source = source;
	}

	public Outlet<FltObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(FltObjOutlet<V>::new, FltObjFunUtil.chunk(n, source)));
	}

	public FltObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<FltObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(FltObj_Obj<V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(FltObj_Obj<V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> FltObjOutlet<V1> concatMapFltObj(FltObj_Obj<V, FltObjOutlet<V1>> fun) {
		return of(FltObjFunUtil.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> FltObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(FltObjFunUtil.concat(FltObjFunUtil.map((k, v) -> {
			var source = fun.apply(v).source();
			return pair -> {
				var value1 = source.g();
				var b = value1 != null;
				if (b)
					pair.update(k, value1);
				return b;
			};
		}, source)));
	}

	public FltObjOutlet<V> cons(float key, V value) {
		return of(FltObjFunUtil.cons(key, value, source));
	}

	public int count() {
		var pair = FltObjPair.of(empty, (V) null);
		var i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public FltObjOutlet<V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(FltObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public FltObjOutlet<V> drop(int n) {
		var pair = FltObjPair.of(empty, (V) null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltObjOutlet.class) {
			@SuppressWarnings("unchecked")
			var outlet = (FltObjOutlet<V>) (FltObjOutlet<?>) object;
			var source2 = outlet.source;
			boolean b, b0, b1;
			var pair0 = FltObjPair.of(empty, (V) null);
			var pair1 = FltObjPair.of(empty, (V) null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public FltObjOutlet<V> filter(FltObjPredicate<V> fun) {
		return of(FltObjFunUtil.filter(fun, source));
	}

	public FltObjOutlet<V> filterKey(FltTest fun) {
		return of(FltObjFunUtil.filterKey(fun, source));
	}

	public FltObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(FltObjFunUtil.filterValue(fun, source));
	}

	public FltObjPair<V> first() {
		var pair = FltObjPair.of(empty, (V) null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(FltObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(FltObjFunUtil.map(fun, source)));
	}

	public FltObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> FltObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var pair = FltObjPair.of(empty, (V) null);
		var h = 7;
		while (next(pair))
			h = h * 31 + pair.hashCode();
		return h;
	}

	public boolean isAll(FltObjPredicate<V> pred) {
		return FltObjFunUtil.isAll(pred, source);
	}

	public boolean isAny(FltObjPredicate<V> pred) {
		return FltObjFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<FltObjPair<V>> iterator() {
		return FltObjFunUtil.iterator(source);
	}

	public FltOutlet keys() {
		return FltOutlet.of(() -> {
			var pair = FltObjPair.of(empty, (V) null);
			return next(pair) ? pair.t0 : empty;
		});
	}

	public FltObjPair<V> last() {
		var pair = FltObjPair.of(empty, (V) null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Outlet<O> map(FltObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Outlet2<K1, V1> map2(FltObj_Obj<V, K1> kf, FltObj_Obj<V, V1> vf) {
		return Outlet2.of(FltObjFunUtil.map2(kf, vf, source));
	}

	public <V1> FltObjOutlet<V1> mapFltObj(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return mapFltObj_(kf, vf);
	}

	public <V1> FltObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapFltObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public FltObjPair<V> min(Comparator<FltObjPair<V>> comparator) {
		var pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			return fail("no result");
	}

	public FltObjPair<V> minOrNull(Comparator<FltObjPair<V>> comparator) {
		var pair = FltObjPair.of(empty, (V) null);
		var pair1 = FltObjPair.of(empty, (V) null);
		if (next(pair)) {
			while (next(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.t0, pair1.t1);
			return pair;
		} else
			return null;
	}

	public FltObjOutlet<V> nonBlocking(Float k0, V v0) {
		var queue = new NullableSyncQueue<FltObjPair<V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = FltObjPair.of(empty, (V) null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new FltObjOutlet<>(pair -> {
			var mutable = Mutable.<FltObjPair<V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.value();
				pair.update(p.t0, p.t1);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public FltObjPair<V> opt() {
		var pair = FltObjPair.of(empty, (V) null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				return fail("more than one result");
		else
			return FltObjPair.none();
	}

	public Outlet<FltObjPair<V>> pairs() {
		return Outlet.of(() -> {
			var pair = FltObjPair.of(empty, (V) null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<FltObjOutlet<V>, FltObjOutlet<V>> partition(FltObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public FltObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink2<Float, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = FltObjPair.of(empty, (V) null);
		while (next(pair))
			sink1.sink2(pair.t0, pair.t1);
	}

	public FltObjOutlet<V> skip(int n) {
		var pair = FltObjPair.of(empty, (V) null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source) : empty();
	}

	public FltObjOutlet<V> snoc(Float key, V value) {
		return of(FltObjFunUtil.snoc(key, value, source));
	}

	public FltObjOutlet<V> sort(Comparator<FltObjPair<V>> comparator) {
		var list = new ArrayList<FltObjPair<V>>();
		FltObjPair<V> pair;
		while (next(pair = FltObjPair.of(empty, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> FltObjOutlet<V> sortBy(FltObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public FltObjOutlet<V> sortByKey(Comparator<Float> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public FltObjOutlet<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public FltObjSource<V> source() {
		return source;
	}

	public Outlet<FltObjOutlet<V>> split(FltObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(FltObjOutlet<V>::new, FltObjFunUtil.split(fun, source)));
	}

	public FltObjOutlet<V> take(int n) {
		return of(new FltObjSource<>() {
			private int count = n;

			public boolean source2(FltObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public FltObjPair<V>[] toArray() {
		var list = toList();
		@SuppressWarnings("unchecked")
		FltObjPair<V>[] array = Array_.newArray(FltObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<FltObjPair<V>> toList() {
		var list = new ArrayList<FltObjPair<V>>();
		FltObjPair<V> pair;
		while (next(pair = FltObjPair.of(empty, null)))
			list.add(pair);
		return list;
	}

	public FltObjMap<List<V>> toListMap() {
		var map = new FltObjMap<List<V>>();
		var pair = FltObjPair.of(empty, (V) null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public FltObjMap<V> toMap() {
		var map = new FltObjMap<V>();
		var pair = FltObjPair.of(empty, (V) null);
		while (source.source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ListMultimap<Float, V> toMultimap() {
		var map = new ListMultimap<Float, V>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public ObjFltMap<V> toObjFltMap() {
		var pair = FltObjPair.of(empty, (V) null);
		var map = new ObjFltMap<V>();
		while (source.source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public Set<FltObjPair<V>> toSet() {
		var set = new HashSet<FltObjPair<V>>();
		FltObjPair<V> pair;
		while (next(pair = FltObjPair.of(empty, null)))
			set.add(pair);
		return set;

	}

	public FltObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Outlet<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Outlet<O> map_(FltObj_Obj<V, O> fun0) {
		return Outlet.of(FltObjFunUtil.map(fun0, source));
	}

	private <V1> FltObjOutlet<V1> mapFltObj_(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return of(FltObjFunUtil.mapFltObj(kf, vf, source));
	}

	private boolean next(FltObjPair<V> pair) {
		return source.source2(pair);
	}

}
