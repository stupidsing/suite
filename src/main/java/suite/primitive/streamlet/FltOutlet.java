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

import suite.adt.Mutable;
import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.Floats;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltFunUtil;
import suite.primitive.FltOpt;
import suite.primitive.FltPrimitives.FltComparator;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.FltPrimitives.FltPredicate;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.adt.pair.FltObjPair;
import suite.primitive.adt.set.FltSet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.OutletDefaults;
import suite.streamlet.Read;
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
public class FltOutlet implements OutletDefaults<Float> {

	private FltSource source;

	@SafeVarargs
	public static FltOutlet concat(FltOutlet... outlets) {
		List<FltSource> sources = new ArrayList<>();
		for (FltOutlet outlet : outlets)
			sources.add(outlet.source);
		return of(FltFunUtil.concat(To.source(sources)));
	}

	public static FltOutlet empty() {
		return of(FltFunUtil.nullSource());
	}

	@SafeVarargs
	public static FltOutlet of(float... ts) {
		return of(new FltSource() {
			private int i;

			public float source() {
				return i < ts.length ? ts[i++] : FltFunUtil.EMPTYVALUE;
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
		return of(() -> source.source());
	}

	public static FltOutlet of(FltSource source) {
		return new FltOutlet(source);
	}

	private FltOutlet(FltSource source) {
		this.source = source;
	}

	@Override
	public Iterator<Float> iterator() {
		return FltFunUtil.iterator(source);
	}

	public FltOutlet append(float c) {
		return of(FltFunUtil.append(c, source));
	}

	public float average() {
		int count = 0;
		float result = 0, c1;
		while ((c1 = next()) != FltFunUtil.EMPTYVALUE) {
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
			float next = next();
			if (next == FltFunUtil.EMPTYVALUE)
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
		int i = 0;
		while (next() != FltFunUtil.EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, FltObj_Obj<U, O> fun) {
		return Outlet.of(new Source<O>() {
			private float c;
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

	public FltOutlet distinct() {
		Set<Float> set = new HashSet<>();
		return of(() -> {
			float c;
			while ((c = next()) != FltFunUtil.EMPTYVALUE && !set.add(c))
				;
			return c;
		});
	}

	public FltOutlet drop(int n) {
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next() != FltFunUtil.EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == FltOutlet.class) {
			FltSource source1 = ((FltOutlet) object).source;
			float o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == FltFunUtil.EMPTYVALUE && o1 == FltFunUtil.EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public FltOutlet filter(FltPredicate fun) {
		return of(FltFunUtil.filter(fun, source));
	}

	public float first() {
		return next();
	}

	public FltOutlet flatMap(Flt_Obj<Iterable<Float>> fun) {
		return of(FunUtil.flatten(FltFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, FltObj_Obj<R, R> fun) {
		float c;
		while ((c = next()) != FltFunUtil.EMPTYVALUE)
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
		int hashCode = 5;
		float c;
		while ((c = source.source()) != FltFunUtil.EMPTYVALUE)
			hashCode = hashCode * 31 + Objects.hashCode(c);
		return hashCode;
	}

	public FltObjOutlet<Integer> index() {
		return FltObjOutlet.of(new FltObjSource<Integer>() {
			private int i = 0;

			public boolean source2(FltObjPair<Integer> pair) {
				float c = next();
				if (c != FltFunUtil.EMPTYVALUE) {
					pair.t0 = c;
					pair.t1 = i++;
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(FltPredicate pred) {
		return FltFunUtil.isAll(pred, source);
	}

	public boolean isAny(FltPredicate pred) {
		return FltFunUtil.isAny(pred, source);
	}

	public float last() {
		float c, c1 = FltFunUtil.EMPTYVALUE;
		while ((c = next()) != FltFunUtil.EMPTYVALUE)
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

	public <O> Outlet<O> mapNonNull(Flt_Obj<O> fun) {
		return Outlet.of(FltFunUtil.mapNonNull(fun, source));
	}

	public float max() {
		return min((c0, c1) -> Float.compare(c1, c0));
	}

	public float min() {
		return min((c0, c1) -> Float.compare(c0, c1));
	}

	public float min(FltComparator comparator) {
		float c = minOrEmpty(comparator);
		if (c != FltFunUtil.EMPTYVALUE)
			return c;
		else
			throw new RuntimeException("no result");
	}

	public float minOrEmpty(FltComparator comparator) {
		float c = next(), c1;
		if (c != FltFunUtil.EMPTYVALUE) {
			while ((c1 = next()) != FltFunUtil.EMPTYVALUE)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return FltFunUtil.EMPTYVALUE;
	}

	public float next() {
		return source.source();
	}

	public FltOutlet nonBlock(float c0) {
		NullableSyncQueue<Float> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			float c;
			do
				queue.offerQuietly(c = source.source());
			while (c != FltFunUtil.EMPTYVALUE);
		}).start();

		return new FltOutlet(() -> {
			Mutable<Float> mutable = Mutable.nil();
			float c = queue.poll(mutable) ? mutable.get() : c0;
			return c;
		});
	}

	public FltOpt opt() {
		float c = next();
		if (c != FltFunUtil.EMPTYVALUE)
			if (next() == FltFunUtil.EMPTYVALUE)
				return FltOpt.of(c);
			else
				throw new RuntimeException("more than one result");
		else
			return FltOpt.none();
	}

	public Pair<FltOutlet, FltOutlet> partition(FltPredicate pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public FltOutlet reverse() {
		return of(toList().toFloats().reverse());
	}

	public void sink(FltSink sink0) {
		FltSink sink1 = sink0.rethrow();
		float c;
		while ((c = next()) != FltFunUtil.EMPTYVALUE)
			sink1.sink(c);
	}

	public FltOutlet skip(int n) {
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next() == FltFunUtil.EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public FltSource source() {
		return source;
	}

	public FltOutlet sort() {
		return of(toList().toFloats().sort());
	}

	public Outlet<FltOutlet> split(FltPredicate fun) {
		return Outlet.of(FunUtil.map(FltOutlet::new, FltFunUtil.split(fun, source)));
	}

	public float sum() {
		float result = 0, c1;
		while ((c1 = next()) != FltFunUtil.EMPTYVALUE)
			result += c1;
		return result;
	}

	public FltOutlet take(int n) {
		return of(new FltSource() {
			private int count = n;

			public float source() {
				return 0 < count-- ? next() : null;
			}
		});
	}

	public float[] toArray() {
		FloatsBuilder list = toList();
		return list.toFloats().toArray();
	}

	public FloatsBuilder toList() {
		FloatsBuilder list = new FloatsBuilder();
		float c;
		while ((c = next()) != FltFunUtil.EMPTYVALUE)
			list.append(c);
		return list;
	}

	public <K> FltObjMap<FloatsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> FltObjMap<FloatsBuilder> toListMap(Flt_Flt valueFun) {
		FltObjMap<FloatsBuilder> map = new FltObjMap<>();
		float c;
		while ((c = next()) != FltFunUtil.EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new FloatsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> Map<K, Float> toMap(Flt_Obj<K> keyFun) {
		return toMap(keyFun, value -> (Float) value);
	}

	public <K, V> Map<K, V> toMap(Flt_Obj<K> kf0, Flt_Obj<V> vf0) {
		Flt_Obj<K> kf1 = kf0.rethrow();
		Flt_Obj<V> vf1 = vf0.rethrow();
		Map<K, V> map = new HashMap<>();
		float c;
		while ((c = next()) != FltFunUtil.EMPTYVALUE) {
			K key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				throw new RuntimeException("duplicate key " + key);
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
		FltSet set = new FltSet();
		float c;
		while ((c = next()) != FltFunUtil.EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).collect(As::map);
	}

	private <K, V> Outlet2<K, V> map2_(Flt_Obj<K> kf0, Flt_Obj<V> vf0) {
		return Outlet2.of(FltFunUtil.map2(kf0, vf0, source));
	}

}
