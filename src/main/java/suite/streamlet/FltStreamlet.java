package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.Floats;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltFunUtil;
import suite.primitive.FltMutable;
import suite.primitive.FltOpt;
import suite.primitive.FltPrimitives.FltComparator;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.FltPrimitives.FltPredicate;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.primitive.Flt_Flt;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.primitive.adt.map.FltObjMap;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class FltStreamlet implements Iterable<Float> {

	private Source<FltOutlet> in;

	@SafeVarargs
	public static FltStreamlet concat(FltStreamlet... streamlets) {
		return streamlet(() -> {
			List<FltSource> sources = new ArrayList<>();
			for (FltStreamlet streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return FltOutlet.of(FltFunUtil.concat(To.source(sources)));
		});
	}

	public static FltStreamlet range(float e) {
		return range((float) 0, e);
	}

	public static FltStreamlet range(float s, float e) {
		return streamlet(() -> {
			FltMutable m = FltMutable.of(s);
			return FltOutlet.of(() -> {
				float c = m.increment();
				return c < e ? c : FltFunUtil.EMPTYVALUE;
			});
		});
	}

	private static FltStreamlet streamlet(Source<FltOutlet> in) {
		return new FltStreamlet(in);
	}

	public FltStreamlet(Source<FltOutlet> in) {
		this.in = in;
	}

	@Override
	public Iterator<Float> iterator() {
		return spawn().iterator();
	}

	public FltStreamlet append(float t) {
		return streamlet(() -> spawn().append(t));
	}

	public Streamlet<FltOutlet> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public FltStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			FltOutlet in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<FltOutlet, R> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(Flt_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Flt_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public FltStreamlet cons(float t) {
		return streamlet(() -> spawn().cons(t));
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
		return Object_.clazz(object) == FltStreamlet.class ? Objects.equals(spawn(), ((FltStreamlet) object).spawn()) : false;
	}

	public FltStreamlet filter(FltPredicate fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public float first() {
		return spawn().first();
	}

	public FltStreamlet flatMap(Flt_Obj<Iterable<Float>> fun) {
		return streamlet(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, FltObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<FltStreamlet, U> fork0, Fun<FltStreamlet, V> fork1, ObjObj_Obj<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> FltObjStreamlet<FloatsBuilder> groupBy() {
		return new FltObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V> FltObjStreamlet<V> groupBy(Fun<Floats, V> fun) {
		return new FltObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public FltObjStreamlet<Integer> index() {
		return new FltObjStreamlet<>(() -> spawn().index());
	}

	public boolean isAll(FltPredicate pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(FltPredicate pred) {
		return spawn().isAny(pred);
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
		return new FltStreamlet(() -> spawn().mapFlt(fun));
	}

	public <K, V> FltObjStreamlet<V> mapFltObj(Flt_Obj<V> fun0) {
		return new FltObjStreamlet<>(() -> spawn().mapFltObj(fun0));
	}

	public <O> Streamlet<O> mapNonNull(Flt_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public FltStreamlet memoize() {
		Floats list = toList().toFloats();
		return streamlet(() -> FltOutlet.of(list));
	}

	public float min(FltComparator comparator) {
		return spawn().min(comparator);
	}

	public float minOrNull(FltComparator comparator) {
		return spawn().minOrNull(comparator);
	}

	public FltOpt opt() {
		return spawn().opt();
	}

	public FltOutlet outlet() {
		return spawn();
	}

	public Pair<FltStreamlet, FltStreamlet> partition(FltPredicate pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public FltStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(FltSink sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().count();
	}

	public FltStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public FltStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public FltSource source() {
		return spawn().source();
	}

	public FltStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public float[] toArray() {
		return spawn().toArray();
	}

	public FloatsBuilder toList() {
		return spawn().toList();
	}

	public <K> FltObjMap<FloatsBuilder> toListMap() {
		return spawn().toListMap();
	}

	public <K> FltObjMap<FloatsBuilder> toListMap(Flt_Flt valueFun) {
		return spawn().toListMap(valueFun);
	}

	public <K> Map<K, Float> toMap(Flt_Obj<K> keyFun) {
		return spawn().toMap(keyFun);
	}

	public <K, V> Map<K, V> toMap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Float> toMultimap(Flt_Obj<K> keyFun) {
		return spawn().toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return spawn().toMultimap(keyFun, valueFun);
	}

	public Set<Float> toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Flt_Obj<K> keyFun, Flt_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public float uniqueResult() {
		return spawn().opt().get();
	}

	private <O> Streamlet<O> concatMap_(Flt_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).outlet()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Flt_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).out()));
	}

	private <O> Streamlet<O> map_(Flt_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Flt_Obj<K> kf, Flt_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private FltOutlet spawn() {
		return in.source();
	}

}
