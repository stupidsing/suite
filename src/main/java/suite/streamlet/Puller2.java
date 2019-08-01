package suite.streamlet;

import static primal.statics.Fail.fail;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import primal.NullableSyncQueue;
import primal.Verbs.Close;
import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Fixie_.FixieFun3;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.FunUtil2;
import primal.fp.Funs.Fun;
import primal.fp.Funs2.Fun2;
import primal.fp.Funs2.Sink2;
import primal.fp.Funs2.Source2;
import suite.adt.map.ListMultimap;
import suite.util.Array_;
import suite.util.List_;
import suite.util.To;

public class Puller2<K, V> implements PullerDefaults<Pair<K, V>> {

	private Source2<K, V> source2;

	@SafeVarargs
	public static <K, V> Puller2<K, V> concat(Puller2<K, V>... outlets) {
		var sources = new ArrayList<Source2<K, V>>();
		for (var outlet : outlets)
			sources.add(outlet.source2);
		return of(FunUtil2.concat(To.source(sources)));
	}

	public static <K, V> Puller2<K, V> empty() {
		return of(FunUtil2.nullSource());
	}

	public static <K, V> Puller2<K, List<V>> of(ListMultimap<K, V> multimap) {
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

	public static <K, V> Puller2<K, V> of(Map<K, V> map) {
		var iter = map.entrySet().iterator();
		return of(pair -> {
			var b = iter.hasNext();
			if (b) {
				Entry<K, V> pair1 = iter.next();
				pair.update(pair1.getKey(), pair1.getValue());
			}
			return b;
		});
	}

	@SafeVarargs
	public static <K, V> Puller2<K, V> of(Pair<K, V>... kvs) {
		return of(new Source2<>() {
			private int i;

			public boolean source2(Pair<K, V> pair) {
				var b = i < kvs.length;
				if (b) {
					var kv = kvs[i];
					pair.update(kv.k, kv.v);
				}
				return b;
			}
		});
	}

	public static <K, V> Puller2<K, V> of(Iterable<Pair<K, V>> col) {
		var iter = col.iterator();
		return of(new Source2<>() {
			public boolean source2(Pair<K, V> pair) {
				var b = iter.hasNext();
				if (b) {
					var pair1 = iter.next();
					pair.update(pair1.k, pair1.v);
				}
				return b;
			}
		});
	}

	public static <K, V> Puller2<K, V> of(Source2<K, V> source) {
		return new Puller2<>(source);
	}

	private Puller2(Source2<K, V> source) {
		this.source2 = source;
	}

	public Puller<Puller2<K, V>> chunk(int n) {
		return Puller.of(FunUtil.map(Puller2<K, V>::new, FunUtil2.chunk(n, source2)));
	}

	public Puller2<K, V> closeAtEnd(Closeable c) {
		return of(pair -> {
			var b = pull(pair);
			if (!b)
				Close.quietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<Puller2<K, V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(Fun2<K, V, Puller<O>> fun) {
		return Puller.of(FunUtil.concat(FunUtil2.map((k, v) -> fun.apply(k, v).source(), source2)));
	}

	public <K1, V1> Puller2<K1, V1> concatMap2(Fun2<K, V, Puller2<K1, V1>> fun) {
		return of(FunUtil2.concat(FunUtil2.map((k, v) -> fun.apply(k, v).source2, source2)));
	}

	public <V1> Puller2<K, V1> concatMapValue(Fun<V, Puller<V1>> fun) {
		return of(FunUtil2.concat(FunUtil2.map((k, v) -> {
			var source = fun.apply(v).source();
			return pair -> {
				var value1 = source.g();
				var b = value1 != null;
				if (b)
					pair.update(k, value1);
				return b;
			};
		}, source2)));
	}

	public Puller2<K, V> cons(K key, V value) {
		return of(FunUtil2.cons(key, value, source2));
	}

	public int count() {
		var pair = Pair.<K, V> of(null, null);
		var i = 0;
		while (pull(pair))
			i++;
		return i;
	}

	public Puller2<K, V> distinct() {
		var set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = pull(pair)) && !set.add(Pair.of(pair.k, pair.v)))
				;
			return b;
		});
	}

