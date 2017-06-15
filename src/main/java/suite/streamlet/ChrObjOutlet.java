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

import suite.adt.Mutable;
import suite.adt.map.ChrObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.ChrObjPair;
import suite.adt.pair.Pair;
import suite.primitive.ChrObjFunUtil;
import suite.primitive.ChrObj_Chr;
import suite.primitive.ChrPrimitives.ChrObjPredicate;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.ChrObj_Obj;
import suite.primitive.ChrPrimitives.ChrPredicate;
import suite.primitive.Chr_Chr;
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

public class ChrObjOutlet<V> implements Iterable<ChrObjPair<V>> {

	private ChrObjSource<V> source;

	@SafeVarargs
	public static <V> ChrObjOutlet<V> concat(ChrObjOutlet<V>... outlets) {
		List<ChrObjSource<V>> sources = new ArrayList<>();
		for (ChrObjOutlet<V> outlet : outlets)
			sources.add(outlet.source);
		return of(ChrObjFunUtil.concat(To.source(sources)));
	}

	public static <V> ChrObjOutlet<V> empty() {
		return of(ChrObjFunUtil.nullSource());
	}

	public static <V> ChrObjOutlet<List<V>> of(ListMultimap<Character, V> multimap) {
		Iterator<Pair<Character, List<V>>> iter = multimap.listEntries().iterator();
		return of(pair -> {
			boolean b = iter.hasNext();
			if (b) {
				Pair<Character, List<V>> pair1 = iter.next();
				pair.t0 = pair1.t0;
				pair.t1 = pair1.t1;
			}
			return b;
		});
	}

