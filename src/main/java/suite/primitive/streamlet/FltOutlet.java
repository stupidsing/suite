package suite.primitive.streamlet;

import static suite.util.Friends.fail;

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
import suite.object.Object_;
import suite.primitive.Floats;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltFunUtil;
import suite.primitive.FltOpt;
import suite.primitive.FltPrimitives.FltComparator;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.FltPrimitives.FltTest;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.adt.map.ObjFltMap;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.adt.set.FltSet;
import suite.streamlet.As;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.OutletDefaults;
import suite.streamlet.Read;
import suite.util.NullableSyncQueue;
import suite.util.To;

public class FltOutlet implements OutletDefaults<Float> {

	private static float EMPTYVALUE = FltFunUtil.EMPTYVALUE;

	private FltSource source;

	@SafeVarargs
	public static FltOutlet concat(FltOutlet... outlets) {
		var sources = new ArrayList<FltSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(FltFunUtil.concat(To.source(sources)));
	}

	public static FltOutlet empty() {
		return of(FltFunUtil.nullSource());
	}

	@SafeVarargs
	public static FltOutlet of(float... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static FltOutlet of(float[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new FltSource() {
			private int i = start;

			public float g() {
				var c = pred.test(i) ? ts[i] : EMPTYVALUE;
				i += inc;
				return c;
			}
		});
	}

	public static FltOutlet of(Enumeration<Float> en) {
		return of(To.source(en));
	}

	public static FltOutlet of(Iterable<Float> col) {
		return of(To.source(col));
	}

	public static FltOutlet of(Source<Float> source) {
		return FltOutlet.of(() -> {
			var c = source.g();
			return c != null ? c : EMPTYVALUE;
		});
	}

	public static FltOutlet of(FltSource source) {
		return new FltOutlet(source);
	}

	private FltOutlet(FltSource source) {
		this.source = source;
	}

	public float average() {
		var count = 0;
		float result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE) {
			result += c1;
			count++;
		}
		return (float) (result / count);
	}

