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
import suite.primitive.ChrFunUtil;
import suite.primitive.ChrObjFunUtil;
import suite.primitive.ChrObj_Chr;
import suite.primitive.ChrPrimitives.ChrObjPredicate;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.ChrObj_Obj;
import suite.primitive.ChrPrimitives.ChrTest;
import suite.primitive.adt.map.ChrObjMap;
import suite.primitive.adt.map.ObjChrMap;
import suite.primitive.adt.pair.ChrObjPair;
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

public class ChrObjOutlet<V> implements OutletDefaults<ChrObjPair<V>> {

	private static char EMPTYVALUE = ChrFunUtil.EMPTYVALUE;

	private ChrObjSource<V> source;

	@SafeVarargs
	public static <V> ChrObjOutlet<V> concat(ChrObjOutlet<V>... outlets) {
		var sources = new ArrayList<ChrObjSource<V>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(ChrObjFunUtil.concat(To.source(sources)));
	}

	public static <V> ChrObjOutlet<V> empty() {
		return of(ChrObjFunUtil.nullSource());
	}

	public static <V> ChrObjOutlet<List<V>> of(ListMultimap<Character, V> multimap) {
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

	public static <V> ChrObjOutlet<V> of(ChrObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> ChrObjOutlet<V> of(ChrObjPair<V>... kvs) {
		return of(new ChrObjSource<>() {
			private int i;

			public boolean source2(ChrObjPair<V> pair) {
				var b = i < kvs.length;
				if (b) {
					ChrObjPair<V> kv = kvs[i];
					pair.update(kv.t0, kv.t1);
				}
				return b;

			}
		});
	}

	public static <V> ChrObjOutlet<V> of(Iterable<ChrObjPair<V>> col) {
		var iter = col.iterator();
		return of(new ChrObjSource<>() {
			public boolean source2(ChrObjPair<V> pair) {
				var b = iter.hasNext();
				if (b) {
					ChrObjPair<V> pair1 = iter.next();
					pair.update(pair1.t0, pair1.t1);
				}
				return b;
			}
		});
	}

	public static <V> ChrObjOutlet<V> of(ChrObjSource<V> source) {
		return new ChrObjOutlet<>(source);
	}

	private ChrObjOutlet(ChrObjSource<V> source) {
		this.source = source;
	}

	public Outlet<ChrObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(ChrObjOutlet<V>::new, ChrObjFunUtil.chunk(n, source)));
	}

