package suite.primitive.streamlet;

import static primal.statics.Fail.fail;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;

import primal.NullableSyncQueue;
import primal.Verbs.Close;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.Verbs.Take;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.FunUtil2;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.primitive.ChrOpt;
import primal.primitive.ChrPrim;
import primal.primitive.ChrPrim.ChrComparator;
import primal.primitive.ChrPrim.ChrObj_Obj;
import primal.primitive.ChrPrim.ChrSink;
import primal.primitive.ChrPrim.ChrSource;
import primal.primitive.ChrPrim.ChrTest;
import primal.primitive.ChrPrim.Chr_Obj;
import primal.primitive.Chr_Chr;
import primal.primitive.fp.ChrFunUtil;
import primal.puller.Puller;
import primal.puller.Puller2;
import primal.puller.PullerDefaults;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.adt.map.ChrObjMap;
import suite.primitive.adt.map.ObjChrMap;
import suite.primitive.adt.set.ChrSet;

public class ChrPuller implements PullerDefaults<Character> {

	private static char empty = ChrPrim.EMPTYVALUE;

	private ChrSource source;

	@SafeVarargs
	public static ChrPuller concat(ChrPuller... outlets) {
		var sources = new ArrayList<ChrSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(ChrFunUtil.concat(Take.from(sources)));
	}

	public static ChrPuller empty() {
		return of(ChrFunUtil.nullSource());
	}

