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
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil2;
import suite.streamlet.FunUtil2.Sink2;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.PullerDefaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Array_;
import suite.util.List_;
import suite.util.NullableSyncQueue;
import suite.util.To;

public class LngObjPuller<V> implements PullerDefaults<LngObjPair<V>> {

	private static long empty = LngFunUtil.EMPTYVALUE;

	private LngObjSource<V> source;

	@SafeVarargs
	public static <V> LngObjPuller<V> concat(LngObjPuller<V>... outlets) {
		var sources = new ArrayList<LngObjSource<V>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(LngObjFunUtil.concat(To.source(sources)));
	}

	public static <V> LngObjPuller<V> empty() {
		return of(LngObjFunUtil.nullSource());
	}

	public static <V> LngObjPuller<List<V>> of(ListMultimap<Long, V> multimap) {
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

	public static <V> LngObjPuller<V> of(LngObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> LngObjPuller<V> of(LngObjPair<V>... kvs) {
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

	public static <V> LngObjPuller<V> of(Iterable<LngObjPair<V>> col) {
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

	public static <V> LngObjPuller<V> of(LngObjSource<V> source) {
		return new LngObjPuller<>(source);
	}

	private LngObjPuller(LngObjSource<V> source) {
		this.source = source;
	}

	public Puller<LngObjPuller<V>> chunk(int n) {
		return Puller.of(FunUtil.map(LngObjPuller<V>::new, LngObjFunUtil.chunk(n, source)));
	}

	public LngObjPuller<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = pull(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<LngObjPuller<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(LngObj_Obj<V, Puller<O>> fun) {
		return Puller.of(FunUtil.concat(LngObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Puller2<K1, V1> concatMap2(LngObj_Obj<V, Puller2<K1, V1>> fun) {
		return Puller2.of(FunUtil2.concat(LngObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> LngObjPuller<V1> concatMapLngObj(LngObj_Obj<V, LngObjPuller<V1>> fun) {
		return of(LngObjFunUtil.concat(LngObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> LngObjPuller<V1> concatMapValue(Fun<V, Puller<V1>> fun) {
		return of(LngObjFunUtil.concat(LngObjFunUtil.map((k, v) -> {
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

	public LngObjPuller<V> cons(long key, V value) {
		return of(LngObjFunUtil.cons(key, value, source));
	}

	public int count() {
		var pair = LngObjPair.of(empty, (V) null);
		var i = 0;
		while (pull(pair))
			i++;
		return i;
	}

	public LngObjPuller<V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = pull(pair)) && !set.add(LngObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public LngObjPuller<V> drop(int n) {
		var pair = LngObjPair.of(empty, (V) null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngObjPuller.class) {
			@SuppressWarnings("unchecked")
			var outlet = (LngObjPuller<V>) (LngObjPuller<?>) object;
			var source2 = outlet.source;
			boolean b, b0, b1;
			var pair0 = LngObjPair.of(empty, (V) null);
			var pair1 = LngObjPair.of(empty, (V) null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public LngObjPuller<V> filter(LngObjPredicate<V> fun) {
		return of(LngObjFunUtil.filter(fun, source));
	}

	public LngObjPuller<V> filterKey(LngTest fun) {
		return of(LngObjFunUtil.filterKey(fun, source));
	}

	public LngObjPuller<V> filterValue(Predicate<V> fun) {
		return of(LngObjFunUtil.filterValue(fun, source));
	}

	public LngObjPair<V> first() {
		var pair = LngObjPair.of(empty, (V) null);
		return pull(pair) ? pair : null;
	}

	public <O> Puller<O> flatMap(LngObj_Obj<V, Iterable<O>> fun) {
		return Puller.of(FunUtil.flatten(LngObjFunUtil.map(fun, source)));
	}

	public LngObjPuller<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> LngObjPuller<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var pair = LngObjPair.of(empty, (V) null);
		var h = 7;
		while (pull(pair))
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

	public LngPuller keys() {
		return LngPuller.of(() -> {
			var pair = LngObjPair.of(empty, (V) null);
			return pull(pair) ? pair.t0 : empty;
		});
	}

	public LngObjPair<V> last() {
		var pair = LngObjPair.of(empty, (V) null);
		if (pull(pair))
			while (pull(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Puller<O> map(LngObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Puller2<K1, V1> map2(LngObj_Obj<V, K1> kf, LngObj_Obj<V, V1> vf) {
		return Puller2.of(LngObjFunUtil.map2(kf, vf, source));
	}

	public <V1> LngObjPuller<V1> mapLngObj(LngObj_Lng<V> kf, LngObj_Obj<V, V1> vf) {
		return mapLngObj_(kf, vf);
	}

	public <V1> LngObjPuller<V1> mapValue(Fun<V, V1> fun) {
		return mapLngObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public LngObjPair<V> min(Comparator<LngObjPair<V>> comparator) {
		var pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			return fail("no result");
	}

	public LngObjPair<V> minOrNull(Comparator<LngObjPair<V>> comparator) {
		var pair = LngObjPair.of(empty, (V) null);
		var pair1 = LngObjPair.of(empty, (V) null);
		if (pull(pair)) {
			while (pull(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.t0, pair1.t1);
			return pair;
		} else
			return null;
	}

	public LngObjPuller<V> nonBlocking(Long k0, V v0) {
		var queue = new NullableSyncQueue<LngObjPair<V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = LngObjPair.of(empty, (V) null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new LngObjPuller<>(pair -> {
			var mutable = Mutable.<LngObjPair<V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.value();
				pair.update(p.t0, p.t1);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public LngObjPair<V> opt() {
		var pair = LngObjPair.of(empty, (V) null);
		if (pull(pair))
			if (!pull(pair))
				return pair;
			else
				return fail("more than one result");
		else
			return LngObjPair.none();
	}

	public Puller<LngObjPair<V>> pairs() {
		return Puller.of(() -> {
			var pair = LngObjPair.of(empty, (V) null);
			return pull(pair) ? pair : null;
		});
	}

	public Pair<LngObjPuller<V>, LngObjPuller<V>> partition(LngObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public LngObjPuller<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink2<Long, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = LngObjPair.of(empty, (V) null);
		while (pull(pair))
			sink1.sink2(pair.t0, pair.t1);
	}

	public LngObjPuller<V> skip(int n) {
		var pair = LngObjPair.of(empty, (V) null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull(pair);
		return !end ? of(source) : empty();
	}

	public LngObjPuller<V> snoc(Long key, V value) {
		return of(LngObjFunUtil.snoc(key, value, source));
	}

	public LngObjPuller<V> sort(Comparator<LngObjPair<V>> comparator) {
		var list = new ArrayList<LngObjPair<V>>();
		LngObjPair<V> pair;
		while (pull(pair = LngObjPair.of(empty, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> LngObjPuller<V> sortBy(LngObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public LngObjPuller<V> sortByKey(Comparator<Long> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public LngObjPuller<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public LngObjSource<V> source() {
		return source;
	}

	public Puller<LngObjPuller<V>> split(LngObjPredicate<V> fun) {
		return Puller.of(FunUtil.map(LngObjPuller<V>::new, LngObjFunUtil.split(fun, source)));
	}

	public LngObjPuller<V> take(int n) {
		return of(new LngObjSource<>() {
			private int count = n;

			public boolean source2(LngObjPair<V> pair) {
				return 0 < count-- ? pull(pair) : false;
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
		while (pull(pair = LngObjPair.of(empty, null)))
			list.add(pair);
		return list;
	}

	public LngObjMap<List<V>> toListMap() {
		var map = new LngObjMap<List<V>>();
		var pair = LngObjPair.of(empty, (V) null);
		while (pull(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public LngObjMap<V> toMap() {
		var map = new LngObjMap<V>();
		var pair = LngObjPair.of(empty, (V) null);
		while (source.source2(pair))
			map.put(pair.t0, pair.t1);
		return map;
	}

	public ListMultimap<Long, V> toMultimap() {
		var map = new ListMultimap<Long, V>();
		groupBy().concatMapValue(Puller::of).sink(map::put);
		return map;
	}

	public ObjLngMap<V> toObjLngMap() {
		var pair = LngObjPair.of(empty, (V) null);
		var map = new ObjLngMap<V>();
		while (source.source2(pair))
			map.put(pair.t1, pair.t0);
		return map;
	}

	public Set<LngObjPair<V>> toSet() {
		var set = new HashSet<LngObjPair<V>>();
		LngObjPair<V> pair;
		while (pull(pair = LngObjPair.of(empty, null)))
			set.add(pair);
		return set;

	}

	public LngObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Puller<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Puller<O> map_(LngObj_Obj<V, O> fun0) {
		return Puller.of(LngObjFunUtil.map(fun0, source));
	}

	private <V1> LngObjPuller<V1> mapLngObj_(LngObj_Lng<V> kf, LngObj_Obj<V, V1> vf) {
		return of(LngObjFunUtil.mapLngObj(kf, vf, source));
	}

	private boolean pull(LngObjPair<V> pair) {
		return source.source2(pair);
	}

}
