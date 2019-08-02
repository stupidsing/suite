package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
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
import primal.primitive.puller.FltObjPuller;
import primal.primitive.puller.FltPuller;
import primal.puller.Puller;
import primal.streamlet.StreamletDefaults;
import suite.adt.map.ListMultimap;
import suite.primitive.Floats;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.Floats_;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.adt.map.ObjFltMap;
import suite.primitive.adt.set.FltSet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;

public class FltStreamlet implements StreamletDefaults<Float, FltOpt, FltTest, FltPuller, FltSink, FltSource> {

	private Source<FltPuller> in;

	private static FltStreamlet streamlet(Source<FltPuller> in) {
		return new FltStreamlet(in);
	}

	public FltStreamlet(Source<FltPuller> in) {
		this.in = in;
	}

	public <R> R apply(Fun<FltStreamlet, R> fun) {
		return fun.apply(this);
	}

	public float average() {
		return spawn().average();
	}

	public Streamlet<FltPuller> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public FltStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(Flt_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Flt_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public FltStreamlet cons(float c) {
		return streamlet(() -> spawn().cons(c));
	}

	public FltStreamlet collect() {
		var floats = toList_();
		return Floats_.of(floats.cs, floats.start, floats.end, 1);
	}

	public <U, O> Streamlet<O> cross(Streamlet<U> st1, FltObj_Obj<U, O> fun) {
		return new Streamlet<>(() -> spawn().cross(st1.toList(), fun));
	}

	public FltStreamlet distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public FltStreamlet drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == FltStreamlet.class ? Equals.ab(spawn(), ((FltStreamlet) object).spawn()) : false;
	}

	public FltStreamlet filter(FltTest fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public float first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(Flt_Obj<Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, FltObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<FltStreamlet, U> fork0, Fun<FltStreamlet, V> fork1, Fun2<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> FltObjStreamlet<FloatsBuilder> groupBy() {
		return new FltObjStreamlet<>(this::groupBy_);
	}

	public <V> FltObjStreamlet<V> groupBy(Fun<Floats, V> fun) {
		return new FltObjStreamlet<>(() -> groupBy_().mapValue(list -> fun.apply(list.toFloats())));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public FltObjStreamlet<Integer> index() {
		return new FltObjStreamlet<>(() -> FltObjPuller.of(new FltObjSource<>() {
			private FltPuller puller = spawn();
			private int i = 0;

			public boolean source2(FltObjPair<Integer> pair) {
				var c = puller.pull();
				if (c != FltPrim.EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		}));
	}

	@Override
	public Iterator<Float> iterator() {
		return spawn().iterator();
	}

	public <O> Streamlet2<Float, O> join2(Streamlet<O> streamlet) {
		return concatMap2_(t -> streamlet.map2(v -> t, v -> v));
	}

	public float last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(Flt_Obj<O> fun) {
		return map_(fun);
	}

	public <K, V> Streamlet2<K, V> map2(Flt_Obj<K> kf, Flt_Obj<V> vf) {
		return map2_(kf, vf);
	}

	public FltStreamlet mapFlt(Flt_Flt fun) {
		return streamlet(() -> spawn().mapFlt(fun));
	}

	public <K, V> FltObjStreamlet<V> mapFltObj(Flt_Obj<V> fun0) {
		return new FltObjStreamlet<>(() -> spawn().mapFltObj(fun0));
	}

	public float max() {
		return spawn().min((c0, c1) -> Float.compare(c1, c0));
	}

	public float min() {
		return spawn().min((c0, c1) -> Float.compare(c0, c1));
	}

	public float min(FltComparator comparator) {
		return spawn().min(comparator);
	}

	public float minOrEmpty(FltComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public Pair<FltStreamlet, FltStreamlet> partition(FltTest pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public FltPuller puller() {
		return spawn();
	}

	public FltStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public FltStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public FltStreamlet snoc(float c) {
		return streamlet(() -> spawn().snoc(c));
	}

	public FltStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public float sum() {
		return spawn().sum();
	}

	public FltStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public float[] toArray() {
		return spawn().toArray();
	}

	public Floats toList() {
		return toList_();
	}

	public <K> FltObjMap<FloatsBuilder> toListMap() {
		return toListMap_();
	}

	public <K> FltObjMap<FloatsBuilder> toListMap(Flt_Flt valueFun) {
		return toListMap_(valueFun);
	}

	public <K> ObjFltMap<K> toMap(Flt_Obj<K> kf0) {
		var puller = spawn();
		var kf1 = kf0.rethrow();
		var map = new ObjFltMap<K>();
		float c;
		while ((c = puller.pull()) != FltPrim.EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Float> toMultimap(Flt_Obj<K> keyFun) {
		return toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public FltSet toSet() {
		var puller = spawn();
		var set = new FltSet();
		float c;
		while ((c = puller.pull()) != FltPrim.EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public float uniqueResult() {
		return spawn().opt().get();
	}

	public <U, V> Streamlet<V> zip(Iterable<U> list1, FltObj_Obj<U, V> fun) {
		return new Streamlet<>(() -> spawn().zip(Puller.of(list1), fun));
	}

	private <O> Streamlet<O> concatMap_(Flt_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).puller()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Flt_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).puller()));
	}

	private <V> FltObjPuller<FloatsBuilder> groupBy_() {
		return FltObjPuller.of(toListMap_().source());
	}

	private <O> Streamlet<O> map_(Flt_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Flt_Obj<K> kf, Flt_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private Floats toList_() {
		var list = spawn().toList();
		return Floats.of(list.cs, 0, list.size);
	}

	private FltObjMap<FloatsBuilder> toListMap_() {
		return toListMap_(value -> value);
	}

	private FltObjMap<FloatsBuilder> toListMap_(Flt_Flt valueFun) {
		var puller = spawn();
		var map = new FltObjMap<FloatsBuilder>();
		float c;
		while ((c = puller.pull()) != FltPrim.EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new FloatsBuilder()).append(valueFun.apply(c));
		return map;
	}

	private FltPuller spawn() {
		return in.g();
	}

}