	public Outlet<FltOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(FltOutlet::new, FltFunUtil.chunk(n, source)));
	}

	public FltOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			var next = next();
			if (next == EMPTYVALUE)
				Object_.closeQuietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<FltOutlet, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(Flt_Obj<Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(FltFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Outlet2<K, V> concatMap2(Flt_Obj<Outlet2<K, V>> fun) {
		return Outlet2.of(FunUtil2.concat(FltFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public FltOutlet concatMapFlt(Flt_Obj<FltOutlet> fun) {
		return of(FltFunUtil.concat(FltFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public FltOutlet cons(float c) {
		return of(FltFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (next() != EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, FltObj_Obj<U, O> fun) {
		return Outlet.of(new Source<>() {
			private float c;
			private int index = list.size();

			public O g() {
				if (index == list.size()) {
					index = 0;
					c = next();
				}
				return fun.apply(c, list.get(index++));
			}
		});
	}

	public FltOutlet distinct() {
		var set = new HashSet<>();
		return of(() -> {
			float c;
			while ((c = next()) != EMPTYVALUE && !set.add(c))
				;
			return c;
		});
	}

	public FltOutlet drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= next() != EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltOutlet.class) {
			var source1 = ((FltOutlet) object).source;
			float o0, o1;
			while (Objects.equals(o0 = source.g(), o1 = source1.g()))
				if (o0 == EMPTYVALUE && o1 == EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public FltOutlet filter(FltTest fun) {
		return of(FltFunUtil.filter(fun, source));
	}

	public float first() {
		return next();
	}

	public <O> Outlet<O> flatMap(Flt_Obj<Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(FltFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, FltObj_Obj<R, R> fun) {
		float c;
		while ((c = next()) != EMPTYVALUE)
			init = fun.apply(c, init);
		return init;
	}

	public <V> FltObjOutlet<FloatsBuilder> groupBy() {
		return FltObjOutlet.of(toListMap().source());
	}

	public <V> FltObjOutlet<V> groupBy(Fun<Floats, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toFloats()));
	}

	@Override
	public int hashCode() {
		var h = 7;
		float c;
		while ((c = source.g()) != EMPTYVALUE)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public FltObjOutlet<Integer> index() {
		return FltObjOutlet.of(new FltObjSource<>() {
			private int i = 0;

			public boolean source2(FltObjPair<Integer> pair) {
				var c = next();
				if (c != EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(FltTest pred) {
		return FltFunUtil.isAll(pred, source);
	}

	public boolean isAny(FltTest pred) {
		return FltFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<Float> iterator() {
		return FltFunUtil.iterator(source);
	}

	public float last() {
		float c, c1 = EMPTYVALUE;
		while ((c = next()) != EMPTYVALUE)
			c1 = c;
		return c1;
	}

	public <O> Outlet<O> map(Flt_Obj<O> fun) {
		return Outlet.of(FltFunUtil.map(fun, source));
	}

	public <K, V> Outlet2<K, V> map2(Flt_Obj<K> kf0, Flt_Obj<V> vf0) {
		return map2_(kf0, vf0);
	}

	public FltOutlet mapFlt(Flt_Flt fun0) {
		return of(FltFunUtil.mapFlt(fun0, source));
	}

	public <V> FltObjOutlet<V> mapFltObj(Flt_Obj<V> fun0) {
		return FltObjOutlet.of(FltFunUtil.mapFltObj(fun0, source));
	}

	public float max() {
		return min((c0, c1) -> Float.compare(c1, c0));
	}

	public float min() {
		return min((c0, c1) -> Float.compare(c0, c1));
	}

	public float min(FltComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != EMPTYVALUE)
			return c;
		else
			return fail("no result");
	}

	public float minOrEmpty(FltComparator comparator) {
		float c = next(), c1;
		if (c != EMPTYVALUE) {
			while ((c1 = next()) != EMPTYVALUE)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return EMPTYVALUE;
	}

	public float next() {
		return source.g();
	}

	public FltOutlet nonBlock(float c0) {
		var queue = new NullableSyncQueue<Float>();

		new Thread(() -> {
			float c;
			do
				queue.offerQuietly(c = source.g());
			while (c != EMPTYVALUE);
		}).start();

		return new FltOutlet(() -> {
			var mutable = Mutable.<Float> nil();
			var c = queue.poll(mutable) ? mutable.value() : c0;
			return c;
		});
	}

	public FltOpt opt() {
		var c = next();
		if (c != EMPTYVALUE)
			if (next() == EMPTYVALUE)
				return FltOpt.of(c);
			else
				return fail("more than one result");
		else
			return FltOpt.none();
	}

	public Pair<FltOutlet, FltOutlet> partition(FltTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public FltOutlet reverse() {
		return of(toList().toFloats().reverse());
	}

	public void sink(FltSink sink0) {
		var sink1 = sink0.rethrow();
		float c;
		while ((c = next()) != EMPTYVALUE)
			sink1.f(c);
	}

	public FltOutlet skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next() == EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public FltOutlet snoc(float c) {
		return of(FltFunUtil.snoc(c, source));
	}

	public FltSource source() {
		return source;
	}

	public FltOutlet sort() {
		return of(toList().toFloats().sort());
	}

	public Outlet<FltOutlet> split(FltTest fun) {
		return Outlet.of(FunUtil.map(FltOutlet::new, FltFunUtil.split(fun, source)));
	}

	public float sum() {
		float result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE)
			result += c1;
		return result;
	}

	public FltOutlet take(int n) {
		return of(new FltSource() {
			private int count = n;

			public float g() {
				return 0 < count-- ? next() : null;
			}
		});
	}

	public float[] toArray() {
		var list = toList();
		return list.toFloats().toArray();
	}

	public FloatsBuilder toList() {
		var list = new FloatsBuilder();
		float c;
		while ((c = next()) != EMPTYVALUE)
			list.append(c);
		return list;
	}

	public <K> FltObjMap<FloatsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> FltObjMap<FloatsBuilder> toListMap(Flt_Flt valueFun) {
		var map = new FltObjMap<FloatsBuilder>();
		float c;
		while ((c = next()) != EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new FloatsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjFltMap<K> toMap(Flt_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjFltMap<K>();
		float c;
		while ((c = next()) != EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Flt_Obj<K> kf0, Flt_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		float c;
		while ((c = next()) != EMPTYVALUE) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				fail("duplicate key " + key);
		}
		return map;
	}

	public <K> ListMultimap<K, Float> toMultimap(Flt_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public FltSet toSet() {
		var set = new FltSet();
		float c;
		while ((c = next()) != EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public <U, R> Outlet<R> zip(Outlet<U> outlet1, FltObj_Obj<U, R> fun) {
		return Outlet.of(() -> {
			var t = next();
			var u = outlet1.next();
			return t != EMPTYVALUE && u != null ? fun.apply(t, u) : null;
		});
	}

	private <K, V> Outlet2<K, V> map2_(Flt_Obj<K> kf0, Flt_Obj<V> vf0) {
		return Outlet2.of(FltFunUtil.map2(kf0, vf0, source));
	}

}
