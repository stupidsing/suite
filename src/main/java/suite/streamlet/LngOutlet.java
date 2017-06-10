package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.map.LngObjMap;
import suite.adt.pair.LngObjPair;
import suite.node.util.Mutable;
import suite.primitive.LngFunUtil;
import suite.primitive.LngObj_Lng;
import suite.primitive.LngPrimitives.LngComparator;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.LngObj_Obj;
import suite.primitive.LngPrimitives.LngPredicate;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.LngPrimitives.Lng_Obj;
import suite.primitive.Lng_Lng;
import suite.primitive.Longs;
import suite.primitive.Longs.LongsBuilder;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.NullableSyncQueue;
import suite.util.Object_;
import suite.util.To;

/***
 * Implement functional structures using
 * 
 * class methods( instead of static* methods in
 * 
 * class FunUtil),just for easier code completion in source editor.**
 * 
 * @author ywsing
 */
public class LngOutlet implements Iterable<Long> {

	private LngSource source;

	@SafeVarargs
	public static LngOutlet concat(LngOutlet... outlets) {
		List<LngSource> sources = new ArrayList<>();
		for (LngOutlet outlet : outlets)
			sources.add(outlet.source);
		return of(LngFunUtil.concat(To.source(sources)));
	}

	public static LngOutlet empty() {
		return of(LngFunUtil.nullSource());
	}

	@SafeVarargs
	public static LngOutlet of(long... ts) {
		return of(new LngSource() {
			private int i;

			public long source() {
				return i < ts.length ? ts[i++] : null;
			}
		});
	}

	public static LngOutlet of(Enumeration<Long> en) {
		return of(To.source(en));
	}

	public static LngOutlet of(Iterable<Long> col) {
		return of(To.source(col));
	}

	public static LngOutlet of(Source<Long> source) {
		return of(() -> source.source());
	}

	public static LngOutlet of(LngSource source) {
		return new LngOutlet(source);
	}

	private LngOutlet(LngSource source) {
		this.source = source;
	}

	@Override
	public Iterator<Long> iterator() {
		return LngFunUtil.iterator(source);
	}

	public LngOutlet append(long t) {
		return of(LngFunUtil.append(t, source));
	}

