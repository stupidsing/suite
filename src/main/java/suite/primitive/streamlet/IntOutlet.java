package suite.primitive.streamlet;

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
import suite.primitive.IntFunUtil;
import suite.primitive.IntOpt;
import suite.primitive.IntPrimitives.IntComparator;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.IntPredicate;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Int_Int;
import suite.primitive.Ints;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.primitive.adt.set.IntSet;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
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
public class IntOutlet implements Iterable<Integer> {

	private IntSource source;

	@SafeVarargs
	public static IntOutlet concat(IntOutlet... outlets) {
		List<IntSource> sources = new ArrayList<>();
		for (IntOutlet outlet : outlets)
			sources.add(outlet.source);
		return of(IntFunUtil.concat(To.source(sources)));
	}

	public static IntOutlet empty() {
		return of(IntFunUtil.nullSource());
	}

	@SafeVarargs
	public static IntOutlet of(int... ts) {
		return of(new IntSource() {
			private int i;

			public int source() {
				return i < ts.length ? ts[i++] : IntFunUtil.EMPTYVALUE;
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
		return of(() -> source.source());
	}

	public static IntOutlet of(IntSource source) {
		return new IntOutlet(source);
	}

	private IntOutlet(IntSource source) {
		this.source = source;
	}

	@Override
	public Iterator<Integer> iterator() {
		return IntFunUtil.iterator(source);
	}

	public IntOutlet append(int c) {
		return of(IntFunUtil.append(c, source));
	}

	public Outlet<IntOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(IntOutlet::new, IntFunUtil.chunk(n, source)));
	}

	public IntOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			int next = next();
			if (next == IntFunUtil.EMPTYVALUE)
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
		int i = 0;
		while (next() != IntFunUtil.EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, IntObj_Obj<U, O> fun) {
		return Outlet.of(new Source<O>() {
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
		Set<Integer> set = new HashSet<>();
		return of(() -> {
			int c;
			while ((c = next()) != IntFunUtil.EMPTYVALUE && !set.add(c))
				;
			return c;
		});
	}

	public IntOutlet drop(int n) {
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next() != IntFunUtil.EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == IntOutlet.class) {
			IntSource source1 = ((IntOutlet) object).source;
			int o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == IntFunUtil.EMPTYVALUE && o1 == IntFunUtil.EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public IntOutlet filter(IntPredicate fun) {
		return of(IntFunUtil.filter(fun, source));
	}

	public int first() {
		return next();
	}

	public IntOutlet flatMap(Int_Obj<Iterable<Integer>> fun) {
		return of(FunUtil.flatten(IntFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, IntObj_Obj<R, R> fun) {
		int c;
		while ((c = next()) != IntFunUtil.EMPTYVALUE)
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
		int hashCode = 5;
		int c;
		while ((c = source.source()) != IntFunUtil.EMPTYVALUE)
			hashCode = hashCode * 31 + Objects.hashCode(c);
		return hashCode;
	}

	public IntObjOutlet<Integer> index() {
		return IntObjOutlet.of(new IntObjSource<Integer>() {
			private int i = 0;

			public boolean source2(IntObjPair<Integer> pair) {
				int c = next();
				if (c != IntFunUtil.EMPTYVALUE) {
					pair.t0 = c;
					pair.t1 = i++;
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(IntPredicate pred) {
		return IntFunUtil.isAll(pred, source);
	}

	public boolean isAny(IntPredicate pred) {
		return IntFunUtil.isAny(pred, source);
	}

	public int last() {
		int c, c1 = IntFunUtil.EMPTYVALUE;
		while ((c = next()) != IntFunUtil.EMPTYVALUE)
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

	public <O> Outlet<O> mapNonNull(Int_Obj<O> fun) {
		return Outlet.of(IntFunUtil.mapNonNull(fun, source));
	}

	public int max() {
		return min((c0, c1) -> Integer.compare(c1, c0));
	}

	public int min() {
		return min((c0, c1) -> Integer.compare(c0, c1));
	}

	public int min(IntComparator comparator) {
		int c = minOrEmpty(comparator);
		if (c != IntFunUtil.EMPTYVALUE)
			return c;
		else
			throw new RuntimeException("no result");
	}

	public int minOrEmpty(IntComparator comparator) {
		int c = next(), c1;
		if (c != IntFunUtil.EMPTYVALUE) {
			while ((c1 = next()) != IntFunUtil.EMPTYVALUE)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return IntFunUtil.EMPTYVALUE;
	}

	public int next() {
		return source.source();
	}

	public IntOutlet nonBlock(int c0) {
		NullableSyncQueue<Integer> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			int c;
			do
				queue.offerQuietly(c = source.source());
			while (c != IntFunUtil.EMPTYVALUE);
		}).start();

		return new IntOutlet(() -> {
			Mutable<Integer> mutable = Mutable.nil();
			int c = queue.poll(mutable) ? mutable.get() : c0;
			return c;
		});
	}

	public IntOpt opt() {
		int c = next();
		if (c != IntFunUtil.EMPTYVALUE)
			if (next() == IntFunUtil.EMPTYVALUE)
				return IntOpt.of(c);
			else
				throw new RuntimeException("more than one result");
		else
			return IntOpt.none();
	}

	public IntOutlet reverse() {
		return of(toList().toInts().reverse());
	}

	public void sink(IntSink sink0) {
		IntSink sink1 = sink0.rethrow();
		int c;
		while ((c = next()) != IntFunUtil.EMPTYVALUE)
			sink1.sink(c);
	}

	public IntOutlet skip(int n) {
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next() == IntFunUtil.EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public IntSource source() {
		return source;
	}

	public IntOutlet sort() {
		return of(toList().toInts().sort());
	}

	public Outlet<IntOutlet> split(IntPredicate fun) {
		return Outlet.of(FunUtil.map(IntOutlet::new, IntFunUtil.split(fun, source)));
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
		IntsBuilder list = toList();
		return list.toInts().toArray();
	}

	public IntsBuilder toList() {
		IntsBuilder list = new IntsBuilder();
		int c;
		while ((c = next()) != IntFunUtil.EMPTYVALUE)
			list.append(c);
		return list;
	}

	public <K> IntObjMap<IntsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> IntObjMap<IntsBuilder> toListMap(Int_Int valueFun) {
		IntObjMap<IntsBuilder> map = new IntObjMap<>();
		int c;
		while ((c = next()) != IntFunUtil.EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new IntsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> Map<K, Integer> toMap(Int_Obj<K> keyFun) {
		return toMap(keyFun, value -> (Integer) value);
	}

	public <K, V> Map<K, V> toMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).uniqueResult()).collect(As::map);
	}

	public <K> ListMultimap<K, Integer> toMultimap(Int_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public IntSet toSet() {
		IntSet set = new IntSet();
		int c;
		while ((c = next()) != IntFunUtil.EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).collect(As::map);
	}

	private <K, V> Outlet2<K, V> map2_(Int_Obj<K> kf0, Int_Obj<V> vf0) {
		return Outlet2.of(IntFunUtil.map2(kf0, vf0, source));
	}

}
