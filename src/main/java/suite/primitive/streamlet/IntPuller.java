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
import java.util.Set;
import java.util.function.IntPredicate;

import primal.NullableSyncQueue;
import primal.Verbs.Close;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.FunUtil2;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.primitive.IntOpt;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntComparator;
import primal.primitive.IntPrim.IntObjSource;
import primal.primitive.IntPrim.IntObj_Obj;
import primal.primitive.IntPrim.IntSink;
import primal.primitive.IntPrim.IntSource;
import primal.primitive.IntPrim.IntTest;
import primal.primitive.IntPrim.Int_Int;
import primal.primitive.IntPrim.Int_Obj;
import primal.primitive.adt.pair.IntObjPair;
import primal.primitive.fp.IntFunUtil;
import suite.adt.map.ListMultimap;
import suite.primitive.Ints;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.set.IntSet;
import suite.streamlet.As;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.PullerDefaults;
import suite.streamlet.Read;
import suite.util.To;

public class IntPuller implements PullerDefaults<Integer> {

	private static int empty = IntPrim.EMPTYVALUE;

	private IntSource source;

	@SafeVarargs
	public static IntPuller concat(IntPuller... outlets) {
		var sources = new ArrayList<IntSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(IntFunUtil.concat(To.source(sources)));
	}

	public static IntPuller empty() {
		return of(IntFunUtil.nullSource());
	}

