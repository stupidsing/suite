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
import suite.primitive.IntFunUtil;
import suite.primitive.IntOpt;
import suite.primitive.IntPrimitives.IntComparator;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.IntPrimitives.IntTest;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Int_Int;
import suite.primitive.Ints;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.adt.set.IntSet;
import suite.streamlet.As;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.OutletDefaults;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.NullableSyncQueue;
import suite.util.Object_;
import suite.util.To;

public class IntOutlet implements OutletDefaults<Integer> {

	private static int EMPTYVALUE = IntFunUtil.EMPTYVALUE;

	private IntSource source;

	@SafeVarargs
	public static IntOutlet concat(IntOutlet... outlets) {
		var sources = new ArrayList<IntSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(IntFunUtil.concat(To.source(sources)));
	}

	public static IntOutlet empty() {
		return of(IntFunUtil.nullSource());
	}

	@SafeVarargs
	public static IntOutlet of(int... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static IntOutlet of(int[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new IntSource() {
			private int i = start;

			public int source() {
				var c = pred.test(i) ? ts[i] : EMPTYVALUE;
				i += inc;
				return c;
			}
		});
	}

	public static IntOutlet of(Enumeration<Integer> en) {
		return of(To.source(en));
	}

	public static IntOutlet of(Iterable<Integer> col) {
		return of(To.source(col));
	}

	public static IntOutlet of(Source<Integer> source) {
		return IntOutlet.of(() -> {
			var c = source.source();
			return c != null ? c : EMPTYVALUE;
		});
	}

	public static IntOutlet of(IntSource source) {
		return new IntOutlet(source);
	}

	private IntOutlet(IntSource source) {
		this.source = source;
	}

	public int average() {
		var count = 0;
		int result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE) {
			result += c1;
			count++;
		}
		return (int) (result / count);
	}

