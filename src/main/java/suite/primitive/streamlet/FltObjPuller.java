package suite.primitive.streamlet;

import static primal.statics.Fail.fail;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import primal.NullableSyncQueue;
import primal.Verbs.Close;
import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.Verbs.New;
import primal.Verbs.Reverse;
import primal.Verbs.Sort;
import primal.Verbs.Take;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.FunUtil2;
import primal.fp.Funs.Fun;
import primal.fp.Funs2.Sink2;
import primal.primitive.FltObj_Flt;
import primal.primitive.FltPrim;
import primal.primitive.FltPrim.FltObjPredicate;
import primal.primitive.FltPrim.FltObjSource;
import primal.primitive.FltPrim.FltObj_Obj;
import primal.primitive.FltPrim.FltTest;
import primal.primitive.adt.pair.FltObjPair;
import primal.primitive.fp.FltObjFunUtil;
import primal.puller.Puller;
import primal.puller.Puller2;
import primal.puller.PullerDefaults;

public class FltObjPuller<V> implements PullerDefaults<FltObjPair<V>> {

	private static float empty = FltPrim.EMPTYVALUE;

	private FltObjSource<V> source;

	@SafeVarargs
	public static <V> FltObjPuller<V> concat(FltObjPuller<V>... outlets) {
		var sources = new ArrayList<FltObjSource<V>>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(FltObjFunUtil.concat(Take.from(sources)));
	}

	public static <V> FltObjPuller<V> empty() {
		return of(FltObjFunUtil.nullSource());
	}

	@SafeVarargs
	public static <V> FltObjPuller<V> of(FltObjPair<V>... kvs) {
		return of(new FltObjSource<>() {
			private int i;

			public boolean source2(FltObjPair<V> pair) {
				var b = i < kvs.length;
				if (b) {
					FltObjPair<V> kv = kvs[i];
					pair.update(kv.k, kv.v);
				}
				return b;

			}
		});
	}

	public static <V> FltObjPuller<V> of(Iterable<FltObjPair<V>> col) {
		var iter = col.iterator();
		return of(new FltObjSource<>() {
			public boolean source2(FltObjPair<V> pair) {
				var b = iter.hasNext();
				if (b) {
					FltObjPair<V> pair1 = iter.next();
					pair.update(pair1.k, pair1.v);
				}
				return b;
			}
		});
	}

	public static <V> FltObjPuller<V> of(FltObjSource<V> source) {
		return new FltObjPuller<>(source);
	}

	private FltObjPuller(FltObjSource<V> source) {
		this.source = source;
	}

	public Puller<FltObjPuller<V>> chunk(int n) {
		return Puller.of(FunUtil.map(FltObjPuller<V>::new, FltObjFunUtil.chunk(n, source)));
	}

