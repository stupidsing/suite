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

/***
 * Implement functional structures using
 * 
 * class methods( instead of static* methods in
 * 
 * class FunUtil),just for easier code completion in source editor.**
 * 
 * @author ywsing
 */
public class DblOutlet implements OutletDefaults<Double> {

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

			public double source() {
				var c = pred.test(i) ? ts[i] : DblFunUtil.EMPTYVALUE;
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
			var c = source.source();
			return c != null ? c : DblFunUtil.EMPTYVALUE;
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
		while ((c1 = next()) != DblFunUtil.EMPTYVALUE) {
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
			if (next == DblFunUtil.EMPTYVALUE)
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
		while (next() != DblFunUtil.EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, DblObj_Obj<U, O> fun) {
		return Outlet.of(new Source<>() {
			private double c;
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

	public DblOutlet distinct() {
		var set = new HashSet<>();
		return of(() -> {
			double c;
			while ((c = next()) != DblFunUtil.EMPTYVALUE && !set.add(c))
				;
			return c;
		});
	}

	public DblOutlet drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= next() != DblFunUtil.EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblOutlet.class) {
			var source1 = ((DblOutlet) object).source;
			double o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == DblFunUtil.EMPTYVALUE && o1 == DblFunUtil.EMPTYVALUE)
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
		while ((c = next()) != DblFunUtil.EMPTYVALUE)
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
		while ((c = source.source()) != DblFunUtil.EMPTYVALUE)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public DblObjOutlet<Integer> index() {
		return DblObjOutlet.of(new DblObjSource<>() {
			private int i = 0;

			public boolean source2(DblObjPair<Integer> pair) {
				var c = next();
				if (c != DblFunUtil.EMPTYVALUE) {
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
		double c, c1 = DblFunUtil.EMPTYVALUE;
		while ((c = next()) != DblFunUtil.EMPTYVALUE)
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
		if (c != DblFunUtil.EMPTYVALUE)
			return c;
		else
			return Fail.t("no result");
	}

	public double minOrEmpty(DblComparator comparator) {
		double c = next(), c1;
		if (c != DblFunUtil.EMPTYVALUE) {
			while ((c1 = next()) != DblFunUtil.EMPTYVALUE)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return DblFunUtil.EMPTYVALUE;
	}

	public double next() {
		return source.source();
	}

	public DblOutlet nonBlock(double c0) {
		var queue = new NullableSyncQueue<Double>();

		new Thread(() -> {
			double c;
			do
				queue.offerQuietly(c = source.source());
			while (c != DblFunUtil.EMPTYVALUE);
		}).start();

		return new DblOutlet(() -> {
			var mutable = Mutable.<Double> nil();
			var c = queue.poll(mutable) ? mutable.get() : c0;
			return c;
		});
	}

	public DblOpt opt() {
		var c = next();
		if (c != DblFunUtil.EMPTYVALUE)
			if (next() == DblFunUtil.EMPTYVALUE)
				return DblOpt.of(c);
			else
				return Fail.t("more than one result");
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
		while ((c = next()) != DblFunUtil.EMPTYVALUE)
			sink1.sink(c);
	}

	public DblOutlet skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next() == DblFunUtil.EMPTYVALUE;
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
		while ((c1 = next()) != DblFunUtil.EMPTYVALUE)
			result += c1;
		return result;
	}

	public DblOutlet take(int n) {
		return of(new DblSource() {
			private int count = n;

			public double source() {
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
		while ((c = next()) != DblFunUtil.EMPTYVALUE)
			list.append(c);
		return list;
	}

	public <K> DblObjMap<DoublesBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> DblObjMap<DoublesBuilder> toListMap(Dbl_Dbl valueFun) {
		var map = new DblObjMap<DoublesBuilder>();
		double c;
		while ((c = next()) != DblFunUtil.EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new DoublesBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjDblMap<K> toMap(Dbl_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjDblMap<K>();
		double c;
		while ((c = next()) != DblFunUtil.EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		double c;
		while ((c = next()) != DblFunUtil.EMPTYVALUE) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				Fail.t("duplicate key " + key);
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
		while ((c = next()) != DblFunUtil.EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).collect(As::map);
	}

	private <K, V> Outlet2<K, V> map2_(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0) {
		return Outlet2.of(DblFunUtil.map2(kf0, vf0, source));
	}

}
