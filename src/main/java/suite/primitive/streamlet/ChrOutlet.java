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
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrFunUtil;
import suite.primitive.ChrOpt;
import suite.primitive.ChrPrimitives.ChrComparator;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.ChrObj_Obj;
import suite.primitive.ChrPrimitives.ChrSink;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.ChrPrimitives.ChrTest;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.primitive.Chr_Chr;
import suite.primitive.adt.map.ChrObjMap;
import suite.primitive.adt.map.ObjChrMap;
import suite.primitive.adt.pair.ChrObjPair;
import suite.primitive.adt.set.ChrSet;
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

public class ChrOutlet implements OutletDefaults<Character> {

	private static char EMPTYVALUE = ChrFunUtil.EMPTYVALUE;

	private ChrSource source;

	@SafeVarargs
	public static ChrOutlet concat(ChrOutlet... outlets) {
		var sources = new ArrayList<ChrSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(ChrFunUtil.concat(To.source(sources)));
	}

	public static ChrOutlet empty() {
		return of(ChrFunUtil.nullSource());
	}

	@SafeVarargs
	public static ChrOutlet of(char... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static ChrOutlet of(char[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new ChrSource() {
			private int i = start;

			public char source() {
				var c = pred.test(i) ? ts[i] : EMPTYVALUE;
				i += inc;
				return c;
			}
		});
	}

	public static ChrOutlet of(Enumeration<Character> en) {
		return of(To.source(en));
	}

	public static ChrOutlet of(Iterable<Character> col) {
		return of(To.source(col));
	}

	public static ChrOutlet of(Source<Character> source) {
		return ChrOutlet.of(() -> {
			var c = source.source();
			return c != null ? c : EMPTYVALUE;
		});
	}

	public static ChrOutlet of(ChrSource source) {
		return new ChrOutlet(source);
	}

	private ChrOutlet(ChrSource source) {
		this.source = source;
	}

	public char average() {
		var count = 0;
		char result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE) {
			result += c1;
			count++;
		}
		return (char) (result / count);
	}

