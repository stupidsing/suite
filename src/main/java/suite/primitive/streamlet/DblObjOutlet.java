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
import suite.primitive.DblFunUtil;
import suite.primitive.DblObjFunUtil;
import suite.primitive.DblObj_Dbl;
import suite.primitive.DblPrimitives.DblObjPredicate;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.DblPrimitives.DblTest;
import suite.primitive.adt.map.DblObjMap;
import suite.primitive.adt.map.ObjDblMap;
import suite.primitive.adt.pair.DblObjPair;
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

public class DblObjOutlet<V> implements OutletDefaults<DblObjPair<V>> {

	private static double EMPTYVALUE = DblFunUtil.EMPTYVALUE;

	private DblObjSource<V> source;

	@SafeVarargs
	public static <V> DblObjOutlet<V> concat(DblObjOutlet<V>... outlets) {
		var sources = new ArrayList<DblObjSource<V>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(DblObjFunUtil.concat(To.source(sources)));
	}

	public static <V> DblObjOutlet<V> empty() {
		return of(DblObjFunUtil.nullSource());
	}

	public static <V> DblObjOutlet<List<V>> of(ListMultimap<Double, V> multimap) {
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

	public static <V> DblObjOutlet<V> of(DblObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> DblObjOutlet<V> of(DblObjPair<V>... kvs) {
		return of(new DblObjSource<>() {
			private int i;

			public boolean source2(DblObjPair<V> pair) {
				var b = i < kvs.length;
				if (b) {
					DblObjPair<V> kv = kvs[i];
					pair.update(kv.t0, kv.t1);
				}
				return b;

			}
		});
	}

	public static <V> DblObjOutlet<V> of(Iterable<DblObjPair<V>> col) {
		var iter = col.iterator();
		return of(new DblObjSource<>() {
			public boolean source2(DblObjPair<V> pair) {
				var b = iter.hasNext();
				if (b) {
					DblObjPair<V> pair1 = iter.next();
					pair.update(pair1.t0, pair1.t1);
				}
				return b;
			}
		});
	}

	public static <V> DblObjOutlet<V> of(DblObjSource<V> source) {
		return new DblObjOutlet<>(source);
	}

	private DblObjOutlet(DblObjSource<V> source) {
		this.source = source;
	}

	public Outlet<DblObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(DblObjOutlet<V>::new, DblObjFunUtil.chunk(n, source)));
	}