	public Outlet<LngOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(LngOutlet::new, LngFunUtil.chunk(n, source)));
	}

	public LngOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			long next = next();
			if (next == LngFunUtil.EMPTYVALUE)
				Object_.closeQuietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<LngOutlet, R> fun) {
		return fun.apply(this);
	}

	public LngOutlet concatMap(Lng_Obj<LngOutlet> fun) {
		return of(LngFunUtil.concat(LngFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public <K, V> Outlet2<K, V> concatMap2(Lng_Obj<Outlet2<K, V>> fun) {
		return Outlet2.of(FunUtil2.concat(LngFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public LngOutlet cons(long t) {
		return of(LngFunUtil.cons(t, source));
	}

	public int count() {
		int i = 0;
		while (next() != LngFunUtil.EMPTYVALUE)
			i++;
		return i;
	}

	public <U> LngOutlet cross(List<U> list, LngObj_Lng<U> fun) {
		return of(new LngSource() {
			private long t;
			private int index = list.size();

			public long source() {
				if (index == list.size()) {
					index = 0;
					t = next();
				}
				return fun.apply(t, list.get(index++));
			}
		});
	}

	public LngOutlet distinct() {
		Set<Long> set = new HashSet<>();
		return of(() -> {
			long t;
			while ((t = next()) != LngFunUtil.EMPTYVALUE && !set.add(t))
				;
			return t;
		});
	}

	public LngOutlet drop(int n) {
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next() != LngFunUtil.EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngOutlet.class) {
			LngSource source1 = ((LngOutlet) object).source;
			long o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == LngFunUtil.EMPTYVALUE && o1 == LngFunUtil.EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public LngOutlet filter(LngPredicate fun) {
		return of(LngFunUtil.filter(fun, source));
	}

	public long first() {
		return next();
	}

	public LngOutlet flatMap(Lng_Obj<Iterable<Long>> fun) {
		return of(FunUtil.flatten(LngFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, LngObj_Obj<R, R> fun) {
		long t;
		while ((t = next()) != LngFunUtil.EMPTYVALUE)
			init = fun.apply(t, init);
		return init;
	}

	public <V> LngObjOutlet<LongsBuilder> groupBy() {
		return LngObjOutlet.of(toListMap().source());
	}

	public <V> LngObjOutlet<V> groupBy(Fun<Longs, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toLongs()));
	}

	@Override
	public int hashCode() {
		int hashCode = 5;
		long t;
		while ((t = source.source()) != LngFunUtil.EMPTYVALUE)
			hashCode = hashCode * 31 + Objects.hashCode(t);
		return hashCode;
	}

	public LngObjOutlet<Integer> index() {
		return LngObjOutlet.of(new LngObjSource<Integer>() {
			private int i = 0;

			public boolean source2(LngObjPair<Integer> pair) {
				long t = next();
				if (t != LngFunUtil.EMPTYVALUE) {
					pair.t0 = t;
					pair.t1 = i++;
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(LngPredicate pred) {
		return LngFunUtil.isAll(pred, source);
	}

	public boolean isAny(LngPredicate pred) {
		return LngFunUtil.isAny(pred, source);
	}

	public long last() {
		long t, t1 = LngFunUtil.EMPTYVALUE;
		while ((t = next()) != LngFunUtil.EMPTYVALUE)
			t1 = t;
		return t1;
	}

	public <O> Outlet<O> map(Lng_Obj<O> fun) {
		return Outlet.of(LngFunUtil.map(fun, source));
	}

	public <K, V> Outlet2<K, V> map2(Lng_Obj<K> kf0, Lng_Obj<V> vf0) {
		return map2_(kf0, vf0);
	}

	public LngOutlet mapLng(Lng_Lng fun0) {
		return of(LngFunUtil.mapLng(fun0, source));
	}

	public <O> Outlet<O> mapNonNull(Lng_Obj<O> fun) {
		return Outlet.of(LngFunUtil.mapNonNull(fun, source));
	}

	public long min(LngComparator comparator) {
		long t = minOrNull(comparator);
		if (t != LngFunUtil.EMPTYVALUE)
			return t;
		else
			throw new RuntimeException("no result");
	}

	public long minOrNull(LngComparator comparator) {
		long t = next(), t1;
		if (t != LngFunUtil.EMPTYVALUE) {
			while ((t1 = next()) != LngFunUtil.EMPTYVALUE)
				if (0 < comparator.compare(t, t1))
					t = t1;
			return t;
		} else
			return LngFunUtil.EMPTYVALUE;
	}

	public long next() {
		return source.source();
	}

	public LngOutlet nonBlock(long t0) {
		NullableSyncQueue<Long> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			long t;
			do
				queue.offerQuietly(t = source.source());
			while (t != LngFunUtil.EMPTYVALUE);
		}).start();

		return new LngOutlet(() -> {
			Mutable<Long> mutable = Mutable.nil();
			long c = queue.poll(mutable) ? mutable.get() : t0;
			return c;
		});
	}

	public LngOutlet reverse() {
		return of(toList().toLongs().reverse());
	}

	public void sink(LngSink sink0) {
		LngSink sink1 = sink0.rethrow();
		long t;
		while ((t = next()) != LngFunUtil.EMPTYVALUE)
			sink1.sink(t);
	}

	public LngOutlet skip(int n) {
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next() == LngFunUtil.EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public LngSource source() {
		return source;
	}

	public LngOutlet sort() {
		return of(toList().toLongs().sort());
	}

	public Outlet<LngOutlet> split(LngPredicate fun) {
		return Outlet.of(FunUtil.map(LngOutlet::new, LngFunUtil.split(fun, source)));
	}

	public LngOutlet take(int n) {
		return of(new LngSource() {
			private int count = n;

			public long source() {
				return 0 < count-- ? next() : null;
			}
		});
	}

	public long[] toArray() {
		LongsBuilder list = toList();
		return list.toLongs().toLongArray();
	}

	public LongsBuilder toList() {
		LongsBuilder list = new LongsBuilder();
		long t;
		while ((t = next()) != LngFunUtil.EMPTYVALUE)
			list.append(t);
		return list;
	}

	public <K> LngObjMap<LongsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> LngObjMap<LongsBuilder> toListMap(Lng_Lng valueFun) {
		LngObjMap<LongsBuilder> map = new LngObjMap<>();
		long t;
		while ((t = next()) != LngFunUtil.EMPTYVALUE)
			map.computeIfAbsent(t, k_ -> new LongsBuilder()).append(valueFun.apply(t));
		return map;
	}

	public <K> Map<K, Long> toMap(Lng_Obj<K> keyFun) {
		return toMap(keyFun, value -> (Long) value);
	}

	public <K, V> Map<K, V> toMap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).uniqueResult()).collect(As::map);
	}

	public <K> ListMultimap<K, Long> toMultimap(Lng_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public Set<Long> toSet() {
		Set<Long> set = new HashSet<>();
		long t;
		while ((t = next()) != LngFunUtil.EMPTYVALUE)
			set.add(t);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).collect(As::map);
	}

	public long uniqueResult() {
		long t = next();
		if (t != LngFunUtil.EMPTYVALUE)
			if (next() == LngFunUtil.EMPTYVALUE)
				return t;
			else
				throw new RuntimeException("more than one result");
		else
			throw new RuntimeException("no result");
	}

	private <K, V> Outlet2<K, V> map2_(Lng_Obj<K> kf0, Lng_Obj<V> vf0) {
		return Outlet2.of(LngFunUtil.map2(kf0, vf0, source));
	}

}
