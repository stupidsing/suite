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
import suite.primitive.DblFunUtil;
import suite.primitive.DblOpt;
import suite.primitive.DblPrimitives.DblComparator;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.DblPrimitives.DblTest;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.Dbl_Dbl;
import suite.primitive.Doubles;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.adt.map.DblObjMap;
import suite.primitive.adt.map.ObjDblMap;
import suite.primitive.adt.pair.DblObjPair;
import suite.primitive.adt.set.DblSet;
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

public class DblOutlet implements OutletDefaults<Double> {

	private static double EMPTYVALUE = DblFunUtil.EMPTYVALUE;

	private DblSource source;

	@SafeVarargs
	public static DblOutlet concat(DblOutlet... outlets) {
		var sources = new ArrayList<DblSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(DblFunUtil.concat(To.source(sources)));
	}

	public static DblOutlet empty() {
		return of(DblFunUtil.nullSource());
	}

	@SafeVarargs
	public static DblOutlet of(double... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static DblOutlet of(double[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new DblSource() {
			private int i = start;

			public double g() {
				var c = pred.test(i) ? ts[i] : EMPTYVALUE;
				i += inc;
				return c;
			}
		});
	}

	public static DblOutlet of(Enumeration<Double> en) {
		return of(To.source(en));
	}

	public static DblOutlet of(Iterable<Double> col) {
		return of(To.source(col));
	}

	public static DblOutlet of(Source<Double> source) {
		return DblOutlet.of(() -> {
			var c = source.g();
			return c != null ? c : EMPTYVALUE;
		});
	}

	public static DblOutlet of(DblSource source) {
		return new DblOutlet(source);
	}

	private DblOutlet(DblSource source) {
		this.source = source;
	}

	public double average() {
		var count = 0;
		double result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE) {
			result += c1;
			count++;
		}
		return (double) (result / count);
	}

