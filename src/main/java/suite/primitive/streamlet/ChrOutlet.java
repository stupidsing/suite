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
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.ChrFunUtil;
import suite.primitive.ChrOpt;
import suite.primitive.ChrPrimitives.ChrComparator;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.ChrObj_Obj;
import suite.primitive.ChrPrimitives.ChrPredicate;
import suite.primitive.ChrPrimitives.ChrSink;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.primitive.Chr_Chr;
import suite.primitive.adt.map.ChrObjMap;
import suite.primitive.adt.pair.ChrObjPair;
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
public class ChrOutlet implements Iterable<Character> {

	private ChrSource source;

	@SafeVarargs
	public static ChrOutlet concat(ChrOutlet... outlets) {
		List<ChrSource> sources = new ArrayList<>();
		for (ChrOutlet outlet : outlets)
			sources.add(outlet.source);
		return of(ChrFunUtil.concat(To.source(sources)));
	}

	public static ChrOutlet empty() {
		return of(ChrFunUtil.nullSource());
	}

	@SafeVarargs
	public static ChrOutlet of(char... ts) {
		return of(new ChrSource() {
			private int i;

			public char source() {
				return i < ts.length ? ts[i++] : ChrFunUtil.EMPTYVALUE;
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
		return of(() -> source.source());
	}

	public static ChrOutlet of(ChrSource source) {
		return new ChrOutlet(source);
	}

	private ChrOutlet(ChrSource source) {
		this.source = source;
	}

	@Override
	public Iterator<Character> iterator() {
		return ChrFunUtil.iterator(source);
	}

	public ChrOutlet append(char t) {
		return of(ChrFunUtil.append(t, source));
	}

	public Outlet<ChrOutlet> chunk(int n) {
		return Outlet.of(FunUtil.map(ChrOutlet::new, ChrFunUtil.chunk(n, source)));
	}

	public ChrOutlet closeAtEnd(Closeable c) {
		return of(() -> {
			char next = next();
			if (next == ChrFunUtil.EMPTYVALUE)
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

	public ChrOutlet cons(char t) {
		return of(ChrFunUtil.cons(t, source));
	}

	public int count() {
		int i = 0;
		while (next() != ChrFunUtil.EMPTYVALUE)
			i++;
		return i;
	}

	public <U, O> Outlet<O> cross(List<U> list, ChrObj_Obj<U, O> fun) {
		return Outlet.of(new Source<O>() {
			private char t;
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

	public ChrOutlet distinct() {
		Set<Character> set = new HashSet<>();
		return of(() -> {
			char t;
			while ((t = next()) != ChrFunUtil.EMPTYVALUE && !set.add(t))
				;
			return t;
		});
	}

	public ChrOutlet drop(int n) {
		boolean isAvailable = true;
		while (0 < n && (isAvailable &= next() != ChrFunUtil.EMPTYVALUE))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == ChrOutlet.class) {
			ChrSource source1 = ((ChrOutlet) object).source;
			char o0, o1;
			while (Objects.equals(o0 = source.source(), o1 = source1.source()))
				if (o0 == ChrFunUtil.EMPTYVALUE && o1 == ChrFunUtil.EMPTYVALUE)
					return true;
			return false;
		} else
			return false;
	}

	public ChrOutlet filter(ChrPredicate fun) {
		return of(ChrFunUtil.filter(fun, source));
	}

	public char first() {
		return next();
	}

	public ChrOutlet flatMap(Chr_Obj<Iterable<Character>> fun) {
		return of(FunUtil.flatten(ChrFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, ChrObj_Obj<R, R> fun) {
		char t;
		while ((t = next()) != ChrFunUtil.EMPTYVALUE)
			init = fun.apply(t, init);
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
		int hashCode = 5;
		char t;
		while ((t = source.source()) != ChrFunUtil.EMPTYVALUE)
			hashCode = hashCode * 31 + Objects.hashCode(t);
		return hashCode;
	}

	public ChrObjOutlet<Integer> index() {
		return ChrObjOutlet.of(new ChrObjSource<Integer>() {
			private int i = 0;

			public boolean source2(ChrObjPair<Integer> pair) {
				char t = next();
				if (t != ChrFunUtil.EMPTYVALUE) {
					pair.t0 = t;
					pair.t1 = i++;
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(ChrPredicate pred) {
		return ChrFunUtil.isAll(pred, source);
	}

	public boolean isAny(ChrPredicate pred) {
		return ChrFunUtil.isAny(pred, source);
	}

	public char last() {
		char t, t1 = ChrFunUtil.EMPTYVALUE;
		while ((t = next()) != ChrFunUtil.EMPTYVALUE)
			t1 = t;
		return t1;
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

	public <O> Outlet<O> mapNonNull(Chr_Obj<O> fun) {
		return Outlet.of(ChrFunUtil.mapNonNull(fun, source));
	}

	public char min(ChrComparator comparator) {
		char t = minOrNull(comparator);
		if (t != ChrFunUtil.EMPTYVALUE)
			return t;
		else
			throw new RuntimeException("no result");
	}

	public char minOrNull(ChrComparator comparator) {
		char t = next(), t1;
		if (t != ChrFunUtil.EMPTYVALUE) {
			while ((t1 = next()) != ChrFunUtil.EMPTYVALUE)
				if (0 < comparator.compare(t, t1))
					t = t1;
			return t;
		} else
			return ChrFunUtil.EMPTYVALUE;
	}

	public char next() {
		return source.source();
	}

	public ChrOutlet nonBlock(char t0) {
		NullableSyncQueue<Character> queue = new NullableSyncQueue<>();

		new Thread(() -> {
			char t;
			do
				queue.offerQuietly(t = source.source());
			while (t != ChrFunUtil.EMPTYVALUE);
		}).start();

		return new ChrOutlet(() -> {
			Mutable<Character> mutable = Mutable.nil();
			char c = queue.poll(mutable) ? mutable.get() : t0;
			return c;
		});
	}

	public ChrOpt opt() {
		char t = next();
		if (t != ChrFunUtil.EMPTYVALUE)
			if (next() == ChrFunUtil.EMPTYVALUE)
				return ChrOpt.of(t);
			else
				throw new RuntimeException("more than one result");
		else
			return ChrOpt.none();
	}

	public ChrOutlet reverse() {
		return of(toList().toChars().reverse());
	}

	public void sink(ChrSink sink0) {
		ChrSink sink1 = sink0.rethrow();
		char t;
		while ((t = next()) != ChrFunUtil.EMPTYVALUE)
			sink1.sink(t);
	}

	public ChrOutlet skip(int n) {
		boolean end = false;
		for (int i = 0; !end && i < n; i++)
			end = next() == ChrFunUtil.EMPTYVALUE;
		return !end ? of(source) : empty();
	}

	public ChrSource source() {
		return source;
	}

	public ChrOutlet sort() {
		return of(toList().toChars().sort());
	}

	public Outlet<ChrOutlet> split(ChrPredicate fun) {
		return Outlet.of(FunUtil.map(ChrOutlet::new, ChrFunUtil.split(fun, source)));
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
		CharsBuilder list = toList();
		return list.toChars().toArray();
	}

	public CharsBuilder toList() {
		CharsBuilder list = new CharsBuilder();
		char t;
		while ((t = next()) != ChrFunUtil.EMPTYVALUE)
			list.append(t);
		return list;
	}

	public <K> ChrObjMap<CharsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> ChrObjMap<CharsBuilder> toListMap(Chr_Chr valueFun) {
		ChrObjMap<CharsBuilder> map = new ChrObjMap<>();
		char t;
		while ((t = next()) != ChrFunUtil.EMPTYVALUE)
			map.computeIfAbsent(t, k_ -> new CharsBuilder()).append(valueFun.apply(t));
		return map;
	}

	public <K> Map<K, Character> toMap(Chr_Obj<K> keyFun) {
		return toMap(keyFun, value -> (Character) value);
	}

	public <K, V> Map<K, V> toMap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).uniqueResult()).collect(As::map);
	}

	public <K> ListMultimap<K, Character> toMultimap(Chr_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public Set<Character> toSet() {
		Set<Character> set = new HashSet<>();
		char t;
		while ((t = next()) != ChrFunUtil.EMPTYVALUE)
			set.add(t);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).collect(As::map);
	}

	private <K, V> Outlet2<K, V> map2_(Chr_Obj<K> kf0, Chr_Obj<V> vf0) {
		return Outlet2.of(ChrFunUtil.map2(kf0, vf0, source));
	}

}
