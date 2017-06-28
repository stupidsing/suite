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

import suite.adt.Mutable;
import suite.adt.map.ListMultimap;
import suite.primitive.DblFunUtil;
import suite.primitive.DblOpt;
import suite.primitive.DblPrimitives.DblComparator;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.DblPrimitives.DblPredicate;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.Dbl_Dbl;
import suite.primitive.Doubles;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.adt.map.DblObjMap;
import suite.primitive.adt.pair.DblObjPair;
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
public class DblOutlet implements Iterable<Double> {

	private DblSource source;

	@SafeVarargs
	public static DblOutlet concat(DblOutlet... outlets) {
		List<DblSource> sources = new ArrayList<>();
		for (DblOutlet outlet : outlets)
			sources.add(outlet.source);
		return of(DblFunUtil.concat(To.source(sources)));
	}

	public static DblOutlet empty() {
		return of(DblFunUtil.nullSource());
	}

	@SafeVarargs
	public static DblOutlet of(double... ts) {
		return of(new DblSource() {
			private int i;

			public double source() {
				return i < ts.length ? ts[i++] : DblFunUtil.EMPTYVALUE;
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
		return of(() -> source.source());
	}

	public static DblOutlet of(DblSource source) {
		return new DblOutlet(source);
	}

	private DblOutlet(DblSource source) {
		this.source = source;
	}

	@Override
	public Iterator<Double> iterator() {
		return DblFunUtil.iterator(source);
	}

	public DblOutlet append(double t) {
		return of(DblFunUtil.append(t, source));
	}

	public Outlet<DblOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(DblOutlet::new, DblFunUtil.chunk(n, source)));
	}

	public DblOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			double next = next();
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

	public DblOutlet cons(double t) {
		return of(DblFunUtil.cons(t, source));
	}

	public int count() {
		int i = 0;
		while (next() != DblFunUtil.EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, DblObj_Obj<U, O> fun) {
		return Outlet.of(new Source<O>() {
			private double t;
			private int index = list.size();

			public O source() {
				if (index == list.size()) {
					index = 0;
					t = next();
				}
				return fun.apply(t, list.get(index++));
			}
		});
	}

	public DblOutlet distinct() {
		Set<Double> set = new HashSet<>();
		return of(() -> {
			double t;
			while ((t = next()) != DblFunUtil.EMPTYVALUE && !set.add(t))
				;
			return t;
		});
	}

	public DblOutlet drop(int n) {
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next() != DblFunUtil.EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == DblOutlet.class) {
			DblSource source1 = ((DblOutlet) object).source;
			double o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == DblFunUtil.EMPTYVALUE && o1 == DblFunUtil.EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public DblOutlet filter(DblPredicate fun) {
		return of(DblFunUtil.filter(fun, source));
	}

	public double first() {
		return next();
	}

	public DblOutlet flatMap(Dbl_Obj<Iterable<Double>> fun) {
		return of(FunUtil.flatten(DblFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, DblObj_Obj<R, R> fun) {
		double t;
		while ((t = next()) != DblFunUtil.EMPTYVALUE)
			init = fun.apply(t, init);
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
		int hashCode = 5;
		double t;
		while ((t = source.source()) != DblFunUtil.EMPTYVALUE)
			hashCode = hashCode * 31 + Objects.hashCode(t);
		return hashCode;
	}

	public DblObjOutlet<Integer> index() {
		return DblObjOutlet.of(new DblObjSource<Integer>() {
			private int i = 0;

			public boolean source2(DblObjPair<Integer> pair) {
				double t = next();
				if (t != DblFunUtil.EMPTYVALUE) {
					pair.t0 = t;
					pair.t1 = i++;
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(DblPredicate pred) {
		return DblFunUtil.isAll(pred, source);
	}

	public boolean isAny(DblPredicate pred) {
		return DblFunUtil.isAny(pred, source);
	}

	public double last() {
		double t, t1 = DblFunUtil.EMPTYVALUE;
		while ((t = next()) != DblFunUtil.EMPTYVALUE)
			t1 = t;
		return t1;
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

	public <O> Outlet<O> mapNonNull(Dbl_Obj<O> fun) {
		return Outlet.of(DblFunUtil.mapNonNull(fun, source));
	}

	public double min(DblComparator comparator) {
		double t = minOrNull(comparator);
		if (t != DblFunUtil.EMPTYVALUE)
			return t;
		else
			throw new RuntimeException("no result");
	}

	public double minOrNull(DblComparator comparator) {
		double t = next(), t1;
		if (t != DblFunUtil.EMPTYVALUE) {
			while ((t1 = next()) != DblFunUtil.EMPTYVALUE)
				if (0 < comparator.compare(t, t1))
					t = t1;
			return t;
		} else
			return DblFunUtil.EMPTYVALUE;
	}

	public double next() {
		return source.source();
	}

	public DblOutlet nonBlock(double t0) {
		NullableSyncQueue<Double> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			double t;
			do
				queue.offerQuietly(t = source.source());
			while (t != DblFunUtil.EMPTYVALUE);
		}).start();

		return new DblOutlet(() -> {
			Mutable<Double> mutable = Mutable.nil();
			double c = queue.poll(mutable) ? mutable.get() : t0;
			return c;
		});
	}

	public DblOpt opt() {
		double t = next();
		if (t != DblFunUtil.EMPTYVALUE)
			if (next() == DblFunUtil.EMPTYVALUE)
				return DblOpt.of(t);
			else
				throw new RuntimeException("more than one result");
		else
			return DblOpt.none();
	}

	public DblOutlet reverse() {
		return of(toList().toDoubles().reverse());
	}

	public void sink(DblSink sink0) {
		DblSink sink1 = sink0.rethrow();
		double t;
		while ((t = next()) != DblFunUtil.EMPTYVALUE)
			sink1.sink(t);
	}

	public DblOutlet skip(int n) {
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next() == DblFunUtil.EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public DblSource source() {
		return source;
	}

	public DblOutlet sort() {
		return of(toList().toDoubles().sort());
	}

	public Outlet<DblOutlet> split(DblPredicate fun) {
		return Outlet.of(FunUtil.map(DblOutlet::new, DblFunUtil.split(fun, source)));
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
		DoublesBuilder list = toList();
		return list.toDoubles().toDoubleArray();
	}

	public DoublesBuilder toList() {
		DoublesBuilder list = new DoublesBuilder();
		double t;
		while ((t = next()) != DblFunUtil.EMPTYVALUE)
			list.append(t);
		return list;
	}

	public <K> DblObjMap<DoublesBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> DblObjMap<DoublesBuilder> toListMap(Dbl_Dbl valueFun) {
		DblObjMap<DoublesBuilder> map = new DblObjMap<>();
		double t;
		while ((t = next()) != DblFunUtil.EMPTYVALUE)
			map.computeIfAbsent(t, k_ -> new DoublesBuilder()).append(valueFun.apply(t));
		return map;
	}

	public <K> Map<K, Double> toMap(Dbl_Obj<K> keyFun) {
		return toMap(keyFun, value -> (Double) value);
	}

	public <K, V> Map<K, V> toMap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).uniqueResult()).collect(As::map);
	}

	public <K> ListMultimap<K, Double> toMultimap(Dbl_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public Set<Double> toSet() {
		Set<Double> set = new HashSet<>();
		double t;
		while ((t = next()) != DblFunUtil.EMPTYVALUE)
			set.add(t);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).collect(As::map);
	}

	private <K, V> Outlet2<K, V> map2_(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0) {
		return Outlet2.of(DblFunUtil.map2(kf0, vf0, source));
	}

}