	@SafeVarargs
	public static IntPuller of(int... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static IntPuller of(int[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new IntSource() {
			private int i = start;

			public int g() {
				var c = pred.test(i) ? ts[i] : empty;
				i += inc;
				return c;
			}
		});
	}

	public static IntPuller of(Enumeration<Integer> en) {
		return of(To.source(en));
	}

	public static IntPuller of(Iterable<Integer> col) {
		return of(To.source(col));
	}

	public static IntPuller of(Source<Integer> source) {
		return IntPuller.of(() -> {
			var c = source.g();
			return c != null ? c : empty;
		});
	}

	public static IntPuller of(IntSource source) {
		return new IntPuller(source);
	}

	private IntPuller(IntSource source) {
		this.source = source;
	}

	public int average() {
		var count = 0;
		int result = 0, c1;
		while ((c1 = pull()) != empty) {
			result += c1;
			count++;
		}
		return (int) (result / count);
	}

	public Puller<IntPuller> chunk(int n) {
		return Puller.of(FunUtil.map(IntPuller::new, IntFunUtil.chunk(n, source)));
	}

	public IntPuller closeAtEnd(Closeable c) {
		return of(() -> {
			var next = pull();
			if (next == empty)
				Close.quietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<IntPuller, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(Int_Obj<Puller<O>> fun) {
		return Puller.of(FunUtil.concat(IntFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Puller2<K, V> concatMap2(Int_Obj<Puller2<K, V>> fun) {
		return Puller2.of(FunUtil2.concat(IntFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public IntPuller concatMapInt(Int_Obj<IntPuller> fun) {
		return of(IntFunUtil.concat(IntFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public IntPuller cons(int c) {
		return of(IntFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (pull() != empty)
			i++;
		return i;
	}

	public <U, O> Puller<O> cross(List<U> list, IntObj_Obj<U, O> fun) {
		return Puller.of(new Source<>() {
			private int c;
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

	public IntPuller distinct() {
		var set = new HashSet<>();
		return of(() -> {
			int c;
			while ((c = pull()) != empty && !set.add(c))
				;
			return c;
		});
	}

	public IntPuller drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull() != empty))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == IntPuller.class) {
			var source1 = ((IntPuller) object).source;
			int o0, o1;
			while (Equals.ab(o0 = source.g(), o1 = source1.g()))
				if (o0 == empty && o1 == empty)
					return true;
			return false;
		} else
			return false;
	}

	public IntPuller filter(IntTest fun) {
		return of(IntFunUtil.filter(fun, source));
	}

	public int first() {
		return pull();
	}

	public <O> Puller<O> flatMap(Int_Obj<Iterable<O>> fun) {
		return Puller.of(FunUtil.flatten(IntFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, IntObj_Obj<R, R> fun) {
		int c;
		while ((c = pull()) != empty)
			init = fun.apply(c, init);
		return init;
	}

	public <V> IntObjPuller<IntsBuilder> groupBy() {
		return IntObjPuller.of(toListMap().source());
	}

	public <V> IntObjPuller<V> groupBy(Fun<Ints, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toInts()));
	}

	@Override
	public int hashCode() {
		var h = 7;
		int c;
		while ((c = source.g()) != empty)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public IntObjPuller<Integer> index() {
		return IntObjPuller.of(new IntObjSource<>() {
			private int i = 0;

			public boolean source2(IntObjPair<Integer> pair) {
				var c = pull();
				if (c != empty) {
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
		int c, c1 = empty;
		while ((c = pull()) != empty)
			c1 = c;
		return c1;
	}

	public <O> Puller<O> map(Int_Obj<O> fun) {
		return Puller.of(IntFunUtil.map(fun, source));
	}

	public <K, V> Puller2<K, V> map2(Int_Obj<K> kf0, Int_Obj<V> vf0) {
		return map2_(kf0, vf0);
	}

	public IntPuller mapInt(Int_Int fun0) {
		return of(IntFunUtil.mapInt(fun0, source));
	}

	public <V> IntObjPuller<V> mapIntObj(Int_Obj<V> fun0) {
		return IntObjPuller.of(IntFunUtil.mapIntObj(fun0, source));
	}

	public int max() {
		return min((c0, c1) -> Integer.compare(c1, c0));
	}

	public int min() {
		return min((c0, c1) -> Integer.compare(c0, c1));
	}

	public int min(IntComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != empty)
			return c;
		else
			return fail("no result");
	}

	public int minOrEmpty(IntComparator comparator) {
		int c = pull(), c1;
		if (c != empty) {
			while ((c1 = pull()) != empty)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return empty;
	}

	public IntPuller nonBlock(int c0) {
		var queue = new NullableSyncQueue<Integer>();

		new Thread(() -> {
			int c;
			do
				queue.offerQuietly(c = source.g());
			while (c != empty);
		}).start();

		return new IntPuller(() -> {
			var mutable = Mutable.<Integer> nil();
			var c = queue.poll(mutable) ? mutable.value() : c0;
			return c;
		});
	}

	public IntOpt opt() {
		var c = pull();
		if (c != empty)
			if (pull() == empty)
				return IntOpt.of(c);
			else
				return fail("more than one result");
		else
			return IntOpt.none();
	}

	public Pair<IntPuller, IntPuller> partition(IntTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public int pull() {
		return source.g();
	}

	public IntPuller reverse() {
		return of(toList().toInts().reverse());
	}

	public void sink(IntSink sink0) {
		var sink1 = sink0.rethrow();
		int c;
		while ((c = pull()) != empty)
			sink1.f(c);
	}

	public IntPuller skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull() == empty;
		return !end ? of(source) : empty();
	}

	public IntPuller snoc(int c) {
		return of(IntFunUtil.snoc(c, source));
	}

	public IntSource source() {
		return source;
	}

	public IntPuller sort() {
		return of(toList().toInts().sort());
	}

	public Puller<IntPuller> split(IntTest fun) {
		return Puller.of(FunUtil.map(IntPuller::new, IntFunUtil.split(fun, source)));
	}

	public int sum() {
		int result = 0, c1;
		while ((c1 = pull()) != empty)
			result += c1;
		return result;
	}

	public IntPuller take(int n) {
		return of(new IntSource() {
			private int count = n;

			public int g() {
				return 0 < count-- ? pull() : null;
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
		while ((c = pull()) != empty)
			list.append(c);
		return list;
	}

	public <K> IntObjMap<IntsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> IntObjMap<IntsBuilder> toListMap(Int_Int valueFun) {
		var map = new IntObjMap<IntsBuilder>();
		int c;
		while ((c = pull()) != empty)
			map.computeIfAbsent(c, k_ -> new IntsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjIntMap<K> toMap(Int_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjIntMap<K>();
		int c;
		while ((c = pull()) != empty)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Int_Obj<K> kf0, Int_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		int c;
		while ((c = pull()) != empty) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				fail("duplicate key " + key);
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
		while ((c = pull()) != empty)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public <U, R> Puller<R> zip(Puller<U> outlet1, IntObj_Obj<U, R> fun) {
		return Puller.of(() -> {
			var t = pull();
			var u = outlet1.pull();
			return t != empty && u != null ? fun.apply(t, u) : null;
		});
	}

	private <K, V> Puller2<K, V> map2_(Int_Obj<K> kf0, Int_Obj<V> vf0) {
		return Puller2.of(IntFunUtil.map2(kf0, vf0, source));
	}

}
