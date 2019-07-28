package suite.primitive.streamlet;

import static suite.util.Fail.fail;

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
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.PullerDefaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.Array_;
import suite.util.List_;
import suite.util.NullableSyncQueue;
import suite.util.To;

public class ChrObjPuller<V> implements PullerDefaults<ChrObjPair<V>> {

	private static char empty = ChrFunUtil.EMPTYVALUE;

	private ChrObjSource<V> source;

	@SafeVarargs
	public static <V> ChrObjPuller<V> concat(ChrObjPuller<V>... outlets) {
		var sources = new ArrayList<ChrObjSource<V>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(ChrObjFunUtil.concat(To.source(sources)));
	}

	public static <V> ChrObjPuller<V> empty() {
		return of(ChrObjFunUtil.nullSource());
	}

	public static <V> ChrObjPuller<List<V>> of(ListMultimap<Character, V> multimap) {
		var iter = multimap.listEntries().iterator();
		return of(pair -> {
			var b = iter.hasNext();
			if (b) {
				var pair1 = iter.next();
				pair.update(pair1.k, pair1.v);
			}
			return b;
		});
	}

	public static <V> ChrObjPuller<V> of(ChrObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> ChrObjPuller<V> of(ChrObjPair<V>... kvs) {
		return of(new ChrObjSource<>() {
			private int i;

			public boolean source2(ChrObjPair<V> pair) {
				var b = i < kvs.length;
				if (b) {
					ChrObjPair<V> kv = kvs[i];
					pair.update(kv.k, kv.v);
				}
				return b;

			}
		});
	}

	public static <V> ChrObjPuller<V> of(Iterable<ChrObjPair<V>> col) {
		var iter = col.iterator();
		return of(new ChrObjSource<>() {
			public boolean source2(ChrObjPair<V> pair) {
				var b = iter.hasNext();
				if (b) {
					ChrObjPair<V> pair1 = iter.next();
					pair.update(pair1.k, pair1.v);
				}
				return b;
			}
		});
	}

	public static <V> ChrObjPuller<V> of(ChrObjSource<V> source) {
		return new ChrObjPuller<>(source);
	}

	private ChrObjPuller(ChrObjSource<V> source) {
		this.source = source;
	}

	public Puller<ChrObjPuller<V>> chunk(int n) {
		return Puller.of(FunUtil.map(ChrObjPuller<V>::new, ChrObjFunUtil.chunk(n, source)));
	}

	public ChrObjPuller<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = pull(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<ChrObjPuller<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(ChrObj_Obj<V, Puller<O>> fun) {
		return Puller.of(FunUtil.concat(ChrObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Puller2<K1, V1> concatMap2(ChrObj_Obj<V, Puller2<K1, V1>> fun) {
		return Puller2.of(FunUtil2.concat(ChrObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> ChrObjPuller<V1> concatMapChrObj(ChrObj_Obj<V, ChrObjPuller<V1>> fun) {
		return of(ChrObjFunUtil.concat(ChrObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> ChrObjPuller<V1> concatMapValue(Fun<V, Puller<V1>> fun) {
		return of(ChrObjFunUtil.concat(ChrObjFunUtil.map((k, v) -> {
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

	public ChrObjPuller<V> cons(char key, V value) {
		return of(ChrObjFunUtil.cons(key, value, source));
	}

	public int count() {
		var pair = ChrObjPair.of(empty, (V) null);
		var i = 0;
		while (pull(pair))
			i++;
		return i;
	}

	public ChrObjPuller<V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = pull(pair)) && !set.add(ChrObjPair.of(pair.k, pair.v)))
				;
			return b;
		});
	}

	public ChrObjPuller<V> drop(int n) {
		var pair = ChrObjPair.of(empty, (V) null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrObjPuller.class) {
			@SuppressWarnings("unchecked")
			var outlet = (ChrObjPuller<V>) (ChrObjPuller<?>) object;
			var source2 = outlet.source;
			boolean b, b0, b1;
			var pair0 = ChrObjPair.of(empty, (V) null);
			var pair1 = ChrObjPair.of(empty, (V) null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public ChrObjPuller<V> filter(ChrObjPredicate<V> fun) {
		return of(ChrObjFunUtil.filter(fun, source));
	}

	public ChrObjPuller<V> filterKey(ChrTest fun) {
		return of(ChrObjFunUtil.filterKey(fun, source));
	}

	public ChrObjPuller<V> filterValue(Predicate<V> fun) {
		return of(ChrObjFunUtil.filterValue(fun, source));
	}

	public ChrObjPair<V> first() {
		var pair = ChrObjPair.of(empty, (V) null);
		return pull(pair) ? pair : null;
	}

	public <O> Puller<O> flatMap(ChrObj_Obj<V, Iterable<O>> fun) {
		return Puller.of(FunUtil.flatten(ChrObjFunUtil.map(fun, source)));
	}

	public ChrObjPuller<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> ChrObjPuller<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var pair = ChrObjPair.of(empty, (V) null);
		var h = 7;
		while (pull(pair))
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

	public ChrPuller keys() {
		return ChrPuller.of(() -> {
			var pair = ChrObjPair.of(empty, (V) null);
			return pull(pair) ? pair.k : empty;
		});
	}

	public ChrObjPair<V> last() {
		var pair = ChrObjPair.of(empty, (V) null);
		if (pull(pair))
			while (pull(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Puller<O> map(ChrObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Puller2<K1, V1> map2(ChrObj_Obj<V, K1> kf, ChrObj_Obj<V, V1> vf) {
		return Puller2.of(ChrObjFunUtil.map2(kf, vf, source));
	}

	public <V1> ChrObjPuller<V1> mapChrObj(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return mapChrObj_(kf, vf);
	}

	public <V1> ChrObjPuller<V1> mapValue(Fun<V, V1> fun) {
		return mapChrObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public ChrObjPair<V> min(Comparator<ChrObjPair<V>> comparator) {
		var pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			return fail("no result");
	}

	public ChrObjPair<V> minOrNull(Comparator<ChrObjPair<V>> comparator) {
		var pair = ChrObjPair.of(empty, (V) null);
		var pair1 = ChrObjPair.of(empty, (V) null);
		if (pull(pair)) {
			while (pull(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.k, pair1.v);
			return pair;
		} else
			return null;
	}

	public ChrObjPuller<V> nonBlocking(Character k0, V v0) {
		var queue = new NullableSyncQueue<ChrObjPair<V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = ChrObjPair.of(empty, (V) null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new ChrObjPuller<>(pair -> {
			var mutable = Mutable.<ChrObjPair<V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.value();
				pair.update(p.k, p.v);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public ChrObjPair<V> opt() {
		var pair = ChrObjPair.of(empty, (V) null);
		if (pull(pair))
			if (!pull(pair))
				return pair;
			else
				return fail("more than one result");
		else
			return ChrObjPair.none();
	}

	public Puller<ChrObjPair<V>> pairs() {
		return Puller.of(() -> {
			var pair = ChrObjPair.of(empty, (V) null);
			return pull(pair) ? pair : null;
		});
	}

	public Pair<ChrObjPuller<V>, ChrObjPuller<V>> partition(ChrObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public ChrObjPuller<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink2<Character, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = ChrObjPair.of(empty, (V) null);
		while (pull(pair))
			sink1.sink2(pair.k, pair.v);
	}

	public ChrObjPuller<V> skip(int n) {
		var pair = ChrObjPair.of(empty, (V) null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull(pair);
		return !end ? of(source) : empty();
	}

	public ChrObjPuller<V> snoc(Character key, V value) {
		return of(ChrObjFunUtil.snoc(key, value, source));
	}

	public ChrObjPuller<V> sort(Comparator<ChrObjPair<V>> comparator) {
		var list = new ArrayList<ChrObjPair<V>>();
		ChrObjPair<V> pair;
		while (pull(pair = ChrObjPair.of(empty, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> ChrObjPuller<V> sortBy(ChrObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.k, e0.v), fun.apply(e1.k, e1.v)));
	}

	public ChrObjPuller<V> sortByKey(Comparator<Character> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.k, e1.k));
	}

	public ChrObjPuller<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.v, e1.v));
	}

	public ChrObjSource<V> source() {
		return source;
	}

	public Puller<ChrObjPuller<V>> split(ChrObjPredicate<V> fun) {
		return Puller.of(FunUtil.map(ChrObjPuller<V>::new, ChrObjFunUtil.split(fun, source)));
	}

	public ChrObjPuller<V> take(int n) {
		return of(new ChrObjSource<>() {
			private int count = n;

			public boolean source2(ChrObjPair<V> pair) {
				return 0 < count-- ? pull(pair) : false;
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
		while (pull(pair = ChrObjPair.of(empty, null)))
			list.add(pair);
		return list;
	}

	public ChrObjMap<List<V>> toListMap() {
		var map = new ChrObjMap<List<V>>();
		var pair = ChrObjPair.of(empty, (V) null);
		while (pull(pair))
			map.computeIfAbsent(pair.k, k_ -> new ArrayList<>()).add(pair.v);
		return map;
	}

	public ChrObjMap<V> toMap() {
		var map = new ChrObjMap<V>();
		var pair = ChrObjPair.of(empty, (V) null);
		while (source.source2(pair))
			map.put(pair.k, pair.v);
		return map;
	}

	public ListMultimap<Character, V> toMultimap() {
		var map = new ListMultimap<Character, V>();
		groupBy().concatMapValue(Puller::of).sink(map::put);
		return map;
	}

	public ObjChrMap<V> toObjChrMap() {
		var pair = ChrObjPair.of(empty, (V) null);
		var map = new ObjChrMap<V>();
		while (source.source2(pair))
			map.put(pair.v, pair.k);
		return map;
	}

	public Set<ChrObjPair<V>> toSet() {
		var set = new HashSet<ChrObjPair<V>>();
		ChrObjPair<V> pair;
		while (pull(pair = ChrObjPair.of(empty, null)))
			set.add(pair);
		return set;

	}

	public ChrObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Puller<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Puller<O> map_(ChrObj_Obj<V, O> fun0) {
		return Puller.of(ChrObjFunUtil.map(fun0, source));
	}

	private <V1> ChrObjPuller<V1> mapChrObj_(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return of(ChrObjFunUtil.mapChrObj(kf, vf, source));
	}

	private boolean pull(ChrObjPair<V> pair) {
		return source.source2(pair);
	}

}
