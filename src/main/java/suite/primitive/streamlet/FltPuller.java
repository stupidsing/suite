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
import primal.primitive.FltOpt;
import primal.primitive.FltPrim;
import primal.primitive.FltPrim.FltComparator;
import primal.primitive.FltPrim.FltObjSource;
import primal.primitive.FltPrim.FltObj_Obj;
import primal.primitive.FltPrim.FltSink;
import primal.primitive.FltPrim.FltSource;
import primal.primitive.FltPrim.FltTest;
import primal.primitive.FltPrim.Flt_Obj;
import primal.primitive.Flt_Flt;
import primal.primitive.adt.pair.FltObjPair;
import primal.primitive.fp.FltFunUtil;
import suite.adt.map.ListMultimap;
import suite.primitive.Floats;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.adt.map.ObjFltMap;
import suite.primitive.adt.set.FltSet;
import suite.streamlet.As;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.PullerDefaults;
import suite.streamlet.Read;
import suite.util.To;

public class FltPuller implements PullerDefaults<Float> {

	private static float empty = FltPrim.EMPTYVALUE;

	private FltSource source;

	@SafeVarargs
	public static FltPuller concat(FltPuller... outlets) {
		var sources = new ArrayList<FltSource>();
		for (var outlet : outlets)
			sources.add(outlet.source);
		return of(FltFunUtil.concat(To.source(sources)));
	}

	public static FltPuller empty() {
		return of(FltFunUtil.nullSource());
	}

	@SafeVarargs
	public static FltPuller of(float... ts) {
		return of(ts, 0, ts.length, 1);
	}

	public static FltPuller of(float[] ts, int start, int end, int inc) {
		IntPredicate pred = 0 < inc ? i -> i < end : i -> end < i;

		return of(new FltSource() {
			private int i = start;

			public float g() {
				var c = pred.test(i) ? ts[i] : empty;
				i += inc;
				return c;
			}
		});
	}

	public static FltPuller of(Enumeration<Float> en) {
		return of(To.source(en));
	}

	public static FltPuller of(Iterable<Float> col) {
		return of(To.source(col));
	}

	public static FltPuller of(Source<Float> source) {
		return FltPuller.of(() -> {
			var c = source.g();
			return c != null ? c : empty;
		});
	}

	public static FltPuller of(FltSource source) {
		return new FltPuller(source);
	}

	private FltPuller(FltSource source) {
		this.source = source;
	}

	public float average() {
		var count = 0;
		float result = 0, c1;
		while ((c1 = pull()) != empty) {
			result += c1;
			count++;
		}
		return (float) (result / count);
	}

	public Puller<FltPuller> chunk(int n) {
		return Puller.of(FunUtil.map(FltPuller::new, FltFunUtil.chunk(n, source)));
	}

	public FltPuller closeAtEnd(Closeable c) {
		return of(() -> {
			var next = pull();
			if (next == empty)
				Close.quietly(c);
			return next;
		});
	}

	public <R> R collect(Fun<FltPuller, R> fun) {
		return fun.apply(this);
	}