	@SafeVarargs
	public static ChrPuller of(char... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static ChrPuller of(char[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new ChrSource() {
			private int i = start;

			public char g() {
				var c = pred.test(i) ? ts[i] : empty;
				i += inc;
				return c;
			}
		});
	}

	public static ChrPuller of(Enumeration<Character> en) {
		return of(Take.from(en));
	}

	public static ChrPuller of(Iterable<Character> col) {
		return of(Take.from(col));
	}

	public static ChrPuller of(Source<Character> source) {
		return ChrPuller.of(() -> {
			var c = source.g();
			return c != null ? c : empty;
		});
	}

	public static ChrPuller of(ChrSource source) {
		return new ChrPuller(source);
	}

	private ChrPuller(ChrSource source) {
		this.source = source;
	}

	public char average() {
		var count = 0;
		char result = 0, c1;
		while ((c1 = pull()) != empty) {
			result += c1;
			count++;
		}
		return (char) (result / count);
	}

	public Puller<ChrPuller> chunk(int n) {
		return Puller.of(FunUtil.map(ChrPuller::new, ChrFunUtil.chunk(n, source)));
	}

	public ChrPuller closeAtEnd(Closeable c) {
		return of(() -> {
			var next = pull();
			if (next == empty)
				Close.quietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<ChrPuller, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(Chr_Obj<Puller<O>> fun) {
		return Puller.of(FunUtil.concat(ChrFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Puller2<K, V> concatMap2(Chr_Obj<Puller2<K, V>> fun) {
		return Puller2.of(FunUtil2.concat(ChrFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public ChrPuller concatMapChr(Chr_Obj<ChrPuller> fun) {
		return of(ChrFunUtil.concat(ChrFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public ChrPuller cons(char c) {
		return of(ChrFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (pull() != empty)
			i++;
		return i;
	}

	public <U, O> Puller<O> cross(List<U> list, ChrObj_Obj<U, O> fun) {
		return Puller.of(new Source<>() {
			private char c;
			private int index = list.size();

			public O g() {
				if (index == list.size()) {
					index = 0;
					c = pull();
				}
				return fun.apply(c, list.get(index++));
			}
		});
	}

	public ChrPuller distinct() {
		var set = new HashSet<>();
		return of(() -> {
			char c;
			while ((c = pull()) != empty && !set.add(c))
				;
			return c;
		});
	}

	public ChrPuller drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull() != empty))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == ChrPuller.class) {
			var source1 = ((ChrPuller) object).source;
			char o0, o1;
			while (Equals.ab(o0 = source.g(), o1 = source1.g()))
				if (o0 == empty && o1 == empty)
					return true;
			return false;
		} else
			return false;
	}

	public ChrPuller filter(ChrTest fun) {
		return of(ChrFunUtil.filter(fun, source));
	}

	public char first() {
		return pull();
	}

	public <O> Puller<O> flatMap(Chr_Obj<Iterable<O>> fun) {
		return Puller.of(FunUtil.flatten(ChrFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, ChrObj_Obj<R, R> fun) {
		char c;
		while ((c = pull()) != empty)
			init = fun.apply(c, init);
		return init;
	}

	public <V> ChrObjPuller<CharsBuilder> groupBy() {
		return ChrObjPuller.of(toListMap().source());
	}

	public <V> ChrObjPuller<V> groupBy(Fun<Chars, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toChars()));
	}

	@Override
	public int hashCode() {
		var h = 7;
		char c;
		while ((c = source.g()) != empty)
			h = h * 31 + Objects.hashCode(c);
		return h;
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
		char c, c1 = empty;
		while ((c = pull()) != empty)
			c1 = c;
		return c1;
	}

	public <O> Puller<O> map(Chr_Obj<O> fun) {
		return Puller.of(ChrFunUtil.map(fun, source));
	}

	public <K, V> Puller2<K, V> map2(Chr_Obj<K> kf0, Chr_Obj<V> vf0) {
		return Puller2.of(ChrFunUtil.map2(kf0, vf0, source));
	}

	public ChrPuller mapChr(Chr_Chr fun0) {
		return of(ChrFunUtil.mapChr(fun0, source));
	}

	public <V> ChrObjPuller<V> mapChrObj(Chr_Obj<V> fun0) {
		return ChrObjPuller.of(ChrFunUtil.mapChrObj(fun0, source));
	}

	public char max() {
		return min((c0, c1) -> Character.compare(c1, c0));
	}

	public char min() {
		return min((c0, c1) -> Character.compare(c0, c1));
	}

	public char min(ChrComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != empty)
			return c;
		else
			return fail("no result");
	}

	public char minOrEmpty(ChrComparator comparator) {
		char c = pull(), c1;
		if (c != empty) {
			while ((c1 = pull()) != empty)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return empty;
	}

	public ChrPuller nonBlock(char c0) {
		var queue = new NullableSyncQueue<Character>();

		new Thread(() -> {
			char c;
			do
				queue.offerQuietly(c = source.g());
			while (c != empty);
		}).start();

		return new ChrPuller(() -> {
			var mutable = Mutable.<Character> nil();
			var c = queue.poll(mutable) ? mutable.value() : c0;
			return c;
		});
	}

	public ChrOpt opt() {
		var c = pull();
		if (c != empty)
			return pull() == empty ? ChrOpt.of(c) : fail("more than one result");
		else
			return ChrOpt.none();
	}

	public Pair<ChrPuller, ChrPuller> partition(ChrTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public char pull() {
		return source.g();
	}

	public ChrPuller reverse() {
		return of(toList().toChars().reverse());
	}

	public void sink(ChrSink sink0) {
		var sink1 = sink0.rethrow();
		char c;
		while ((c = pull()) != empty)
			sink1.f(c);
	}

	public ChrPuller skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull() == empty;
		return !end ? of(source) : empty();
	}

	public ChrPuller snoc(char c) {
		return of(ChrFunUtil.snoc(c, source));
	}

	public ChrSource source() {
		return source;
	}

	public ChrPuller sort() {
		return of(toList().toChars().sort());
	}

	public Puller<ChrPuller> split(ChrTest fun) {
		return Puller.of(FunUtil.map(ChrPuller::new, ChrFunUtil.split(fun, source)));
	}

	public char sum() {
		char result = 0, c1;
		while ((c1 = pull()) != empty)
			result += c1;
		return result;
	}

	public ChrPuller take(int n) {
		return of(new ChrSource() {
			private int count = n;

			public char g() {
				return 0 < count-- ? pull() : null;
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
		while ((c = pull()) != empty)
			list.append(c);
		return list;
	}

	public <K> ChrObjMap<CharsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> ChrObjMap<CharsBuilder> toListMap(Chr_Chr valueFun) {
		var map = new ChrObjMap<CharsBuilder>();
		char c;
		while ((c = pull()) != empty)
			map.computeIfAbsent(c, k_ -> new CharsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjChrMap<K> toMap(Chr_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjChrMap<K>();
		char c;
		while ((c = pull()) != empty)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Chr_Obj<K> kf0, Chr_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		char c;
		while ((c = pull()) != empty) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				fail("duplicate key " + key);
		}
		return map;
	}

	public ChrSet toSet() {
		var set = new ChrSet();
		char c;
		while ((c = pull()) != empty)
			set.add(c);
		return set;
	}

	public <U, R> Puller<R> zip(Puller<U> outlet1, ChrObj_Obj<U, R> fun) {
		return Puller.of(() -> {
			var t = pull();
			var u = outlet1.pull();
			return t != empty && u != null ? fun.apply(t, u) : null;
		});
	}

}
