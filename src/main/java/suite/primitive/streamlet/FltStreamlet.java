package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.Floats;
import suite.primitive.Floats.FloatsBuilder;
import suite.primitive.FltFunUtil;
import suite.primitive.FltOpt;
import suite.primitive.FltPrimitives.FltComparator;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.FltPrimitives.FltPredicate;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.adt.set.FltSet;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
import suite.util.Object_;

public class FltStreamlet implements StreamletDefaults<Float, FltOutlet> {

	private Source<FltOutlet> in;

	@SafeVarargs
	public static FltStreamlet concat(FltStreamlet... streamlets) {
		return Read.from(streamlets).collect(FltStreamlet::concat);
	}

	public static FltStreamlet concat(Outlet<FltStreamlet> streamlets) {
		return streamlet(() -> {
			Source<FltStreamlet> source = streamlets.source();
			return FltOutlet.of(FltFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	public static <T> Fun<Outlet<T>, FltStreamlet> of(Obj_Flt<T> fun0) {
		Obj_Flt<T> fun1 = fun0.rethrow();
		return ts -> {
			FloatsBuilder cb = new FloatsBuilder();
			T t;
			while ((t = ts.next()) != null)
				cb.append(fun1.apply(t));
			return cb.toFloats().streamlet();
		};
	}

	public static FltStreamlet of(float[] ts) {
		return streamlet(() -> FltOutlet.of(ts));
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

	public FltStreamlet append(float c) {
		return streamlet(() -> spawn().append(c));
	}

	public <R> R apply(Fun<FltStreamlet, R> fun) {
		return fun.apply(this);
	}

	public float average() {
		return spawn().average();
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

	public <O> Streamlet<O> concatMap(Flt_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Flt_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public FltStreamlet cons(float c) {
		return streamlet(() -> spawn().cons(c));
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

	public <U, V, W> W forkJoin(Fun<FltStreamlet, U> fork0, Fun<FltStreamlet, V> fork1, Fun2<U, V, W> join) {
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
		return streamlet(() -> spawn().mapFlt(fun));
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

	public float max() {
		return spawn().max();
	}

	public float min() {
		return spawn().min();
	}

	public float min(FltComparator comparator) {
		return spawn().min(comparator);
	}

	public float minOrEmpty(FltComparator comparator) {
		return spawn().minOrEmpty(comparator);
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

	public FltStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public FltStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public FltSource source() {
		return spawn().source();
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

	public FltSet toSet() {
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
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).outlet()));
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