	public Puller2<K, V> drop(int n) {
		var pair = Pair.<K, V> of(null, null);
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == Puller2.class) {
			@SuppressWarnings("unchecked")
			var outlet = (Puller2<K, V>) (Puller2<?, ?>) object;
			var source2 = outlet.source2;
			boolean b, b0, b1;
			var pair0 = Pair.<K, V> of(null, null);
			var pair1 = Pair.<K, V> of(null, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Equals.ab(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public Puller2<K, V> filter(BiPredicate<K, V> fun) {
		return of(FunUtil2.filter(fun, source2));
	}

	public Puller2<K, V> filterKey(Predicate<K> fun) {
		return of(FunUtil2.filterKey(fun, source2));
	}

	public Puller2<K, V> filterValue(Predicate<V> fun) {
		return of(FunUtil2.filterValue(fun, source2));
	}

	public Pair<K, V> first() {
		var pair = Pair.<K, V> of(null, null);
		return pull(pair) ? pair : null;
	}

	public <O> Puller<O> flatMap(Fun2<K, V, Iterable<O>> fun) {
		return Puller.of(FunUtil.flatten(FunUtil2.map(fun, source2)));
	}

	public <R> R fold(R init, FixieFun3<R, K, V, R> fun) {
		var pair = Pair.<K, V> of(null, null);
		while (pull(pair))
			init = fun.apply(init, pair.k, pair.v);
		return init;
	}

	public Puller2<K, List<V>> groupBy() {
		var map = toListMap();
		return Puller.of(map.entrySet()).map2(Entry::getKey, Entry::getValue);
	}

	public <V1> Puller2<K, V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		var pair = Pair.<K, V> of(null, null);
		var h = 7;
		while (pull(pair))
			h = h * 31 + pair.hashCode();
		return h;
	}

	public boolean isAll(BiPredicate<K, V> pred) {
		return FunUtil2.isAll(pred, source2);
	}

	public boolean isAny(BiPredicate<K, V> pred) {
		return FunUtil2.isAny(pred, source2);
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return FunUtil2.iterator(source2);
	}

	public Puller<K> keys() {
		return map_((k, v) -> k);
	}

	public Pair<K, V> last() {
		var pair = Pair.<K, V> of(null, null);
		if (pull(pair))
			while (pull(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Puller<O> map(Fun2<K, V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Puller2<K1, V1> map2(Fun2<K, V, K1> kf, Fun2<K, V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> Puller2<K, V1> mapValue(Fun<V, V1> fun) {
		return map2_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public Pair<K, V> min(Comparator<Pair<K, V>> comparator) {
		var pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			return fail("no result");
	}

	public Pair<K, V> minOrNull(Comparator<Pair<K, V>> comparator) {
		var pair = Pair.<K, V> of(null, null);
		var pair1 = Pair.<K, V> of(null, null);
		if (pull(pair)) {
			while (pull(pair1))
				if (0 < comparator.compare(pair, pair1))
					pair.update(pair1.k, pair1.v);
			return pair;
		} else
			return null;
	}

	public Puller2<K, V> nonBlocking(K k0, V v0) {
		var queue = new NullableSyncQueue<Pair<K, V>>();

		new Thread(() -> {
			boolean b;
			do {
				var pair = Pair.<K, V> of(null, null);
				b = source2.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new Puller2<>(pair -> {
			var mutable = Mutable.<Pair<K, V>> nil();
			var b = queue.poll(mutable);
			if (b) {
				var p = mutable.value();
				pair.update(p.k, p.v);
			} else
				pair.update(k0, v0);
			return b;
		});
	}

	public Pair<K, V> opt() {
		var pair = Pair.<K, V> of(null, null);
		if (pull(pair))
			if (!pull(pair))
				return pair;
			else
				return fail("more than one result");
		else
			return Pair.none();
	}

	public Puller<Pair<K, V>> pairs() {
		return Puller.of(() -> {
			var pair = Pair.<K, V> of(null, null);
			return pull(pair) ? pair : null;
		});
	}

	public Pair<Puller2<K, V>, Puller2<K, V>> partition(BiPredicate<K, V> pred) {
		return Pair.of(filter(pred), filter(pred.negate()));
	}

	public Puller2<K, V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(Sink2<K, V> sink0) {
		var sink1 = sink0.rethrow();
		var pair = Pair.<K, V> of(null, null);
		while (pull(pair))
			sink1.sink2(pair.k, pair.v);
	}

	public Puller2<K, V> skip(int n) {
		var pair = Pair.<K, V> of(null, null);
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull(pair);
		return !end ? of(source2) : empty();
	}

	public Puller2<K, V> snoc(K key, V value) {
		return of(FunUtil2.snoc(key, value, source2));
	}

	public Puller2<K, V> sort(Comparator<Pair<K, V>> comparator) {
		var list = new ArrayList<Pair<K, V>>();
		Pair<K, V> pair;
		while (pull(pair = Pair.of(null, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> Puller2<K, V> sortBy(Fun2<K, V, O> fun) {
		return sort((e0, e1) -> Compare.objects(fun.apply(e0.k, e0.v), fun.apply(e1.k, e1.v)));
	}

	public Puller2<K, V> sortByKey(Comparator<K> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.k, e1.k));
	}

	public Puller2<K, V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.v, e1.v));
	}

	public Source2<K, V> source() {
		return source2;
	}

	public Puller<Puller2<K, V>> split(BiPredicate<K, V> fun) {
		return Puller.of(FunUtil.map(Puller2<K, V>::new, FunUtil2.split(fun, source2)));
	}

	public Puller2<K, V> take(int n) {
		return of(new Source2<>() {
			private int count = n;

			public boolean source2(Pair<K, V> pair) {
				return 0 < count-- ? pull(pair) : false;
			}
		});
	}

	public Pair<K, V>[] toArray() {
		var list = toList();
		@SuppressWarnings("unchecked")
		Pair<K, V>[] array = Array_.newArray(Pair.class, list.size());
		return list.toArray(array);
	}

	public List<Pair<K, V>> toList() {
		var list = new ArrayList<Pair<K, V>>();
		Pair<K, V> pair;
		while (pull(pair = Pair.of(null, null)))
			list.add(pair);
		return list;
	}

	public Map<K, List<V>> toListMap() {
		var map = new HashMap<K, List<V>>();
		var pair = Pair.<K, V> of(null, null);
		while (pull(pair))
			map.computeIfAbsent(pair.k, k_ -> new ArrayList<>()).add(pair.v);
		return map;
	}

	public Map<K, V> toMap() {
		var map = new HashMap<K, V>();
		var pair = Pair.<K, V> of(null, null);
		while (pull(pair))
			if (map.put(pair.k, pair.v) != null)
				fail("duplicate key " + pair.k);
		return map;
	}

	public ListMultimap<K, V> toMultimap() {
		var map = new ListMultimap<K, V>();
		groupBy().concatMapValue(Puller::of).sink(map::put);
		return map;
	}

	public Set<Pair<K, V>> toSet() {
		var set = new HashSet<Pair<K, V>>();
		Pair<K, V> pair;
		while (pull(pair = Pair.of(null, null)))
			set.add(pair);
		return set;

	}

	public Map<K, Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public Puller<V> values() {
		return map_((k, v) -> v);
	}

	private <O> Puller<O> map_(Fun2<K, V, O> fun0) {
		return Puller.of(FunUtil2.map(fun0, source2));
	}

	private <K1, V1> Puller2<K1, V1> map2_(Fun2<K, V, K1> kf, Fun2<K, V, V1> vf) {
		return of(FunUtil2.map2(kf, vf, source2));
	}

	private boolean pull(Pair<K, V> pair) {
		return source2.source2(pair);
	}

}