	public Outlet<IntOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(IntOutlet::new, IntFunUtil.chunk(n, source)));
	}

	public IntOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			var next = next();
			if (next == EMPTYVALUE)
				Object_.closeQuietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<IntOutlet, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(Int_Obj<Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(IntFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Outlet2<K, V> concatMap2(Int_Obj<Outlet2<K, V>> fun) {
		return Outlet2.of(FunUtil2.concat(IntFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public IntOutlet concatMapInt(Int_Obj<IntOutlet> fun) {
		return of(IntFunUtil.concat(IntFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public IntOutlet cons(int c) {
		return of(IntFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (next() != EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, IntObj_Obj<U, O> fun) {
		return Outlet.of(new Source<>() {
			private int c;
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

	public IntOutlet distinct() {
		var set = new HashSet<>();
		return of(() -> {
			int c;
			while ((c = next()) != EMPTYVALUE && !set.add(c))
				;
			return c;
		});
	}

	public IntOutlet drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= next() != EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntOutlet.class) {
			var source1 = ((IntOutlet) object).source;
			int o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == EMPTYVALUE && o1 == EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public IntOutlet filter(IntTest fun) {
		return of(IntFunUtil.filter(fun, source));
	}

	public int first() {
		return next();
	}

	public <O> Outlet<O> flatMap(Int_Obj<Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(IntFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, IntObj_Obj<R, R> fun) {
		int c;
		while ((c = next()) != EMPTYVALUE)
			init = fun.apply(c, init);
		return init;
	}

	public <V> IntObjOutlet<IntsBuilder> groupBy() {
		return IntObjOutlet.of(toListMap().source());
	}

	public <V> IntObjOutlet<V> groupBy(Fun<Ints, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toInts()));
	}

	@Override
	public int hashCode() {
		var h = 7;
		int c;
		while ((c = source.source()) != EMPTYVALUE)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public IntObjOutlet<Integer> index() {
		return IntObjOutlet.of(new IntObjSource<>() {
			private int i = 0;

			public boolean source2(IntObjPair<Integer> pair) {
				var c = next();
				if (c != EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(IntTest pred) {
		return IntFunUtil.isAll(pred, source);
	}

	public boolean isAny(IntTest pred) {
		return IntFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<Integer> iterator() {
		return IntFunUtil.iterator(source);
	}

	public int last() {
		int c, c1 = EMPTYVALUE;
		while ((c = next()) != EMPTYVALUE)
			c1 = c;
		return c1;
	}

	public <O> Outlet<O> map(Int_Obj<O> fun) {
		return Outlet.of(IntFunUtil.map(fun, source));
	}

	public <K, V> Outlet2<K, V> map2(Int_Obj<K> kf0, Int_Obj<V> vf0) {
		return map2_(kf0, vf0);
	}

	public IntOutlet mapInt(Int_Int fun0) {
		return of(IntFunUtil.mapInt(fun0, source));
	}

	public <V> IntObjOutlet<V> mapIntObj(Int_Obj<V> fun0) {
		return IntObjOutlet.of(IntFunUtil.mapIntObj(fun0, source));
	}

	public int max() {
		return min((c0, c1) -> Integer.compare(c1, c0));
	}

	public int min() {
		return min((c0, c1) -> Integer.compare(c0, c1));
	}

	public int min(IntComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != EMPTYVALUE)
			return c;
		else
			return Fail.t("no result");
	}

	public int minOrEmpty(IntComparator comparator) {
		int c = next(), c1;
		if (c != EMPTYVALUE) {
			while ((c1 = next()) != EMPTYVALUE)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return EMPTYVALUE;
	}

	public int next() {
		return source.source();
	}

	public IntOutlet nonBlock(int c0) {
		var queue = new NullableSyncQueue<Integer>();

		new Thread(() -> {
			int c;
			do
				queue.offerQuietly(c = source.source());
			while (c != EMPTYVALUE);
		}).start();

		return new IntOutlet(() -> {
			var mutable = Mutable.<Integer> nil();
			var c = queue.poll(mutable) ? mutable.get() : c0;
			return c;
		});
	}

	public IntOpt opt() {
		var c = next();
		if (c != EMPTYVALUE)
			if (next() == EMPTYVALUE)
				return IntOpt.of(c);
			else
				return Fail.t("more than one result");
		else
			return IntOpt.none();
	}

	public Pair<IntOutlet, IntOutlet> partition(IntTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public IntOutlet reverse() {
		return of(toList().toInts().reverse());
	}

	public void sink(IntSink sink0) {
		var sink1 = sink0.rethrow();
		int c;
		while ((c = next()) != EMPTYVALUE)
			sink1.sink(c);
	}

	public IntOutlet skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next() == EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public IntOutlet snoc(int c) {
		return of(IntFunUtil.snoc(c, source));
	}

	public IntSource source() {
		return source;
	}

	public IntOutlet sort() {
		return of(toList().toInts().sort());
	}

	public Outlet<IntOutlet> split(IntTest fun) {
		return Outlet.of(FunUtil.map(IntOutlet::new, IntFunUtil.split(fun, source)));
	}

	public int sum() {
		int result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE)
			result += c1;
		return result;
	}

	public IntOutlet take(int n) {
		return of(new IntSource() {
			private int count = n;

			public int source() {
				return 0 < count-- ? next() : null;
			}
		});
	}

	public int[] toArray() {
		var list = toList();
		return list.toInts().toArray();
	}

	public IntsBuilder toList() {
		var list = new IntsBuilder();
		int c;
		while ((c = next()) != EMPTYVALUE)
			list.append(c);
		return list;
	}

	public <K> IntObjMap<IntsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> IntObjMap<IntsBuilder> toListMap(Int_Int valueFun) {
		var map = new IntObjMap<IntsBuilder>();
		int c;
		while ((c = next()) != EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new IntsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjIntMap<K> toMap(Int_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjIntMap<K>();
		int c;
		while ((c = next()) != EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Int_Obj<K> kf0, Int_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		int c;
		while ((c = next()) != EMPTYVALUE) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				Fail.t("duplicate key " + key);
		}
		return map;
	}

	public <K> ListMultimap<K, Integer> toMultimap(Int_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public IntSet toSet() {
		var set = new IntSet();
		int c;
		while ((c = next()) != EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	private <K, V> Outlet2<K, V> map2_(Int_Obj<K> kf0, Int_Obj<V> vf0) {
		return Outlet2.of(IntFunUtil.map2(kf0, vf0, source));
	}

}