	public FltObjPuller<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = pull(pair);
			if (!b)
				Close.quietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<FltObjPuller<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(FltObj_Obj<V, Puller<O>> fun) {
		return Puller.of(FunUtil.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <K1, V1> Puller2<K1, V1> concatMap2(FltObj_Obj<V, Puller2<K1, V1>> fun) {
		return Puller2.of(FunUtil2.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).source(), source)));
	}

	public <V1> FltObjPuller<V1> concatMapFltObj(FltObj_Obj<V, FltObjPuller<V1>> fun) {
		return of(FltObjFunUtil.concat(FltObjFunUtil.map((k, v) -> fun.apply(k, v).source, source)));
	}

	public <V1> FltObjPuller<V1> concatMapValue(Fun<V, Puller<V1>> fun) {
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

	public FltObjPuller<V> cons(float key, V value) {
		return of(FltObjFunUtil.cons(key, value, source));
	}

	public int count() {
		var pair = FltObjPair.of(empty, (V) null);
		var i = 0;
		while (pull(pair))
			i++;
		return i;
	}

	public FltObjPuller<V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = pull(pair)) && !set.add(FltObjPair.of(pair.k, pair.v)))
				;
			return b;
		});
	}

	public FltObjPuller<V> drop(int n) {
		var pair = FltObjPair.of(empty, (V) null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == FltObjPuller.class) {
			@SuppressWarnings("unchecked")
			var outlet = (FltObjPuller<V>) (FltObjPuller<?>) object;
			var source2 = outlet.source;
			boolean b, b0, b1;
			var pair0 = FltObjPair.of(empty, (V) null);
			var pair1 = FltObjPair.of(empty, (V) null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Equals.ab(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public FltObjPuller<V> filter(FltObjPredicate<V> fun) {
		return of(FltObjFunUtil.filter(fun, source));
	}

	public FltObjPuller<V> filterKey(FltTest fun) {
		return of(FltObjFunUtil.filterKey(fun, source));
	}

	public FltObjPuller<V> filterValue(Predicate<V> fun) {
		return of(FltObjFunUtil.filterValue(fun, source));
	}

	public FltObjPair<V> first() {
		var pair = FltObjPair.of(empty, (V) null);
		return pull(pair) ? pair : null;
	}

	public <O> Puller<O> flatMap(FltObj_Obj<V, Iterable<O>> fun) {
		return Puller.of(FunUtil.flatten(FltObjFunUtil.map(fun, source)));
	}

	@Override
	public int hashCode() {
		var pair = FltObjPair.of(empty, (V) null);
		var h = 7;
		while (pull(pair))
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

	public FltPuller keys() {
		return FltPuller.of(() -> {
			var pair = FltObjPair.of(empty, (V) null);
			return pull(pair) ? pair.k : empty;
		});
	}

	public FltObjPair<V> last() {
		var pair = FltObjPair.of(empty, (V) null);
		if (pull(pair))
			while (pull(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Puller<O> map(FltObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Puller2<K1, V1> map2(FltObj_Obj<V, K1> kf, FltObj_Obj<V, V1> vf) {
		return Puller2.of(FltObjFunUtil.map2(kf, vf, source));
	}

	public <V1> FltObjPuller<V1> mapFltObj(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return mapFltObj_(kf, vf);
	}

	public <V1> FltObjPuller<V1> mapValue(Fun<V, V1> fun) {
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
		if (pull(pair)) {
			while (pull(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.k, pair1.v);
			return pair;
		} else
			return null;
	}

	public FltObjPuller<V> nonBlocking(Float k0, V v0) {
		var queue = new NullableSyncQueue<FltObjPair<V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = FltObjPair.of(empty, (V) null);
				b = source.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new FltObjPuller<>(pair -> {
			var mutable = Mutable.<FltObjPair<V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.value();
				pair.update(p.k, p.v);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public FltObjPair<V> opt() {
		var pair = FltObjPair.of(empty, (V) null);
		if (pull(pair))
			if (!pull(pair))
				return pair;
			else
				return fail("more than one result");
		else
			return FltObjPair.none();
	}

	public Puller<FltObjPair<V>> pairs() {
		return Puller.of(() -> {
			var pair = FltObjPair.of(empty, (V) null);
			return pull(pair) ? pair : null;
		});
	}

	public Pair<FltObjPuller<V>, FltObjPuller<V>> partition(FltObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public FltObjPuller<V> reverse() {
		return of(Reverse.of(toList()));
	}

	public void sink(Sink2<Float, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = FltObjPair.of(empty, (V) null);
		while (pull(pair))
			sink1.sink2(pair.k, pair.v);
	}

	public FltObjPuller<V> skip(int n) {
		var pair = FltObjPair.of(empty, (V) null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull(pair);
		return !end ? of(source) : empty();
	}

	public FltObjPuller<V> snoc(Float key, V value) {
		return of(FltObjFunUtil.snoc(key, value, source));
	}

	public FltObjPuller<V> sort(Comparator<FltObjPair<V>> comparator) {
		var list = new ArrayList<FltObjPair<V>>();
		FltObjPair<V> pair;
		while (pull(pair = FltObjPair.of(empty, null)))
			list.add(pair);
		return of(Sort.list(list, comparator));
	}

	public <O extends Comparable<? super O>> FltObjPuller<V> sortBy(FltObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Compare.objects(fun.apply(e0.k, e0.v), fun.apply(e1.k, e1.v)));
	}

	public FltObjPuller<V> sortByKey(Comparator<Float> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.k, e1.k));
	}

	public FltObjPuller<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.v, e1.v));
	}

	public FltObjSource<V> source() {
		return source;
	}

	public Puller<FltObjPuller<V>> split(FltObjPredicate<V> fun) {
		return Puller.of(FunUtil.map(FltObjPuller<V>::new, FltObjFunUtil.split(fun, source)));
	}

	public FltObjPuller<V> take(int n) {
		return of(new FltObjSource<>() {
			private int count = n;

			public boolean source2(FltObjPair<V> pair) {
				return 0 < count-- ? pull(pair) : false;
			}
		});
	}

	public FltObjPair<V>[] toArray() {
		var list = toList();
		@SuppressWarnings("unchecked")
		FltObjPair<V>[] array = New.array(FltObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<FltObjPair<V>> toList() {
		var list = new ArrayList<FltObjPair<V>>();
		FltObjPair<V> pair;
		while (pull(pair = FltObjPair.of(empty, null)))
			list.add(pair);
		return list;
	}

	public Set<FltObjPair<V>> toSet() {
		var set = new HashSet<FltObjPair<V>>();
		FltObjPair<V> pair;
		while (pull(pair = FltObjPair.of(empty, null)))
			set.add(pair);
		return set;

	}

	public Puller<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Puller<O> map_(FltObj_Obj<V, O> fun0) {
		return Puller.of(FltObjFunUtil.map(fun0, source));
	}

	private <V1> FltObjPuller<V1> mapFltObj_(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return of(FltObjFunUtil.mapFltObj(kf, vf, source));
	}

	private boolean pull(FltObjPair<V> pair) {
		return source.source2(pair);
	}

}