	public Outlet<ChrOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(ChrOutlet::new, ChrFunUtil.chunk(n, source)));
	}

	public ChrOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			var next = next();
			if (next == EMPTYVALUE)
				Object_.closeQuietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<ChrOutlet, R> fun) {
		return fun.apply(this);
	}

	public <O> Outlet<O> concatMap(Chr_Obj<Outlet<O>> fun) {
		return Outlet.of(FunUtil.concat(ChrFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Outlet2<K, V> concatMap2(Chr_Obj<Outlet2<K, V>> fun) {
		return Outlet2.of(FunUtil2.concat(ChrFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public ChrOutlet concatMapChr(Chr_Obj<ChrOutlet> fun) {
		return of(ChrFunUtil.concat(ChrFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public ChrOutlet cons(char c) {
		return of(ChrFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (next() != EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, ChrObj_Obj<U, O> fun) {
		return Outlet.of(new Source<>() {
			private char c;
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

	public ChrOutlet distinct() {
		var set = new HashSet<>();
		return of(() -> {
			char c;
			while ((c = next()) != EMPTYVALUE && !set.add(c))
				;
			return c;
		});
	}

	public ChrOutlet drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= next() != EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrOutlet.class) {
			var source1 = ((ChrOutlet) object).source;
			char o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == EMPTYVALUE && o1 == EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public ChrOutlet filter(ChrTest fun) {
		return of(ChrFunUtil.filter(fun, source));
	}

	public char first() {
		return next();
	}

	public <O> Outlet<O> flatMap(Chr_Obj<Iterable<O>> fun) {
		return Outlet.of(FunUtil.flatten(ChrFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, ChrObj_Obj<R, R> fun) {
		char c;
		while ((c = next()) != EMPTYVALUE)
			init = fun.apply(c, init);
		return init;
	}

	public <V> ChrObjOutlet<CharsBuilder> groupBy() {
		return ChrObjOutlet.of(toListMap().source());
	}

	public <V> ChrObjOutlet<V> groupBy(Fun<Chars, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toChars()));
	}

	@Override
	public int hashCode() {
		var h = 7;
		char c;
		while ((c = source.source()) != EMPTYVALUE)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public ChrObjOutlet<Integer> index() {
		return ChrObjOutlet.of(new ChrObjSource<>() {
			private int i = 0;

			public boolean source2(ChrObjPair<Integer> pair) {
				var c = next();
				if (c != EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(ChrTest pred) {
		return ChrFunUtil.isAll(pred, source);
	}

	public boolean isAny(ChrTest pred) {
		return ChrFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<Character> iterator() {
		return ChrFunUtil.iterator(source);
	}

	public char last() {
		char c, c1 = EMPTYVALUE;
		while ((c = next()) != EMPTYVALUE)
			c1 = c;
		return c1;
	}

	public <O> Outlet<O> map(Chr_Obj<O> fun) {
		return Outlet.of(ChrFunUtil.map(fun, source));
	}

	public <K, V> Outlet2<K, V> map2(Chr_Obj<K> kf0, Chr_Obj<V> vf0) {
		return map2_(kf0, vf0);
	}

	public ChrOutlet mapChr(Chr_Chr fun0) {
		return of(ChrFunUtil.mapChr(fun0, source));
	}

	public <V> ChrObjOutlet<V> mapChrObj(Chr_Obj<V> fun0) {
		return ChrObjOutlet.of(ChrFunUtil.mapChrObj(fun0, source));
	}

	public char max() {
		return min((c0, c1) -> Character.compare(c1, c0));
	}

	public char min() {
		return min((c0, c1) -> Character.compare(c0, c1));
	}

	public char min(ChrComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != EMPTYVALUE)
			return c;
		else
			return fail("no result");
	}

	public char minOrEmpty(ChrComparator comparator) {
		char c = next(), c1;
		if (c != EMPTYVALUE) {
			while ((c1 = next()) != EMPTYVALUE)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return EMPTYVALUE;
	}

	public char next() {
		return source.source();
	}

	public ChrOutlet nonBlock(char c0) {
		var queue = new NullableSyncQueue<Character>();

		new Thread(() -> {
			char c;
			do
				queue.offerQuietly(c = source.source());
			while (c != EMPTYVALUE);
		}).start();

		return new ChrOutlet(() -> {
			var mutable = Mutable.<Character> nil();
			var c = queue.poll(mutable) ? mutable.value() : c0;
			return c;
		});
	}

	public ChrOpt opt() {
		var c = next();
		if (c != EMPTYVALUE)
			if (next() == EMPTYVALUE)
				return ChrOpt.of(c);
			else
				return fail("more than one result");
		else
			return ChrOpt.none();
	}

	public Pair<ChrOutlet, ChrOutlet> partition(ChrTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public ChrOutlet reverse() {
		return of(toList().toChars().reverse());
	}

	public void sink(ChrSink sink0) {
		var sink1 = sink0.rethrow();
		char c;
		while ((c = next()) != EMPTYVALUE)
			sink1.sink(c);
	}

	public ChrOutlet skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = next() == EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public ChrOutlet snoc(char c) {
		return of(ChrFunUtil.snoc(c, source));
	}

	public ChrSource source() {
		return source;
	}

	public ChrOutlet sort() {
		return of(toList().toChars().sort());
	}

	public Outlet<ChrOutlet> split(ChrTest fun) {
		return Outlet.of(FunUtil.map(ChrOutlet::new, ChrFunUtil.split(fun, source)));
	}

	public char sum() {
		char result = 0, c1;
		while ((c1 = next()) != EMPTYVALUE)
			result += c1;
		return result;
	}

	public ChrOutlet take(int n) {
		return of(new ChrSource() {
			private int count = n;

			public char source() {
				return 0 < count-- ? next() : null;
			}
		});
	}

	public char[] toArray() {
		var list = toList();
		return list.toChars().toArray();
	}

	public CharsBuilder toList() {
		var list = new CharsBuilder();
		char c;
		while ((c = next()) != EMPTYVALUE)
			list.append(c);
		return list;
	}

	public <K> ChrObjMap<CharsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> ChrObjMap<CharsBuilder> toListMap(Chr_Chr valueFun) {
		var map = new ChrObjMap<CharsBuilder>();
		char c;
		while ((c = next()) != EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new CharsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjChrMap<K> toMap(Chr_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjChrMap<K>();
		char c;
		while ((c = next()) != EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Chr_Obj<K> kf0, Chr_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		char c;
		while ((c = next()) != EMPTYVALUE) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				fail("duplicate key " + key);
		}
		return map;
	}

	public <K> ListMultimap<K, Character> toMultimap(Chr_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public ChrSet toSet() {
		var set = new ChrSet();
		char c;
		while ((c = next()) != EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public <U, R> Outlet<R> zip(Outlet<U> outlet1, ChrObj_Obj<U, R> fun) {
		return Outlet.of(() -> {
			var t = next();
			var u = outlet1.next();
			return t != EMPTYVALUE && u != null ? fun.apply(t, u) : null;
		});
	}

	private <K, V> Outlet2<K, V> map2_(Chr_Obj<K> kf0, Chr_Obj<V> vf0) {
		return Outlet2.of(ChrFunUtil.map2(kf0, vf0, source));
	}

}
