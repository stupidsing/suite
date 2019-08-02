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
import primal.primitive.LngOpt;
import primal.primitive.LngPrim;
import primal.primitive.LngPrim.LngComparator;
import primal.primitive.LngPrim.LngObj_Obj;
import primal.primitive.LngPrim.LngSink;
import primal.primitive.LngPrim.LngSource;
import primal.primitive.LngPrim.LngTest;
import primal.primitive.LngPrim.Lng_Obj;
import primal.primitive.Lng_Lng;
import primal.primitive.fp.LngFunUtil;
import primal.puller.Puller;
import primal.puller.Puller2;
import primal.puller.PullerDefaults;
import suite.primitive.Longs;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.adt.map.LngObjMap;
import suite.primitive.adt.map.ObjLngMap;
import suite.primitive.adt.set.LngSet;

public class LngPuller implements PullerDefaults<Long> {

	private static long empty = LngPrim.EMPTYVALUE;

	private LngSource source;

	@SafeVarargs
	public static LngPuller concat(LngPuller... outlets) {
		var sources = new ArrayList<LngSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(LngFunUtil.concat(Take.from(sources)));
	}

	public static LngPuller empty() {
		return of(LngFunUtil.nullSource());
	}

	@SafeVarargs
	public static LngPuller of(long... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static LngPuller of(long[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new LngSource() {
			private int i = start;

			public long g() {
				var c = pred.test(i) ? ts[i] : empty;
				i += inc;
				return c;
			}
		});
	}

	public static LngPuller of(Enumeration<Long> en) {
		return of(Take.from(en));
	}

	public static LngPuller of(Iterable<Long> col) {
		return of(Take.from(col));
	}

	public static LngPuller of(Source<Long> source) {
		return LngPuller.of(() -> {
			var c = source.g();
			return c != null ? c : empty;
		});
	}

	public static LngPuller of(LngSource source) {
		return new LngPuller(source);
	}

	private LngPuller(LngSource source) {
		this.source = source;
	}

	public long average() {
		var count = 0;
		long result = 0, c1;
		while ((c1 = pull()) != empty) {
			result += c1;
			count++;
		}
		return (long) (result / count);
	}

	public Puller<LngPuller> chunk(int n) {
		return Puller.of(FunUtil.map(LngPuller::new, LngFunUtil.chunk(n, source)));
	}

	public LngPuller closeAtEnd(Closeable c) {
		return of(() -> {
			var next = pull();
			if (next == empty)
				Close.quietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<LngPuller, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(Lng_Obj<Puller<O>> fun) {
		return Puller.of(FunUtil.concat(LngFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Puller2<K, V> concatMap2(Lng_Obj<Puller2<K, V>> fun) {
		return Puller2.of(FunUtil2.concat(LngFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public LngPuller concatMapLng(Lng_Obj<LngPuller> fun) {
		return of(LngFunUtil.concat(LngFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public LngPuller cons(long c) {
		return of(LngFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (pull() != empty)
			i++;
		return i;
	}

	public <U, O> Puller<O> cross(List<U> list, LngObj_Obj<U, O> fun) {
		return Puller.of(new Source<>() {
			private long c;
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

	public LngPuller distinct() {
		var set = new HashSet<>();
		return of(() -> {
			long c;
			while ((c = pull()) != empty && !set.add(c))
				;
			return c;
		});
	}

	public LngPuller drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull() != empty))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == LngPuller.class) {
			var source1 = ((LngPuller) object).source;
			long o0, o1;
			while (Equals.ab(o0 = source.g(), o1 = source1.g()))
				if (o0 == empty && o1 == empty)
					return true;
			return false;
		} else
			return false;
	}

	public LngPuller filter(LngTest fun) {
		return of(LngFunUtil.filter(fun, source));
	}

	public long first() {
		return pull();
	}

	public <O> Puller<O> flatMap(Lng_Obj<Iterable<O>> fun) {
		return Puller.of(FunUtil.flatten(LngFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, LngObj_Obj<R, R> fun) {
		long c;
		while ((c = pull()) != empty)
			init = fun.apply(c, init);
		return init;
	}

	public <V> LngObjPuller<LongsBuilder> groupBy() {
		return LngObjPuller.of(toListMap().source());
	}

	public <V> LngObjPuller<V> groupBy(Fun<Longs, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toLongs()));
	}

	@Override
	public int hashCode() {
		var h = 7;
		long c;
		while ((c = source.g()) != empty)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public boolean isAll(LngTest pred) {
		return LngFunUtil.isAll(pred, source);
	}

	public boolean isAny(LngTest pred) {
		return LngFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<Long> iterator() {
		return LngFunUtil.iterator(source);
	}

	public long last() {
		long c, c1 = empty;
		while ((c = pull()) != empty)
			c1 = c;
		return c1;
	}

	public <O> Puller<O> map(Lng_Obj<O> fun) {
		return Puller.of(LngFunUtil.map(fun, source));
	}

	public <K, V> Puller2<K, V> map2(Lng_Obj<K> kf0, Lng_Obj<V> vf0) {
		return Puller2.of(LngFunUtil.map2(kf0, vf0, source));
	}

	public LngPuller mapLng(Lng_Lng fun0) {
		return of(LngFunUtil.mapLng(fun0, source));
	}

	public <V> LngObjPuller<V> mapLngObj(Lng_Obj<V> fun0) {
		return LngObjPuller.of(LngFunUtil.mapLngObj(fun0, source));
	}

	public long max() {
		return min((c0, c1) -> Long.compare(c1, c0));
	}

	public long min() {
		return min((c0, c1) -> Long.compare(c0, c1));
	}

	public long min(LngComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != empty)
			return c;
		else
			return fail("no result");
	}

	public long minOrEmpty(LngComparator comparator) {
		long c = pull(), c1;
		if (c != empty) {
			while ((c1 = pull()) != empty)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return empty;
	}

	public LngPuller nonBlock(long c0) {
		var queue = new NullableSyncQueue<Long>();

		new Thread(() -> {
			long c;
			do
				queue.offerQuietly(c = source.g());
			while (c != empty);
		}).start();

		return new LngPuller(() -> {
			var mutable = Mutable.<Long> nil();
			var c = queue.poll(mutable) ? mutable.value() : c0;
			return c;
		});
	}

	public LngOpt opt() {
		var c = pull();
		if (c != empty)
			return pull() == empty ? LngOpt.of(c) : fail("more than one result");
		else
			return LngOpt.none();
	}

	public Pair<LngPuller, LngPuller> partition(LngTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public long pull() {
		return source.g();
	}

	public LngPuller reverse() {
		return of(toList().toLongs().reverse());
	}

	public void sink(LngSink sink0) {
		var sink1 = sink0.rethrow();
		long c;
		while ((c = pull()) != empty)
			sink1.f(c);
	}

	public LngPuller skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull() == empty;
		return !end ? of(source) : empty();
	}

	public LngPuller snoc(long c) {
		return of(LngFunUtil.snoc(c, source));
	}

	public LngSource source() {
		return source;
	}

	public LngPuller sort() {
		return of(toList().toLongs().sort());
	}

	public Puller<LngPuller> split(LngTest fun) {
		return Puller.of(FunUtil.map(LngPuller::new, LngFunUtil.split(fun, source)));
	}

	public long sum() {
		long result = 0, c1;
		while ((c1 = pull()) != empty)
			result += c1;
		return result;
	}

	public LngPuller take(int n) {
		return of(new LngSource() {
			private int count = n;

			public long g() {
				return 0 < count-- ? pull() : null;
			}
		});
	}

	public long[] toArray() {
		var list = toList();
		return list.toLongs().toArray();
	}

	public LongsBuilder toList() {
		var list = new LongsBuilder();
		long c;
		while ((c = pull()) != empty)
			list.append(c);
		return list;
	}

	public <K> LngObjMap<LongsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> LngObjMap<LongsBuilder> toListMap(Lng_Lng valueFun) {
		var map = new LngObjMap<LongsBuilder>();
		long c;
		while ((c = pull()) != empty)
			map.computeIfAbsent(c, k_ -> new LongsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjLngMap<K> toMap(Lng_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjLngMap<K>();
		long c;
		while ((c = pull()) != empty)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Lng_Obj<K> kf0, Lng_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		long c;
		while ((c = pull()) != empty) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				fail("duplicate key " + key);
		}
		return map;
	}

	public LngSet toSet() {
		var set = new LngSet();
		long c;
		while ((c = pull()) != empty)
			set.add(c);
		return set;
	}

	public <U, R> Puller<R> zip(Puller<U> outlet1, LngObj_Obj<U, R> fun) {
		return Puller.of(() -> {
			var t = pull();
			var u = outlet1.pull();
			return t != empty && u != null ? fun.apply(t, u) : null;
		});
	}

}
