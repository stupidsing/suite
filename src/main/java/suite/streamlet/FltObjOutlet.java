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

import suite.adt.map.FltObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.FltObjPair;
import suite.adt.pair.Pair;
import suite.node.util.Mutable;
import suite.primitive.FltObjFunUtil;
import suite.primitive.FltObj_Flt;
import suite.primitive.FltPrimitiveFun.FltObj_Obj;
import suite.primitive.FltPrimitivePredicate.FltObjPredicate;
import suite.primitive.FltPrimitivePredicate.FltPredicate_;
import suite.primitive.FltPrimitiveSource.FltObjSource;
import suite.primitive.Flt_Flt;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
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

public class FltObjOutlet<V> implements Iterable<FltObjPair<V>> {

	private FltObjSource<V> floatObjSource;

	@SafeVarargs
	public static <V> FltObjOutlet<V> concat(FltObjOutlet<V>... outlets) {
		List<FltObjSource<V>> sources = new ArrayList<>();
		for (FltObjOutlet<V> outlet : outlets)
			sources.add(outlet.floatObjSource);
		return of(FltObjFunUtil.concat(To.source(sources)));
	}

	public static <V> FltObjOutlet<V> empty() {
		return of(FltObjFunUtil.nullSource());
	}

	public static <V> FltObjOutlet<List<V>> of(ListMultimap<Float, V> multimap) {
		Iterator<Pair<Float, List<V>>> iter = multimap.listEntries().iterator();
		return of(pair -> {
			boolean b = iter.hasNext();
			if (b) {
				Pair<Float, List<V>> pair1 = iter.next();
				pair.t0 = pair1.t0;
				pair.t1 = pair1.t1;
			}
			return b;
		});
	}

