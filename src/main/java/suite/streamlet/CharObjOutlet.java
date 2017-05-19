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

import suite.adt.map.CharObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.CharObjPair;
import suite.adt.pair.Pair;
import suite.node.util.Mutable;
import suite.primitive.CharPrimitiveFun.CharObj_Char;
import suite.primitive.CharPrimitiveFun.CharObj_Obj;
import suite.primitive.CharPrimitiveFun.Char_Char;
import suite.primitive.CharPrimitivePredicate.CharObjPredicate;
import suite.primitive.CharPrimitivePredicate.CharPredicate_;
import suite.primitive.CharPrimitiveSource.CharObjSource;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.util.Array_;
import suite.util.CharObjFunUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.List_;
import suite.util.NullableSynchronousQueue;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.To;

public class CharObjOutlet<V> implements Iterable<CharObjPair<V>> {

	private CharObjSource<V> charObjSource;

	@SafeVarargs
	public static <V> CharObjOutlet<V> concat(CharObjOutlet<V>... outlets) {
		List<CharObjSource<V>> sources = new ArrayList<>();
		for (CharObjOutlet<V> outlet : outlets)
			sources.add(outlet.charObjSource);
		return of(CharObjFunUtil.concat(To.source(sources)));
	}

	public static <V> CharObjOutlet<V> empty() {
		return of(CharObjFunUtil.nullSource());
	}