	public static <V> ChrObjOutlet<V> of(ChrObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> ChrObjOutlet<V> of(ChrObjPair<V>... kvs) {
		return of(new ChrObjSource<V>() {
			private int i;

			public boolean source2(ChrObjPair<V> pair) {
				boolean b = i < kvs.length;
				if (b) {
					ChrObjPair<V> kv = kvs[i];
					pair.t0 = kv.t0;
					pair.t1 = kv.t1;
				}
				return b;

			}
		});
	}

	public static <V> ChrObjOutlet<V> of(Iterable<ChrObjPair<V>> col) {
		Iterator<ChrObjPair<V>> iter = col.iterator();
		return of(new ChrObjSource<V>() {
			public boolean source2(ChrObjPair<V> pair) {
				boolean b = iter.hasNext();
				if (b) {
					ChrObjPair<V> pair1 = iter.next();
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
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

	@Override
	public Iterator<ChrObjPair<V>> iterator() {
		return ChrObjFunUtil.iterator(source);
	}

	public ChrObjOutlet<V> append(Character key, V value) {
		return of(ChrObjFunUtil.append(key, value, source));
	}

	public Outlet<ChrObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(ChrObjOutlet<V>::new, ChrObjFunUtil.chunk(n, source)));
	}

	public ChrObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			boolean b = next(pair);
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

	public ChrObjOutlet<V> cons(char key, V value) {
		return of(ChrObjFunUtil.cons(key, value, source));
	}

	public ChrObjOutlet<V> distinct() {
		Set<ChrObjPair<V>> set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(ChrObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public ChrObjOutlet<V> drop(int n) {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrObjOutlet.class) {
			@SuppressWarnings("unchecked")
			ChrObjOutlet<V> outlet = (ChrObjOutlet<V>) (ChrObjOutlet<?>) object;
			ChrObjSource<V> source2 = outlet.source;
			boolean b, b0, b1;
			ChrObjPair<V> pair0 = ChrObjPair.of((char) 0, null);
			ChrObjPair<V> pair1 = ChrObjPair.of((char) 0, null);
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

	public ChrObjOutlet<V> filterKey(ChrPredicate fun) {
		return of(ChrObjFunUtil.filterKey(fun, source));
	}

	public ChrObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(ChrObjFunUtil.filterValue(fun, source));
	}

	public ChrObjPair<V> first() {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
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
		int hashCode = 5;
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		while (next(pair))
			hashCode = hashCode * 31 + pair.hashCode();
		return hashCode;
	}

	public boolean isAll(ChrObjPredicate<V> pred) {
		return ChrObjFunUtil.isAll(pred, source);
	}

	public boolean isAny(ChrObjPredicate<V> pred) {
		return ChrObjFunUtil.isAny(pred, source);
	}

	public Outlet<Character> keys() {
		return map_((k, v) -> k);
	}

	public ChrObjPair<V> last() {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
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

	public ChrObjOutlet<V> mapKey(Chr_Chr fun) {
		return mapChrObj_((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <O> Outlet<O> mapNonNull(ChrObj_Obj<V, O> fun) {
		return Outlet.of(ChrObjFunUtil.mapNonNull(fun, source));
	}

	public <V1> ChrObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapChrObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public ChrObjPair<V> min(Comparator<ChrObjPair<V>> comparator) {
		ChrObjPair<V> pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			throw new RuntimeException("no result");
	}

	public ChrObjPair<V> minOrNull(Comparator<ChrObjPair<V>> comparator) {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		ChrObjPair<V> pair1 = ChrObjPair.of((char) 0, null);
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

	public ChrObjOutlet<V> nonBlocking(Character k0, V v0) {
		NullableSyncQueue<ChrObjPair<V>> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			boolean b;
			do {
				ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new ChrObjOutlet<>(pair -> {
			Mutable<ChrObjPair<V>> mutable = Mutable.nil();
			boolean b = queue.poll(mutable);
			if (b) {
				ChrObjPair<V> p = mutable.get();
				pair.t0 = p.t0;
				pair.t1 = p.t1;
			} else {
				pair.t0 = k0;
				pair.t1 = v0;
			}
			return b;
		});
	}

	public Outlet<ChrObjPair<V>> pairs() {
		return Outlet.of(() -> {
			ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<ChrObjOutlet<V>, ChrObjOutlet<V>> partition(ChrObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public ChrObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(BiConsumer<Character, V> sink0) {
		BiConsumer<Character, V> sink1 = Rethrow.biConsumer(sink0);
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		while (next(pair))
			sink1.accept(pair.t0, pair.t1);
	}

	public int size() {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public ChrObjOutlet<V> skip(int n) {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(source) : empty();
	}

	public ChrObjOutlet<V> sort(Comparator<ChrObjPair<V>> comparator) {
		List<ChrObjPair<V>> list = new ArrayList<>();
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
		return of(new ChrObjSource<V>() {
			private int count = n;

			public boolean source2(ChrObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public ChrObjPair<V>[] toArray() {
		List<ChrObjPair<V>> list = toList();
		@SuppressWarnings("unchecked")
		ChrObjPair<V>[] array = Array_.newArray(ChrObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<ChrObjPair<V>> toList() {
		List<ChrObjPair<V>> list = new ArrayList<>();
		ChrObjPair<V> pair;
		while (next(pair = ChrObjPair.of((char) 0, null)))
			list.add(pair);
		return list;
	}

	public ChrObjMap<List<V>> toListMap() {
		ChrObjMap<List<V>> map = new ChrObjMap<>();
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public ChrObjMap<V> toMap() {
		ChrObjMap<V> map = new ChrObjMap<>();
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink(map::put);
		return map;
	}

	public ListMultimap<Character, V> toMultimap() {
		ListMultimap<Character, V> map = new ListMultimap<>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public Set<ChrObjPair<V>> toSet() {
		Set<ChrObjPair<V>> set = new HashSet<>();
		ChrObjPair<V> pair;
		while (next(pair = ChrObjPair.of((char) 0, null)))
			set.add(pair);
		return set;

	}

	public ChrObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public ChrObjPair<V> uniqueResult() {
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		if (next(pair))
			if (!next(pair))
				return pair;
			else
				throw new RuntimeException("more than one result");
		else
			throw new RuntimeException("no result");
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