	public Outlet<DblOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(DblOutlet::new, DblFunUtil.chunk(n, source)));
	}

	public DblOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			var next = next();
			if (next == EMPTYVALUE)
				Object_.closeQuietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<DblOutlet, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(Dbl_Obj<Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(DblFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Outlet2<K, V> concatMap2(Dbl_Obj<Outlet2<K, V>> fun) {
		return Outlet2.of(FunUtil2.concat(DblFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public DblOutlet concatMapDbl(Dbl_Obj<DblOutlet> fun) {
		return of(DblFunUtil.concat(DblFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public DblOutlet cons(double c) {
		return of(DblFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (next() != EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, DblObj_Obj<U, O> fun) {
		return Outlet.of(new Source<>() {
			private double c;
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

	public DblOutlet distinct() {
		var set = new HashSet<>();
		return of(() -> {
			double c;
			while ((c = next()) != EMPTYVALUE && !set.add(c))
				;
			return c;
		});
	}

	public DblOutlet drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= next() != EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblOutlet.class) {
			var source1 = ((DblOutlet) object).source;
			double o0, o1;
			while (Objects.equals(o0 = source.g(), o1 = source1.g()))
				if (o0 == EMPTYVALUE && o1 == EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public DblOutlet filter(DblTest fun) {
		return of(DblFunUtil.filter(fun, source));
	}

	public double first() {
		return next();
	}

	public <O> Outlet<O> flatMap(Dbl_Obj<Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(DblFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, DblObj_Obj<R, R> fun) {
		double c;
		while ((c = next()) != EMPTYVALUE)
			init = fun.apply(c, init);
		return init;
	}

	public <V> DblObjOutlet<DoublesBuilder> groupBy() {
		return DblObjOutlet.of(toListMap().source());
	}

	public <V> DblObjOutlet<V> groupBy(Fun<Doubles, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toDoubles()));
	}

	@Override
	public int hashCode() {
		var h = 7;
		double c;
		while ((c = source.g()) != EMPTYVALUE)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public DblObjOutlet<Integer> index() {
		return DblObjOutlet.of(new DblObjSource<>() {
			private int i = 0;

			public boolean source2(DblObjPair<Integer> pair) {
				var c = next();
				if (c != EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(DblTest pred) {
		return DblFunUtil.isAll(pred, source);
	}

	public boolean isAny(DblTest pred) {
		return DblFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<Double> iterator() {
		return DblFunUtil.iterator(source);
	}

	public double last() {
		double c, c1 = EMPTYVALUE;
		while ((c = next()) != EMPTYVALUE)
			c1 = c;
		return c1;
	}

	public <O> Outlet<O> map(Dbl_Obj<O> fun) {
		return Outlet.of(DblFunUtil.map(fun, source));
	}

	public <K, V> Outlet2<K, V> map2(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0) {
		return map2_(kf0, vf0);
	}

	public DblOutlet mapDbl(Dbl_Dbl fun0) {
		return of(DblFunUtil.mapDbl(fun0, source));
	}

	public <V> DblObjOutlet<V> mapDblObj(Dbl_Obj<V> fun0) {
		return DblObjOutlet.of(DblFunUtil.mapDblObj(fun0, source));
	}

	public double max() {
		return min((c0, c1) -> Double.compare(c1, c0));
	}

	public double min() {
		return min((c0, c1) -> Double.compare(c0, c1));
	}

	public double min(DblComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != EMPTYVALUE)
			return c;
		else
			return fail("no result");
	}

	public double minOrEmpty(DblComparator comparator) {
		double c = next(), c1;
		if (c != EMPTYVALUE) {
			while ((c1 = next()) != EMPTYVALUE)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return EMPTYVALUE;
	}

	public double next() {
		return source.g();
	}

	public DblOutlet nonBlock(double c0) {
		var queue = new NullableSyncQueue<Double>();

		new Thread(() -> {
			double c;
			do
				queue.offerQuietly(c = source.g());
			while (c != EMPTYVALUE);
		}).start();

		return new DblOutlet(() -> {
			var mutable = Mutable.<Double> nil();
			var c = queue.poll(mutable) ? mutable.value() : c0;
			return c;
		});
	}

	public DblOpt opt() {
		var c = next();
		if (c != EMPTYVALUE)
			if (next() == EMPTYVALUE)
				return DblOpt.of(c);
			else
				return fail("more than one result");
		else
			return DblOpt.none();
	}

	public Pair<DblOutlet, DblOutlet> partition(DblTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public DblOutlet reverse() {
		return of(toList().toDoubles().reverse());
	}

	public void sink(DblSink sink0) {
		var sink1 = sink0.rethrow();
		double c;
		while ((c = next()) != EMPTYVALUE)
			sink1.f(c);
	}

	public DblOutlet skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next() == EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public DblOutlet snoc(double c) {
		return of(DblFunUtil.snoc(c, source));
	}

	public DblSource source() {
		return source;
	}

	public DblOutlet sort() {
		return of(toList().toDoubles().sort());
	}

	public Outlet<DblOutlet> split(DblTest fun) {
		return Outlet.of(FunUtil.map(DblOutlet::new, DblFunUtil.split(fun, source)));
	}

	public double sum() {
		double result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE)
			result += c1;
		return result;
	}

	public DblOutlet take(int n) {
		return of(new DblSource() {
			private int count = n;

			public double g() {
				return 0 < count-- ? next() : null;
			}
		});
	}

	public double[] toArray() {
		var list = toList();
		return list.toDoubles().toArray();
	}

	public DoublesBuilder toList() {
		var list = new DoublesBuilder();
		double c;
		while ((c = next()) != EMPTYVALUE)
			list.append(c);
		return list;
	}

	public <K> DblObjMap<DoublesBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> DblObjMap<DoublesBuilder> toListMap(Dbl_Dbl valueFun) {
		var map = new DblObjMap<DoublesBuilder>();
		double c;
		while ((c = next()) != EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new DoublesBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjDblMap<K> toMap(Dbl_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjDblMap<K>();
		double c;
		while ((c = next()) != EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		double c;
		while ((c = next()) != EMPTYVALUE) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				fail("duplicate key " + key);
		}
		return map;
	}

	public <K> ListMultimap<K, Double> toMultimap(Dbl_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public DblSet toSet() {
		var set = new DblSet();
		double c;
		while ((c = next()) != EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public <U, R> Outlet<R> zip(Outlet<U> outlet1, DblObj_Obj<U, R> fun) {
		return Outlet.of(() -> {
			var t = next();
			var u = outlet1.next();
			return t != EMPTYVALUE && u != null ? fun.apply(t, u) : null;
		});
	}

	private <K, V> Outlet2<K, V> map2_(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0) {
		return Outlet2.of(DblFunUtil.map2(kf0, vf0, source));
	}

}
