package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;

import suite.adt.Mutable;
import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.LngFunUtil;
import suite.primitive.LngOpt;
import suite.primitive.LngPrimitives.LngComparator;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.LngObj_Obj;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.LngPrimitives.LngTest;
import suite.primitive.LngPrimitives.Lng_Obj;
import suite.primitive.Lng_Lng;
import suite.primitive.Longs;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.adt.map.LngObjMap;
import suite.primitive.adt.map.ObjLngMap;
import suite.primitive.adt.pair.LngObjPair;
import suite.primitive.adt.set.LngSet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.OutletDefaults;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.NullableSyncQueue;
import suite.util.Object_;
import suite.util.To;

public class LngOutlet implements OutletDefaults<Long> {

	private static long EMPTYVALUE = LngFunUtil.EMPTYVALUE;

	private LngSource source;

	@SafeVarargs
	public static LngOutlet concat(LngOutlet... outlets) {
		var sources = new ArrayList<LngSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(LngFunUtil.concat(To.source(sources)));
	}

	public static LngOutlet empty() {
		return of(LngFunUtil.nullSource());
	}

	@SafeVarargs
	public static LngOutlet of(long... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static LngOutlet of(long[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new LngSource() {
			private int i = start;

			public long source() {
				var c = pred.test(i) ? ts[i] : EMPTYVALUE;
				i += inc;
				return c;
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
		return LngOutlet.of(() -> {
			var c = source.source();
			return c != null ? c : EMPTYVALUE;
		});
	}

	public static LngOutlet of(LngSource source) {
		return new LngOutlet(source);
	}

	private LngOutlet(LngSource source) {
		this.source = source;
	}

	public long average() {
		var count = 0;
		long result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE) {
			result += c1;
			count++;
		}
		return (long) (result / count);
	}

	public Outlet<LngOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(LngOutlet::new, LngFunUtil.chunk(n, source)));
	}

	public LngOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			var next = next();
			if (next == EMPTYVALUE)
				Object_.closeQuietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<LngOutlet, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(Lng_Obj<Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(LngFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Outlet2<K, V> concatMap2(Lng_Obj<Outlet2<K, V>> fun) {
		return Outlet2.of(FunUtil2.concat(LngFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public LngOutlet concatMapLng(Lng_Obj<LngOutlet> fun) {
		return of(LngFunUtil.concat(LngFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public LngOutlet cons(long c) {
		return of(LngFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (next() != EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, LngObj_Obj<U, O> fun) {
		return Outlet.of(new Source<>() {
			private long c;
			private int index = list.size();

			public O source() {
				if (index == list.size()) {
					index = 0;
					c = next();
				}
				return fun.apply(c, list.get(index++));
			}
		});
	}

	public LngOutlet distinct() {
		var set = new HashSet<>();
		return of(() -> {
			long c;
			while ((c = next()) != EMPTYVALUE && !set.add(c))
				;
			return c;
		});
	}

	public LngOutlet drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= next() != EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == LngOutlet.class) {
			var source1 = ((LngOutlet) object).source;
			long o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == EMPTYVALUE && o1 == EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public LngOutlet filter(LngTest fun) {
		return of(LngFunUtil.filter(fun, source));
	}

	public long first() {
		return next();
	}

	public <O> Outlet<O> flatMap(Lng_Obj<Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(LngFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, LngObj_Obj<R, R> fun) {
		long c;
		while ((c = next()) != EMPTYVALUE)
			init = fun.apply(c, init);
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
		var h = 7;
		long c;
		while ((c = source.source()) != EMPTYVALUE)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public LngObjOutlet<Integer> index() {
		return LngObjOutlet.of(new LngObjSource<>() {
			private int i = 0;

			public boolean source2(LngObjPair<Integer> pair) {
				var c = next();
				if (c != EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(LngTest pred) {
		return LngFunUtil.isAll(pred, source);
	}

	public boolean isAny(LngTest pred) {
		return LngFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<Long> iterator() {
		return LngFunUtil.iterator(source);
	}

	public long last() {
		long c, c1 = EMPTYVALUE;
		while ((c = next()) != EMPTYVALUE)
			c1 = c;
		return c1;
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

	public <V> LngObjOutlet<V> mapLngObj(Lng_Obj<V> fun0) {
		return LngObjOutlet.of(LngFunUtil.mapLngObj(fun0, source));
	}

	public long max() {
		return min((c0, c1) -> Long.compare(c1, c0));
	}

	public long min() {
		return min((c0, c1) -> Long.compare(c0, c1));
	}

	public long min(LngComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != EMPTYVALUE)
			return c;
		else
			return Fail.t("no result");
	}

	public long minOrEmpty(LngComparator comparator) {
		long c = next(), c1;
		if (c != EMPTYVALUE) {
			while ((c1 = next()) != EMPTYVALUE)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return EMPTYVALUE;
	}

	public long next() {
		return source.source();
	}

	public LngOutlet nonBlock(long c0) {
		var queue = new NullableSyncQueue<Long>();

		new Thread(() -> {
			long c;
			do
				queue.offerQuietly(c = source.source());
			while (c != EMPTYVALUE);
		}).start();

		return new LngOutlet(() -> {
			var mutable = Mutable.<Long> nil();
			var c = queue.poll(mutable) ? mutable.get() : c0;
			return c;
		});
	}

	public LngOpt opt() {
		var c = next();
		if (c != EMPTYVALUE)
			if (next() == EMPTYVALUE)
				return LngOpt.of(c);
			else
				return Fail.t("more than one result");
		else
			return LngOpt.none();
	}

	public Pair<LngOutlet, LngOutlet> partition(LngTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public LngOutlet reverse() {
		return of(toList().toLongs().reverse());
	}

	public void sink(LngSink sink0) {
		var sink1 = sink0.rethrow();
		long c;
		while ((c = next()) != EMPTYVALUE)
			sink1.sink(c);
	}

	public LngOutlet skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next() == EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public LngOutlet snoc(long c) {
		return of(LngFunUtil.snoc(c, source));
	}

	public LngSource source() {
		return source;
	}

	public LngOutlet sort() {
		return of(toList().toLongs().sort());
	}

	public Outlet<LngOutlet> split(LngTest fun) {
		return Outlet.of(FunUtil.map(LngOutlet::new, LngFunUtil.split(fun, source)));
	}

	public long sum() {
		long result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE)
			result += c1;
		return result;
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
		var list = toList();
		return list.toLongs().toArray();
	}

	public LongsBuilder toList() {
		var list = new LongsBuilder();
		long c;
		while ((c = next()) != EMPTYVALUE)
			list.append(c);
		return list;
	}

	public <K> LngObjMap<LongsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> LngObjMap<LongsBuilder> toListMap(Lng_Lng valueFun) {
		var map = new LngObjMap<LongsBuilder>();
		long c;
		while ((c = next()) != EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new LongsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjLngMap<K> toMap(Lng_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjLngMap<K>();
		long c;
		while ((c = next()) != EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Lng_Obj<K> kf0, Lng_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		long c;
		while ((c = next()) != EMPTYVALUE) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				Fail.t("duplicate key " + key);
		}
		return map;
	}

	public <K> ListMultimap<K, Long> toMultimap(Lng_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public LngSet toSet() {
		var set = new LngSet();
		long c;
		while ((c = next()) != EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	private <K, V> Outlet2<K, V> map2_(Lng_Obj<K> kf0, Lng_Obj<V> vf0) {
		return Outlet2.of(LngFunUtil.map2(kf0, vf0, source));
	}

}