	public ChrObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<ChrObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(ChrObj_Obj<V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(ChrObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(ChrObj_Obj<V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(ChrObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> ChrObjOutlet<V1> concatMapChrObj(ChrObj_Obj<V, ChrObjOutlet<V1>> fun) {
		return of(ChrObjFunUtil.concat(ChrObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> ChrObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(ChrObjFunUtil.concat(ChrObjFunUtil.map((k, v) -> {
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

	public ChrObjOutlet<V> cons(char key, V value) {
		return of(ChrObjFunUtil.cons(key, value, source));
	}

	public int count() {
		var pair = ChrObjPair.<V> of((char) 0, null);
		var i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public ChrObjOutlet<V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(ChrObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public ChrObjOutlet<V> drop(int n) {
		var pair = ChrObjPair.<V> of((char) 0, null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrObjOutlet.class) {
			@SuppressWarnings("unchecked")
			var outlet = (ChrObjOutlet<V>) (ChrObjOutlet<?>) object;
			var source2 = outlet.source;
			boolean b, b0, b1;
			var pair0 = ChrObjPair.<V> of((char) 0, null);
			var pair1 = ChrObjPair.<V> of((char) 0, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public ChrObjOutlet<V> filter(ChrObjPredicate<V> fun) {
		return of(ChrObjFunUtil.filter(fun, source));
	}

	public ChrObjOutlet<V> filterKey(ChrTest fun) {
		return of(ChrObjFunUtil.filterKey(fun, source));
	}

	public ChrObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(ChrObjFunUtil.filterValue(fun, source));
	}

	public ChrObjPair<V> first() {
		var pair = ChrObjPair.<V> of((char) 0, null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(ChrObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(ChrObjFunUtil.map(fun, source)));
	}

	public ChrObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> ChrObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var pair = ChrObjPair.<V> of((char) 0, null);
		var h = 7;
		while (next(pair))
			h = h * 31 + pair.hashCode();
		return h;
	}

	public boolean isAll(ChrObjPredicate<V> pred) {
		return ChrObjFunUtil.isAll(pred, source);
	}

	public boolean isAny(ChrObjPredicate<V> pred) {
		return ChrObjFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<ChrObjPair<V>> iterator() {
		return ChrObjFunUtil.iterator(source);
	}

	public ChrOutlet keys() {
		return ChrOutlet.of(() -> {
			var pair = ChrObjPair.<V> of((char) 0, null);
			return next(pair) ? pair.t0 : EMPTYVALUE;
		});
	}

	public ChrObjPair<V> last() {
		var pair = ChrObjPair.<V> of((char) 0, null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Outlet<O> map(ChrObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Outlet2<K1, V1> map2(ChrObj_Obj<V, K1> kf, ChrObj_Obj<V, V1> vf) {
		return Outlet2.of(ChrObjFunUtil.map2(kf, vf, source));
	}

	public <V1> ChrObjOutlet<V1> mapChrObj(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return mapChrObj_(kf, vf);
	}

	public <V1> ChrObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapChrObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public ChrObjPair<V> min(Comparator<ChrObjPair<V>> comparator) {
		var pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			return Fail.t("no result");
	}

	public ChrObjPair<V> minOrNull(Comparator<ChrObjPair<V>> comparator) {
		var pair = ChrObjPair.<V> of((char) 0, null);
		var pair1 = ChrObjPair.<V> of((char) 0, null);
		if (next(pair)) {
			while (next(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.t0, pair1.t1);
			return pair;
		} else
			return null;
	}

	public ChrObjOutlet<V> nonBlocking(Character k0, V v0) {
		var queue = new NullableSyncQueue<ChrObjPair<V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = ChrObjPair.<V> of((char) 0, null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new ChrObjOutlet<>(pair -> {
			var mutable = Mutable.<ChrObjPair<V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.get();
				pair.update(p.t0, p.t1);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public ChrObjPair<V> opt() {
		var pair = ChrObjPair.<V> of((char) 0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				return Fail.t("more than one result");
		else
			return ChrObjPair.none();
	}

	public Outlet<ChrObjPair<V>> pairs() {
		return Outlet.of(() -> {
			var pair = ChrObjPair.<V> of((char) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<ChrObjOutlet<V>, ChrObjOutlet<V>> partition(ChrObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public ChrObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink2<Character, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = ChrObjPair.<V> of((char) 0, null);
		while (next(pair))
			sink1.sink2(pair.t0, pair.t1);
	}

	public ChrObjOutlet<V> skip(int n) {
		var pair = ChrObjPair.<V> of((char) 0, null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source) : empty();
	}

	public ChrObjOutlet<V> snoc(Character key, V value) {
		return of(ChrObjFunUtil.snoc(key, value, source));
	}

	public ChrObjOutlet<V> sort(Comparator<ChrObjPair<V>> comparator) {
		var list = new ArrayList<ChrObjPair<V>>();
		ChrObjPair<V> pair;
		while (next(pair = ChrObjPair.of((char) 0, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> ChrObjOutlet<V> sortBy(ChrObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public ChrObjOutlet<V> sortByKey(Comparator<Character> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public ChrObjOutlet<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public ChrObjSource<V> source() {
		return source;
	}

	public Outlet<ChrObjOutlet<V>> split(ChrObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(ChrObjOutlet<V>::new, ChrObjFunUtil.split(fun, source)));
	}

	public ChrObjOutlet<V> take(int n) {
		return of(new ChrObjSource<>() {
			private int count = n;

			public boolean source2(ChrObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public ChrObjPair<V>[] toArray() {
		var list = toList();
		@SuppressWarnings("unchecked")
		ChrObjPair<V>[] array = Array_.newArray(ChrObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<ChrObjPair<V>> toList() {
		var list = new ArrayList<ChrObjPair<V>>();
		ChrObjPair<V> pair;
		while (next(pair = ChrObjPair.of((char) 0, null)))
			list.add(pair);
		return list;
	}

	public ChrObjMap<List<V>> toListMap() {
		var map = new ChrObjMap<List<V>>();
		var pair = ChrObjPair.<V> of((char) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public ChrObjMap<V> toMap() {
		var map = new ChrObjMap<V>();
		var pair = ChrObjPair.<V> of((char) 0, null);
		while (source.source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ListMultimap<Character, V> toMultimap() {
		var map = new ListMultimap<Character, V>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public ObjChrMap<V> toObjChrMap() {
		var pair = ChrObjPair.<V> of((char) 0, null);
		var map = new ObjChrMap<V>();
		while (source.source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public Set<ChrObjPair<V>> toSet() {
		var set = new HashSet<ChrObjPair<V>>();
		ChrObjPair<V> pair;
		while (next(pair = ChrObjPair.of((char) 0, null)))
			set.add(pair);
		return set;

	}

	public ChrObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Outlet<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Outlet<O> map_(ChrObj_Obj<V, O> fun0) {
		return Outlet.of(ChrObjFunUtil.map(fun0, source));
	}

	private <V1> ChrObjOutlet<V1> mapChrObj_(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return of(ChrObjFunUtil.mapChrObj(kf, vf, source));
	}

	private boolean next(ChrObjPair<V> pair) {
		return source.source2(pair);
	}

}