	public <O> Puller<O> concatMap(Flt_Obj<Puller<O>> fun) {
		return Puller.of(FunUtil.concat(FltFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public <K, V> Puller2<K, V> concatMap2(Flt_Obj<Puller2<K, V>> fun) {
		return Puller2.of(FunUtil2.concat(FltFunUtil.map(t -> fun.apply(t).source(), source)));
	}

	public FltPuller concatMapFlt(Flt_Obj<FltPuller> fun) {
		return of(FltFunUtil.concat(FltFunUtil.map(t -> fun.apply(t).source, source)));
	}

	public FltPuller cons(float c) {
		return of(FltFunUtil.cons(c, source));
	}

	public int count() {
		var i = 0;
		while (pull() != empty)
			i++;
		return i;
	}

	public <U, O> Puller<O> cross(List<U> list, FltObj_Obj<U, O> fun) {
		return Puller.of(new Source<>() {
			private float c;
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

	public FltPuller distinct() {
		var set = new HashSet<>();
		return of(() -> {
			float c;
			while ((c = pull()) != empty && !set.add(c))
				;
			return c;
		});
	}

	public FltPuller drop(int n) {
		var isAvailable = true;
		while (0 < n && (isAvailable &= pull() != empty))
			n--;
		return isAvailable ? this : empty();
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == FltPuller.class) {
			var source1 = ((FltPuller) object).source;
			float o0, o1;
			while (Equals.ab(o0 = source.g(), o1 = source1.g()))
				if (o0 == empty && o1 == empty)
					return true;
			return false;
		} else
			return false;
	}

	public FltPuller filter(FltTest fun) {
		return of(FltFunUtil.filter(fun, source));
	}

	public float first() {
		return pull();
	}

	public <O> Puller<O> flatMap(Flt_Obj<Iterable<O>> fun) {
		return Puller.of(FunUtil.flatten(FltFunUtil.map(fun, source)));
	}

	public <R> R fold(R init, FltObj_Obj<R, R> fun) {
		float c;
		while ((c = pull()) != empty)
			init = fun.apply(c, init);
		return init;
	}

	public <V> FltObjPuller<FloatsBuilder> groupBy() {
		return FltObjPuller.of(toListMap().source());
	}

	public <V> FltObjPuller<V> groupBy(Fun<Floats, V> fun) {
		return groupBy().mapValue(list -> fun.apply(list.toFloats()));
	}

	@Override
	public int hashCode() {
		var h = 7;
		float c;
		while ((c = source.g()) != empty)
			h = h * 31 + Objects.hashCode(c);
		return h;
	}

	public FltObjPuller<Integer> index() {
		return FltObjPuller.of(new FltObjSource<>() {
			private int i = 0;

			public boolean source2(FltObjPair<Integer> pair) {
				var c = pull();
				if (c != empty) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		});
	}

	public boolean isAll(FltTest pred) {
		return FltFunUtil.isAll(pred, source);
	}

	public boolean isAny(FltTest pred) {
		return FltFunUtil.isAny(pred, source);
	}

	@Override
	public Iterator<Float> iterator() {
		return FltFunUtil.iterator(source);
	}

	public float last() {
		float c, c1 = empty;
		while ((c = pull()) != empty)
			c1 = c;
		return c1;
	}

	public <O> Puller<O> map(Flt_Obj<O> fun) {
		return Puller.of(FltFunUtil.map(fun, source));
	}

	public <K, V> Puller2<K, V> map2(Flt_Obj<K> kf0, Flt_Obj<V> vf0) {
		return map2_(kf0, vf0);
	}

	public FltPuller mapFlt(Flt_Flt fun0) {
		return of(FltFunUtil.mapFlt(fun0, source));
	}

	public <V> FltObjPuller<V> mapFltObj(Flt_Obj<V> fun0) {
		return FltObjPuller.of(FltFunUtil.mapFltObj(fun0, source));
	}

	public float max() {
		return min((c0, c1) -> Float.compare(c1, c0));
	}

	public float min() {
		return min((c0, c1) -> Float.compare(c0, c1));
	}

	public float min(FltComparator comparator) {
		var c = minOrEmpty(comparator);
		if (c != empty)
			return c;
		else
			return fail("no result");
	}

	public float minOrEmpty(FltComparator comparator) {
		float c = pull(), c1;
		if (c != empty) {
			while ((c1 = pull()) != empty)
				if (0 < comparator.compare(c, c1))
					c = c1;
			return c;
		} else
			return empty;
	}

	public FltPuller nonBlock(float c0) {
		var queue = new NullableSyncQueue<Float>();

		new Thread(() -> {
			float c;
			do
				queue.offerQuietly(c = source.g());
			while (c != empty);
		}).start();

		return new FltPuller(() -> {
			var mutable = Mutable.<Float> nil();
			var c = queue.poll(mutable) ? mutable.value() : c0;
			return c;
		});
	}

	public FltOpt opt() {
		var c = pull();
		if (c != empty)
			if (pull() == empty)
				return FltOpt.of(c);
			else
				return fail("more than one result");
		else
			return FltOpt.none();
	}

	public Pair<FltPuller, FltPuller> partition(FltTest pred) {
		return Pair.of(filter(pred), filter(c -> !pred.test(c)));
	}

	public float pull() {
		return source.g();
	}

	public FltPuller reverse() {
		return of(toList().toFloats().reverse());
	}

	public void sink(FltSink sink0) {
		var sink1 = sink0.rethrow();
		float c;
		while ((c = pull()) != empty)
			sink1.f(c);
	}

	public FltPuller skip(int n) {
		var end = false;
		for (var i = 0; !end && i < n; i++)
			end = pull() == empty;
		return !end ? of(source) : empty();
	}

	public FltPuller snoc(float c) {
		return of(FltFunUtil.snoc(c, source));
	}

	public FltSource source() {
		return source;
	}

	public FltPuller sort() {
		return of(toList().toFloats().sort());
	}

	public Puller<FltPuller> split(FltTest fun) {
		return Puller.of(FunUtil.map(FltPuller::new, FltFunUtil.split(fun, source)));
	}

	public float sum() {
		float result = 0, c1;
		while ((c1 = pull()) != empty)
			result += c1;
		return result;
	}

	public FltPuller take(int n) {
		return of(new FltSource() {
			private int count = n;

			public float g() {
				return 0 < count-- ? pull() : null;
			}
		});
	}

	public float[] toArray() {
		var list = toList();
		return list.toFloats().toArray();
	}

	public FloatsBuilder toList() {
		var list = new FloatsBuilder();
		float c;
		while ((c = pull()) != empty)
			list.append(c);
		return list;
	}

	public <K> FltObjMap<FloatsBuilder> toListMap() {
		return toListMap(value -> value);
	}

	public <K> FltObjMap<FloatsBuilder> toListMap(Flt_Flt valueFun) {
		var map = new FltObjMap<FloatsBuilder>();
		float c;
		while ((c = pull()) != empty)
			map.computeIfAbsent(c, k_ -> new FloatsBuilder()).append(valueFun.apply(c));
		return map;
	}

	public <K> ObjFltMap<K> toMap(Flt_Obj<K> keyFun) {
		var kf1 = keyFun.rethrow();
		var map = new ObjFltMap<K>();
		float c;
		while ((c = pull()) != empty)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Flt_Obj<K> kf0, Flt_Obj<V> vf0) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var map = new HashMap<K, V>();
		float c;
		while ((c = pull()) != empty) {
			var key = kf1.apply(c);
			if (map.put(key, vf1.apply(c)) != null)
				fail("duplicate key " + key);
		}
		return map;
	}

	public <K> ListMultimap<K, Float> toMultimap(Flt_Obj<K> keyFun) {
		return toMultimap(keyFun, value -> value);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public FltSet toSet() {
		var set = new FltSet();
		float c;
		while ((c = pull()) != empty)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return map2_(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public <U, R> Puller<R> zip(Puller<U> outlet1, FltObj_Obj<U, R> fun) {
		return Puller.of(() -> {
			var t = pull();
			var u = outlet1.pull();
			return t != empty && u != null ? fun.apply(t, u) : null;
		});
	}

	private <K, V> Puller2<K, V> map2_(Flt_Obj<K> kf0, Flt_Obj<V> vf0) {
		return Puller2.of(FltFunUtil.map2(kf0, vf0, source));
	}

}