	public static <V> FltObjOutlet<V> of(FltObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> FltObjOutlet<V> of(FltObjPair<V>... kvs) {
		return of(new FltObjSource<V>() {
			private int i;

			public boolean source2(FltObjPair<V> pair) {
				boolean b = i < kvs.length;
				if (b) {
					FltObjPair<V> kv = kvs[i];
					pair.t0 = kv.t0;
					pair.t1 = kv.t1;
				}
				return b;

			}
		});
	}

	public static <V> FltObjOutlet<V> of(Iterable<FltObjPair<V>> col) {
		Iterator<FltObjPair<V>> iter = col.iterator();
		return of(new FltObjSource<V>() {
			public boolean source2(FltObjPair<V> pair) {
				boolean b = iter.hasNext();
				if (b) {
					FltObjPair<V> pair1 = iter.next();
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
				}
				return b;
			}
		});
	}

	public static <V> FltObjOutlet<V> of(FltObjSource<V> source) {
		return new FltObjOutlet<>(source);
	}

	private FltObjOutlet(FltObjSource<V> source) {
		this.floatObjSource = source;
	}

	@Override
	public Iterator<FltObjPair<V>> iterator() {
		return FltObjFunUtil.iterator(floatObjSource);
	}

	public FltObjOutlet<V> append(Float key, V value) {
		return of(FltObjFunUtil.append(key, value, floatObjSource));
	}

	public Outlet<FltObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(FltObjOutlet<V>::new, FltObjFunUtil.chunk(n, floatObjSource)));
	}

	public FltObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			boolean b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<FltObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(ObjObj_Obj<Float, V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).source(), floatObjSource)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(ObjObj_Obj<Float, V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).source(), floatObjSource)));
	}

	public <V1> FltObjOutlet<V1> concatMapFloatObj(ObjObj_Obj<Float, V, FltObjOutlet<V1>> fun) {
		return of(FltObjFunUtil.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).floatObjSource, floatObjSource)));
	}

	public <V1> FltObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(FltObjFunUtil.concat(FltObjFunUtil.map((k, v) -> {
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
		}, floatObjSource)));
	}

	public FltObjOutlet<V> cons(float key, V value) {
		return of(FltObjFunUtil.cons(key, value, floatObjSource));
	}

	public FltObjOutlet<V> distinct() {
		Set<FltObjPair<V>> set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(FltObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public FltObjOutlet<V> drop(int n) {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltObjOutlet.class) {
			@SuppressWarnings("unchecked")
			FltObjOutlet<V> outlet = (FltObjOutlet<V>) (FltObjOutlet<?>) object;
			FltObjSource<V> source2 = outlet.floatObjSource;
			boolean b, b0, b1;
			FltObjPair<V> pair0 = FltObjPair.of((float) 0, null);
			FltObjPair<V> pair1 = FltObjPair.of((float) 0, null);
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
		return of(FltObjFunUtil.filter(fun, floatObjSource));
	}

	public FltObjOutlet<V> filterKey(FltPredicate_ fun) {
		return of(FltObjFunUtil.filterKey(fun, floatObjSource));
	}

	public FltObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(FltObjFunUtil.filterValue(fun, floatObjSource));
	}

	public FltObjPair<V> first() {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(FltObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(FltObjFunUtil.map(fun, floatObjSource)));
	}

	public FltObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> FltObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		while (next(pair))
			hashCode = hashCode * 31 + pair.hashCode();
		return hashCode;
	}

	public boolean isAll(FltObjPredicate<V> pred) {
		return FltObjFunUtil.isAll(pred, floatObjSource);
	}

	public boolean isAny(FltObjPredicate<V> pred) {
		return FltObjFunUtil.isAny(pred, floatObjSource);
	}

	public Outlet<Float> keys() {
		return map_((k, v) -> k);
	}

	public FltObjPair<V> last() {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
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
		return Outlet2.of(FltObjFunUtil.map2(kf, vf, floatObjSource));
	}

	public <V1> FltObjOutlet<V1> mapFltObj(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return mapFltObj_(kf, vf);
	}

	public FltObjOutlet<V> mapKey(Flt_Flt fun) {
		return mapFltObj_((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <O> Outlet<O> mapNonNull(FltObj_Obj<V, O> fun) {
		return Outlet.of(FltObjFunUtil.mapNonNull(fun, floatObjSource));
	}

	public <V1> FltObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapFltObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public FltObjPair<V> min(Comparator<FltObjPair<V>> comparator) {
		FltObjPair<V> pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			throw new RuntimeException("no result");
	}

	public FltObjPair<V> minOrNull(Comparator<FltObjPair<V>> comparator) {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		FltObjPair<V> pair1 = FltObjPair.of((float) 0, null);
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

	public FltObjOutlet<V> nonBlocking(Float k0, V v0) {
		NullableSyncQueue<FltObjPair<V>> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			boolean b;
			do {
				FltObjPair<V> pair = FltObjPair.of((float) 0, null);
				b = floatObjSource.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new FltObjOutlet<>(pair -> {
			Mutable<FltObjPair<V>> mutable = Mutable.nil();
			boolean b = queue.poll(mutable);
			if (b) {
				FltObjPair<V> p = mutable.get();
				pair.t0 = p.t0;
				pair.t1 = p.t1;
			} else {
				pair.t0 = k0;
				pair.t1 = v0;
			}
			return b;
		});
	}

	public Outlet<FltObjPair<V>> pairs() {
		return Outlet.of(() -> {
			FltObjPair<V> pair = FltObjPair.of((float) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<FltObjOutlet<V>, FltObjOutlet<V>> partition(FltObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public FltObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(BiConsumer<Float, V> sink0) {
		BiConsumer<Float, V> sink1 = Rethrow.biConsumer(sink0);
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		while (next(pair))
			sink1.accept(pair.t0, pair.t1);
	}

	public int size() {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public FltObjOutlet<V> skip(int n) {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(floatObjSource) : empty();
	}

	public FltObjOutlet<V> sort(Comparator<FltObjPair<V>> comparator) {
		List<FltObjPair<V>> list = new ArrayList<>();
		FltObjPair<V> pair;
		while (next(pair = FltObjPair.of((float) 0, null)))
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
		return floatObjSource;
	}

	public Outlet<FltObjOutlet<V>> split(FltObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(FltObjOutlet<V>::new, FltObjFunUtil.split(fun, floatObjSource)));
	}

	public FltObjOutlet<V> take(int n) {
		return of(new FltObjSource<V>() {
			private int count = n;

			public boolean source2(FltObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public FltObjPair<V>[] toArray() {
		List<FltObjPair<V>> list = toList();
		@SuppressWarnings("unchecked")
		FltObjPair<V>[] array = Array_.newArray(FltObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<FltObjPair<V>> toList() {
		List<FltObjPair<V>> list = new ArrayList<>();
		FltObjPair<V> pair;
		while (next(pair = FltObjPair.of((float) 0, null)))
			list.add(pair);
		return list;
	}

	public FltObjMap<List<V>> toListMap() {
		FltObjMap<List<V>> map = new FltObjMap<>();
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public FltObjMap<V> toMap() {
		FltObjMap<V> map = new FltObjMap<>();
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink(map::put);
		return map;
	}

	public ListMultimap<Float, V> toMultimap() {
		ListMultimap<Float, V> map = new ListMultimap<>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public Set<FltObjPair<V>> toSet() {
		Set<FltObjPair<V>> set = new HashSet<>();
		FltObjPair<V> pair;
		while (next(pair = FltObjPair.of((float) 0, null)))
			set.add(pair);
		return set;

	}

	public FltObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public FltObjPair<V> uniqueResult() {
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
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

	private <O> Outlet<O> map_(FltObj_Obj<V, O> fun0) {
		return Outlet.of(FltObjFunUtil.map(fun0, floatObjSource));
	}

	private <V1> FltObjOutlet<V1> mapFltObj_(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return of(FltObjFunUtil.mapFloatObj(kf, vf, floatObjSource));
	}

	private boolean next(FltObjPair<V> pair) {
		return floatObjSource.source2(pair);
	}

}