	public DblObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<DblObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(DblObj_Obj<V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(DblObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(DblObj_Obj<V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(DblObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> DblObjOutlet<V1> concatMapDblObj(DblObj_Obj<V, DblObjOutlet<V1>> fun) {
		return of(DblObjFunUtil.concat(DblObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> DblObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(DblObjFunUtil.concat(DblObjFunUtil.map((k, v) -> {
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

	public DblObjOutlet<V> cons(double key, V value) {
		return of(DblObjFunUtil.cons(key, value, source));
	}

	public int count() {
		var pair = DblObjPair.<V> of((double) 0, null);
		var i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public DblObjOutlet<V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(DblObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public DblObjOutlet<V> drop(int n) {
		var pair = DblObjPair.<V> of((double) 0, null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblObjOutlet.class) {
			@SuppressWarnings("unchecked")
			var outlet = (DblObjOutlet<V>) (DblObjOutlet<?>) object;
			var source2 = outlet.source;
			boolean b, b0, b1;
			var pair0 = DblObjPair.<V> of((double) 0, null);
			var pair1 = DblObjPair.<V> of((double) 0, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public DblObjOutlet<V> filter(DblObjPredicate<V> fun) {
		return of(DblObjFunUtil.filter(fun, source));
	}

	public DblObjOutlet<V> filterKey(DblTest fun) {
		return of(DblObjFunUtil.filterKey(fun, source));
	}

	public DblObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(DblObjFunUtil.filterValue(fun, source));
	}

	public DblObjPair<V> first() {
		var pair = DblObjPair.<V> of((double) 0, null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(DblObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(DblObjFunUtil.map(fun, source)));
	}

	public DblObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> DblObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var pair = DblObjPair.<V> of((double) 0, null);
		var h = 7;
		while (next(pair))
			h = h * 31 + pair.hashCode();
		return h;
	}

	public boolean isAll(DblObjPredicate<V> pred) {
		return DblObjFunUtil.isAll(pred, source);
	}

	public boolean isAny(DblObjPredicate<V> pred) {
		return DblObjFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<DblObjPair<V>> iterator() {
		return DblObjFunUtil.iterator(source);
	}

	public DblOutlet keys() {
		return DblOutlet.of(() -> {
			var pair = DblObjPair.<V> of((double) 0, null);
			return next(pair) ? pair.t0 : EMPTYVALUE;
		});
	}

	public DblObjPair<V> last() {
		var pair = DblObjPair.<V> of((double) 0, null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Outlet<O> map(DblObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Outlet2<K1, V1> map2(DblObj_Obj<V, K1> kf, DblObj_Obj<V, V1> vf) {
		return Outlet2.of(DblObjFunUtil.map2(kf, vf, source));
	}

	public <V1> DblObjOutlet<V1> mapDblObj(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return mapDblObj_(kf, vf);
	}

	public <V1> DblObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapDblObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public DblObjPair<V> min(Comparator<DblObjPair<V>> comparator) {
		var pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			return fail("no result");
	}

	public DblObjPair<V> minOrNull(Comparator<DblObjPair<V>> comparator) {
		var pair = DblObjPair.<V> of((double) 0, null);
		var pair1 = DblObjPair.<V> of((double) 0, null);
		if (next(pair)) {
			while (next(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.t0, pair1.t1);
			return pair;
		} else
			return null;
	}

	public DblObjOutlet<V> nonBlocking(Double k0, V v0) {
		var queue = new NullableSyncQueue<DblObjPair<V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = DblObjPair.<V> of((double) 0, null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new DblObjOutlet<>(pair -> {
			var mutable = Mutable.<DblObjPair<V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.value();
				pair.update(p.t0, p.t1);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public DblObjPair<V> opt() {
		var pair = DblObjPair.<V> of((double) 0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				return fail("more than one result");
		else
			return DblObjPair.none();
	}

	public Outlet<DblObjPair<V>> pairs() {
		return Outlet.of(() -> {
			var pair = DblObjPair.<V> of((double) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<DblObjOutlet<V>, DblObjOutlet<V>> partition(DblObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public DblObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink2<Double, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = DblObjPair.<V> of((double) 0, null);
		while (next(pair))
			sink1.sink2(pair.t0, pair.t1);
	}

	public DblObjOutlet<V> skip(int n) {
		var pair = DblObjPair.<V> of((double) 0, null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source) : empty();
	}

	public DblObjOutlet<V> snoc(Double key, V value) {
		return of(DblObjFunUtil.snoc(key, value, source));
	}

	public DblObjOutlet<V> sort(Comparator<DblObjPair<V>> comparator) {
		var list = new ArrayList<DblObjPair<V>>();
		DblObjPair<V> pair;
		while (next(pair = DblObjPair.of((double) 0, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> DblObjOutlet<V> sortBy(DblObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public DblObjOutlet<V> sortByKey(Comparator<Double> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public DblObjOutlet<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public DblObjSource<V> source() {
		return source;
	}

	public Outlet<DblObjOutlet<V>> split(DblObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(DblObjOutlet<V>::new, DblObjFunUtil.split(fun, source)));
	}

	public DblObjOutlet<V> take(int n) {
		return of(new DblObjSource<>() {
			private int count = n;

			public boolean source2(DblObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public DblObjPair<V>[] toArray() {
		var list = toList();
		@SuppressWarnings("unchecked")
		DblObjPair<V>[] array = Array_.newArray(DblObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<DblObjPair<V>> toList() {
		var list = new ArrayList<DblObjPair<V>>();
		DblObjPair<V> pair;
		while (next(pair = DblObjPair.of((double) 0, null)))
			list.add(pair);
		return list;
	}

	public DblObjMap<List<V>> toListMap() {
		var map = new DblObjMap<List<V>>();
		var pair = DblObjPair.<V> of((double) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public DblObjMap<V> toMap() {
		var map = new DblObjMap<V>();
		var pair = DblObjPair.<V> of((double) 0, null);
		while (source.source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ListMultimap<Double, V> toMultimap() {
		var map = new ListMultimap<Double, V>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public ObjDblMap<V> toObjDblMap() {
		var pair = DblObjPair.<V> of((double) 0, null);
		var map = new ObjDblMap<V>();
		while (source.source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public Set<DblObjPair<V>> toSet() {
		var set = new HashSet<DblObjPair<V>>();
		DblObjPair<V> pair;
		while (next(pair = DblObjPair.of((double) 0, null)))
			set.add(pair);
		return set;

	}

	public DblObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Outlet<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Outlet<O> map_(DblObj_Obj<V, O> fun0) {
		return Outlet.of(DblObjFunUtil.map(fun0, source));
	}

	private <V1> DblObjOutlet<V1> mapDblObj_(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return of(DblObjFunUtil.mapDblObj(kf, vf, source));
	}

	private boolean next(DblObjPair<V> pair) {
		return source.source2(pair);
	}

}
