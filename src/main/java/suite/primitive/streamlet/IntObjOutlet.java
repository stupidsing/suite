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
import suite.object.Object_;
import suite.primitive.IntFunUtil;
import suite.primitive.IntObjFunUtil;
import suite.primitive.IntObj_Int;
import suite.primitive.IntPrimitives.IntObjPredicate;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.IntTest;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.pair.IntObjPair;
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
import suite.util.Fail;
import suite.util.List_;
import suite.util.NullableSyncQueue;
import suite.util.To;

public class IntObjOutlet<V> implements OutletDefaults<IntObjPair<V>> {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private IntObjSource<V> source;

	@SafeVarargs
	public static <V> IntObjOutlet<V> concat(IntObjOutlet<V>... outlets) {
		var sources = new ArrayList<IntObjSource<V>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(IntObjFunUtil.concat(To.source(sources)));
	}

	public static <V> IntObjOutlet<V> empty() {
		return of(IntObjFunUtil.nullSource());
	}

	public static <V> IntObjOutlet<List<V>> of(ListMultimap<Integer, V> multimap) {
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

	public static <V> IntObjOutlet<V> of(IntObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> IntObjOutlet<V> of(IntObjPair<V>... kvs) {
		return of(new IntObjSource<>() {
			private int i;

			public boolean source2(IntObjPair<V> pair) {
				var b = i < kvs.length;
				if (b) {
					IntObjPair<V> kv = kvs[i];
					pair.update(kv.t0, kv.t1);
				}
				return b;

			}
		});
	}

	public static <V> IntObjOutlet<V> of(Iterable<IntObjPair<V>> col) {
		var iter = col.iterator();
		return of(new IntObjSource<>() {
			public boolean source2(IntObjPair<V> pair) {
				var b = iter.hasNext();
				if (b) {
					IntObjPair<V> pair1 = iter.next();
					pair.update(pair1.t0, pair1.t1);
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

	public Outlet<IntObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(IntObjOutlet<V>::new, IntObjFunUtil.chunk(n, source)));
	}

	public IntObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = next(pair);
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

	public IntObjOutlet<V> cons(int key, V value) {
		return of(IntObjFunUtil.cons(key, value, source));
	}

	public int count() {
		var pair = IntObjPair.<V> of((int) 0, null);
		var i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public IntObjOutlet<V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(IntObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public IntObjOutlet<V> drop(int n) {
		var pair = IntObjPair.<V> of((int) 0, null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntObjOutlet.class) {
			@SuppressWarnings("unchecked")
			var outlet = (IntObjOutlet<V>) (IntObjOutlet<?>) object;
			var source2 = outlet.source;
			boolean b, b0, b1;
			var pair0 = IntObjPair.<V> of((int) 0, null);
			var pair1 = IntObjPair.<V> of((int) 0, null);
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

	public IntObjOutlet<V> filterKey(IntTest fun) {
		return of(IntObjFunUtil.filterKey(fun, source));
	}

	public IntObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(IntObjFunUtil.filterValue(fun, source));
	}

	public IntObjPair<V> first() {
		var pair = IntObjPair.<V> of((int) 0, null);
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
		var pair = IntObjPair.<V> of((int) 0, null);
		var h = 7;
		while (next(pair))
			h = h * 31 + pair.hashCode();
		return h;
	}

	public boolean isAll(IntObjPredicate<V> pred) {
		return IntObjFunUtil.isAll(pred, source);
	}

	public boolean isAny(IntObjPredicate<V> pred) {
		return IntObjFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<IntObjPair<V>> iterator() {
		return IntObjFunUtil.iterator(source);
	}

	public IntOutlet keys() {
		return IntOutlet.of(() -> {
			var pair = IntObjPair.<V> of((int) 0, null);
			return next(pair) ? pair.t0 : EMPTYVALUE;
		});
	}

	public IntObjPair<V> last() {
		var pair = IntObjPair.<V> of((int) 0, null);
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

	public <V1> IntObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapIntObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public IntObjPair<V> min(Comparator<IntObjPair<V>> comparator) {
		var pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			return Fail.t("no result");
	}

	public IntObjPair<V> minOrNull(Comparator<IntObjPair<V>> comparator) {
		var pair = IntObjPair.<V> of((int) 0, null);
		var pair1 = IntObjPair.<V> of((int) 0, null);
		if (next(pair)) {
			while (next(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.t0, pair1.t1);
			return pair;
		} else
			return null;
	}

	public IntObjOutlet<V> nonBlocking(Integer k0, V v0) {
		var queue = new NullableSyncQueue<IntObjPair<V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = IntObjPair.<V> of((int) 0, null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new IntObjOutlet<>(pair -> {
			var mutable = Mutable.<IntObjPair<V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.get();
				pair.update(p.t0, p.t1);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public IntObjPair<V> opt() {
		var pair = IntObjPair.<V> of((int) 0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				return Fail.t("more than one result");
		else
			return IntObjPair.none();
	}

	public Outlet<IntObjPair<V>> pairs() {
		return Outlet.of(() -> {
			var pair = IntObjPair.<V> of((int) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<IntObjOutlet<V>, IntObjOutlet<V>> partition(IntObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public IntObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink2<Integer, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = IntObjPair.<V> of((int) 0, null);
		while (next(pair))
			sink1.sink2(pair.t0, pair.t1);
	}

	public IntObjOutlet<V> skip(int n) {
		var pair = IntObjPair.<V> of((int) 0, null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source) : empty();
	}

	public IntObjOutlet<V> snoc(Integer key, V value) {
		return of(IntObjFunUtil.snoc(key, value, source));
	}

	public IntObjOutlet<V> sort(Comparator<IntObjPair<V>> comparator) {
		var list = new ArrayList<IntObjPair<V>>();
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
		return of(new IntObjSource<>() {
			private int count = n;

			public boolean source2(IntObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public IntObjPair<V>[] toArray() {
		var list = toList();
		@SuppressWarnings("unchecked")
		IntObjPair<V>[] array = Array_.newArray(IntObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<IntObjPair<V>> toList() {
		var list = new ArrayList<IntObjPair<V>>();
		IntObjPair<V> pair;
		while (next(pair = IntObjPair.of((int) 0, null)))
			list.add(pair);
		return list;
	}

	public IntObjMap<List<V>> toListMap() {
		var map = new IntObjMap<List<V>>();
		var pair = IntObjPair.<V> of((int) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public IntObjMap<V> toMap() {
		var map = new IntObjMap<V>();
		var pair = IntObjPair.<V> of((int) 0, null);
		while (source.source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ListMultimap<Integer, V> toMultimap() {
		var map = new ListMultimap<Integer, V>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public ObjIntMap<V> toObjIntMap() {
		var pair = IntObjPair.<V> of((int) 0, null);
		var map = new ObjIntMap<V>();
		while (source.source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public Set<IntObjPair<V>> toSet() {
		var set = new HashSet<IntObjPair<V>>();
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