	public static <V> CharObjOutlet<List<V>> of(ListMultimap<Character, V> multimap) {
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

	public static <V> CharObjOutlet<V> of(CharObjMap<V> map) {
		return of(map.source());
	}

	@SafeVarargs
	public static <V> CharObjOutlet<V> of(CharObjPair<V>... kvs) {
		return of(new CharObjSource<V>() {
			private int i;

			public boolean source2(CharObjPair<V> pair) {
				boolean b = i < kvs.length;
				if (b) {
					CharObjPair<V> kv = kvs[i];
					pair.t0 = kv.t0;
					pair.t1 = kv.t1;
				}
				return b;

			}
		});
	}

	public static <V> CharObjOutlet<V> of(Iterable<CharObjPair<V>> col) {
		Iterator<CharObjPair<V>> iter = col.iterator();
		return of(new CharObjSource<V>() {
			public boolean source2(CharObjPair<V> pair) {
				boolean b = iter.hasNext();
				if (b) {
					CharObjPair<V> pair1 = iter.next();
					pair.t0 = pair1.t0;
					pair.t1 = pair1.t1;
				}
				return b;
			}
		});
	}

	public static <V> CharObjOutlet<V> of(CharObjSource<V> source) {
		return new CharObjOutlet<>(source);
	}

	private CharObjOutlet(CharObjSource<V> source) {
		this.charObjSource = source;
	}

	@Override
	public Iterator<CharObjPair<V>> iterator() {
		return CharObjFunUtil.iterator(charObjSource);
	}

	public CharObjOutlet<V> append(Character key, V value) {
		return of(CharObjFunUtil.append(key, value, charObjSource));
	}

	public Outlet<CharObjOutlet<V>> chunk(int n) {
		return Outlet.of(FunUtil.map(CharObjOutlet<V>::new, CharObjFunUtil.chunk(n, charObjSource)));
	}

	public CharObjOutlet<V> closeAtEnd(Closeable c) {
		return of(pair -> {
			boolean b = next(pair);
			if (!b)
				Object_.closeQuietly(c);
			return b;
		});
	}

	public <R> R collect(Fun<CharObjOutlet<V>, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(ObjObj_Obj<Character, V, Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(CharObjFunUtil.map((k, v) -> fun.apply(k, v).source(), charObjSource)));
	}

	public <K1, V1> Outlet2<K1, V1> concatMap2(ObjObj_Obj<Character, V, Outlet2<K1, V1>> fun) {
		return Outlet2.of(FunUtil2.concat(CharObjFunUtil.map((k, v) -> fun.apply(k, v).source(), charObjSource)));
	}

	public <V1> CharObjOutlet<V1> concatMapCharObj(ObjObj_Obj<Character, V, CharObjOutlet<V1>> fun) {
		return of(CharObjFunUtil.concat(CharObjFunUtil.map((k, v) -> fun.apply(k, v).charObjSource, charObjSource)));
	}

	public <V1> CharObjOutlet<V1> concatMapValue(Fun<V, Outlet<V1>> fun) {
		return of(CharObjFunUtil.concat(CharObjFunUtil.map((k, v) -> {
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
		}, charObjSource)));
	}

	public CharObjOutlet<V> cons(char key, V value) {
		return of(CharObjFunUtil.cons(key, value, charObjSource));
	}

	public CharObjOutlet<V> distinct() {
		Set<CharObjPair<V>> set = new HashSet<>();
		return of(pair -> {
			boolean b;
			while ((b = next(pair)) && !set.add(CharObjPair.of(pair.t0, pair.t1)))
				;
			return b;
		});
	}

	public CharObjOutlet<V> drop(int n) {
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next(pair)))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == CharObjOutlet.class) {
			@SuppressWarnings("unchecked")
			CharObjOutlet<V> outlet = (CharObjOutlet<V>) (CharObjOutlet<?>) object;
			CharObjSource<V> source2 = outlet.charObjSource;
			boolean b, b0, b1;
			CharObjPair<V> pair0 = CharObjPair.of((char) 0, null);
			CharObjPair<V> pair1 = CharObjPair.of((char) 0, null);
			while ((b = (b0 = source2.source2(pair0)) == (b1 = source2.source2(pair1))) //
					&& b0 //
					&& b1 //
					&& (b = Objects.equals(pair0, pair1)))
				;
			return b;
		} else
			return false;
	}

	public CharObjOutlet<V> filter(CharObjPredicate<V> fun) {
		return of(CharObjFunUtil.filter(fun, charObjSource));
	}

	public CharObjOutlet<V> filterKey(CharPredicate_ fun) {
		return of(CharObjFunUtil.filterKey(fun, charObjSource));
	}

	public CharObjOutlet<V> filterValue(Predicate<V> fun) {
		return of(CharObjFunUtil.filterValue(fun, charObjSource));
	}

	public CharObjPair<V> first() {
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		return next(pair) ? pair : null;
	}

	public <O> Outlet<O> flatMap(CharObj_Obj<V, Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(CharObjFunUtil.map(fun, charObjSource)));
	}

	public CharObjOutlet<List<V>> groupBy() {
		return of(toListMap().source());
	}

	public <V1> CharObjOutlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return groupBy().mapValue(list -> fun.apply(Read.from(list)));
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		while (next(pair))
			hashCode = hashCode * 31 + pair.hashCode();
		return hashCode;
	}

	public boolean isAll(CharObjPredicate<V> pred) {
		return CharObjFunUtil.isAll(pred, charObjSource);
	}

	public boolean isAny(CharObjPredicate<V> pred) {
		return CharObjFunUtil.isAny(pred, charObjSource);
	}

	public Outlet<Character> keys() {
		return map_((k, v) -> k);
	}

	public CharObjPair<V> last() {
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		if (next(pair))
			while (next(pair))
				;
		else
			pair = null;
		return pair;
	}

	public <O> Outlet<O> map(CharObj_Obj<V, O> fun0) {
		return map_(fun0);
	}

	public <K1, V1> Outlet2<K1, V1> map2(CharObj_Obj<V, K1> kf, CharObj_Obj<V, V1> vf) {
		return Outlet2.of(CharObjFunUtil.map2(kf, vf, charObjSource));
	}

	public <V1> CharObjOutlet<V1> mapCharObj(CharObj_Char<V> kf, CharObj_Obj<V, V1> vf) {
		return mapCharObj_(kf, vf);
	}

	public CharObjOutlet<V> mapKey(Char_Char fun) {
		return mapCharObj_((k, v) -> fun.apply(k), (k, v) -> v);
	}

	public <O> Outlet<O> mapNonNull(CharObj_Obj<V, O> fun) {
		return Outlet.of(CharObjFunUtil.mapNonNull(fun, charObjSource));
	}

	public <V1> CharObjOutlet<V1> mapValue(Fun<V, V1> fun) {
		return mapCharObj_((k, v) -> k, (k, v) -> fun.apply(v));
	}

	public CharObjPair<V> min(Comparator<CharObjPair<V>> comparator) {
		CharObjPair<V> pair = minOrNull(comparator);
		if (pair != null)
			return pair;
		else
			throw new RuntimeException("no result");
	}

	public CharObjPair<V> minOrNull(Comparator<CharObjPair<V>> comparator) {
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		CharObjPair<V> pair1 = CharObjPair.of((char) 0, null);
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

	public CharObjOutlet<V> nonBlocking(Character k0, V v0) {
		NullableSynchronousQueue<CharObjPair<V>> queue = new NullableSynchronousQueue<>();

		new Thread(() -> {
			boolean b;
			do {
				CharObjPair<V> pair = CharObjPair.of((char) 0, null);
				b = charObjSource.source2(pair);
				queue.offerQuietly(pair);
			} while (b);
		}).start();

		return new CharObjOutlet<>(pair -> {
			Mutable<CharObjPair<V>> mutable = Mutable.nil();
			boolean b = queue.poll(mutable);
			if (b) {
				CharObjPair<V> p = mutable.get();
				pair.t0 = p.t0;
				pair.t1 = p.t1;
			} else {
				pair.t0 = k0;
				pair.t1 = v0;
			}
			return b;
		});
	}

	public Outlet<CharObjPair<V>> pairs() {
		return Outlet.of(() -> {
			CharObjPair<V> pair = CharObjPair.of((char) 0, null);
			return next(pair) ? pair : null;
		});
	}

	public Pair<CharObjOutlet<V>, CharObjOutlet<V>> partition(CharObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public CharObjOutlet<V> reverse() {
		return of(List_.reverse(toList()));
	}

	public void sink(BiConsumer<Character, V> sink0) {
		BiConsumer<Character, V> sink1 = Rethrow.biConsumer(sink0);
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		while (next(pair))
			sink1.accept(pair.t0, pair.t1);
	}

	public int size() {
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		int i = 0;
		while (next(pair))
			i++;
		return i;
	}

	public CharObjOutlet<V> skip(int n) {
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next(pair);
		return !end ? of(charObjSource) : empty();
	}

	public CharObjOutlet<V> sort(Comparator<CharObjPair<V>> comparator) {
		List<CharObjPair<V>> list = new ArrayList<>();
		CharObjPair<V> pair;
		while (next(pair = CharObjPair.of((char) 0, null)))
			list.add(pair);
		return of(List_.sort(list, comparator));
	}

	public <O extends Comparable<? super O>> CharObjOutlet<V> sortBy(CharObj_Obj<V, O> fun) {
		return sort((e0, e1) -> Object_.compare(fun.apply(e0.t0, e0.t1), fun.apply(e1.t0, e1.t1)));
	}

	public CharObjOutlet<V> sortByKey(Comparator<Character> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t0, e1.t0));
	}

	public CharObjOutlet<V> sortByValue(Comparator<V> comparator) {
		return sort((e0, e1) -> comparator.compare(e0.t1, e1.t1));
	}

	public CharObjSource<V> source() {
		return charObjSource;
	}

	public Outlet<CharObjOutlet<V>> split(CharObjPredicate<V> fun) {
		return Outlet.of(FunUtil.map(CharObjOutlet<V>::new, CharObjFunUtil.split(fun, charObjSource)));
	}

	public CharObjOutlet<V> take(int n) {
		return of(new CharObjSource<V>() {
			private int count = n;

			public boolean source2(CharObjPair<V> pair) {
				return 0 < count-- ? next(pair) : false;
			}
		});
	}

	public CharObjPair<V>[] toArray() {
		List<CharObjPair<V>> list = toList();
		@SuppressWarnings("unchecked")
		CharObjPair<V>[] array = Array_.newArray(CharObjPair.class, list.size());
		return list.toArray(array);
	}

	public List<CharObjPair<V>> toList() {
		List<CharObjPair<V>> list = new ArrayList<>();
		CharObjPair<V> pair;
		while (next(pair = CharObjPair.of((char) 0, null)))
			list.add(pair);
		return list;
	}

	public CharObjMap<List<V>> toListMap() {
		CharObjMap<List<V>> map = new CharObjMap<>();
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		while (next(pair))
			map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1);
		return map;
	}

	public CharObjMap<V> toMap() {
		CharObjMap<V> map = new CharObjMap<>();
		groupBy().mapValue(values -> Read.from(values).uniqueResult()).sink(map::put);
		return map;
	}

	public ListMultimap<Character, V> toMultimap() {
		ListMultimap<Character, V> map = new ListMultimap<>();
		groupBy().concatMapValue(Outlet::of).sink(map::put);
		return map;
	}

	public Set<CharObjPair<V>> toSet() {
		Set<CharObjPair<V>> set = new HashSet<>();
		CharObjPair<V> pair;
		while (next(pair = CharObjPair.of((char) 0, null)))
			set.add(pair);
		return set;

	}

	public CharObjMap<Set<V>> toSetMap() {
		return groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public CharObjPair<V> uniqueResult() {
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
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

	private <O> Outlet<O> map_(CharObj_Obj<V, O> fun0) {
		return Outlet.of(CharObjFunUtil.map(fun0, charObjSource));
	}

	private <V1> CharObjOutlet<V1> mapCharObj_(CharObj_Char<V> kf, CharObj_Obj<V, V1> vf) {
		return of(CharObjFunUtil.mapCharObj(kf, vf, charObjSource));
	}

	private boolean next(CharObjPair<V> pair) {
		return charObjSource.source2(pair);
	}

}
