package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.DblFunUtil;
import suite.primitive.DblOpt;
import suite.primitive.DblPrimitives.DblComparator;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.DblPrimitives.DblPredicate;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.primitive.Doubles;
import suite.primitive.Doubles.DoublesBuilder;
import suite.primitive.adt.map.DblObjMap;
import suite.primitive.adt.set.DblSet;
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

public class DblStreamlet implements StreamletDefaults<Double, DblOutlet> {

	private Source<DblOutlet> in;

	@SafeVarargs
	public static DblStreamlet concat(DblStreamlet... streamlets) {
		return Read.from(streamlets).collect(DblStreamlet::concat);
	}

	public static DblStreamlet concat(Outlet<DblStreamlet> streamlets) {
		return streamlet(() -> {
			Source<DblStreamlet> source = streamlets.source();
			return DblOutlet.of(DblFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	public static <T> Fun<Outlet<T>, DblStreamlet> of(Obj_Dbl<T> fun0) {
		Obj_Dbl<T> fun1 = fun0.rethrow();
		return ts -> {
			DoublesBuilder cb = new DoublesBuilder();
			T t;
			while ((t = ts.next()) != null)
				cb.append(fun1.apply(t));
			return cb.toDoubles().streamlet();
		};
	}

	public static DblStreamlet of(double[] ts) {
		return streamlet(() -> DblOutlet.of(ts));
	}

	private static DblStreamlet streamlet(Source<DblOutlet> in) {
		return new DblStreamlet(in);
	}

	public DblStreamlet(Source<DblOutlet> in) {
		this.in = in;
	}

	@Override
	public Iterator<Double> iterator() {
		return spawn().iterator();
	}

	public DblStreamlet append(double c) {
		return streamlet(() -> spawn().append(c));
	}

	public <R> R apply(Fun<DblStreamlet, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<DblOutlet> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public DblStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			DblOutlet in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(Dbl_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Dbl_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public DblStreamlet cons(double c) {
		return streamlet(() -> spawn().cons(c));
	}

	public <U, O> Streamlet<O> cross(Streamlet<U> st1, DblObj_Obj<U, O> fun) {
		return new Streamlet<>(() -> spawn().cross(st1.toList(), fun));
	}

	public DblStreamlet distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public DblStreamlet drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == DblStreamlet.class ? Objects.equals(spawn(), ((DblStreamlet) object).spawn()) : false;
	}

	public DblStreamlet filter(DblPredicate fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public double first() {
		return spawn().first();
	}

	public DblStreamlet flatMap(Dbl_Obj<Iterable<Double>> fun) {
		return streamlet(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, DblObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<DblStreamlet, U> fork0, Fun<DblStreamlet, V> fork1, Fun2<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> DblObjStreamlet<DoublesBuilder> groupBy() {
		return new DblObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V> DblObjStreamlet<V> groupBy(Fun<Doubles, V> fun) {
		return new DblObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public DblObjStreamlet<Integer> index() {
		return new DblObjStreamlet<>(() -> spawn().index());
	}

	public boolean isAll(DblPredicate pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(DblPredicate pred) {
		return spawn().isAny(pred);
	}

	public <O> Streamlet2<Double, O> join2(Streamlet<O> streamlet) {
		return concatMap2_(t -> streamlet.map2(v -> t, v -> v));
	}

	public double last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(Dbl_Obj<O> fun) {
		return map_(fun);
	}

	public <K, V> Streamlet2<K, V> map2(Dbl_Obj<K> kf, Dbl_Obj<V> vf) {
		return map2_(kf, vf);
	}

	public DblStreamlet mapDbl(Dbl_Dbl fun) {
		return streamlet(() -> spawn().mapDbl(fun));
	}

	public <K, V> DblObjStreamlet<V> mapDblObj(Dbl_Obj<V> fun0) {
		return new DblObjStreamlet<>(() -> spawn().mapDblObj(fun0));
	}

	public <O> Streamlet<O> mapNonNull(Dbl_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public DblStreamlet memoize() {
		Doubles list = toList().toDoubles();
		return streamlet(() -> DblOutlet.of(list));
	}

	public double max() {
		return spawn().max();
	}

	public double min() {
		return spawn().min();
	}

	public double min(DblComparator comparator) {
		return spawn().min(comparator);
	}

	public double minOrEmpty(DblComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public DblOpt opt() {
		return spawn().opt();
	}

	public DblOutlet outlet() {
		return spawn();
	}

	public Pair<DblStreamlet, DblStreamlet> partition(DblPredicate pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public DblStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(DblSink sink) {
		spawn().sink(sink);
	}

	public DblStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public DblStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public DblSource source() {
		return spawn().source();
	}

	public double sum() {
		return spawn().sum();
	}

	public DblStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public double[] toArray() {
		return spawn().toArray();
	}

	public DoublesBuilder toList() {
		return spawn().toList();
	}

	public <K> DblObjMap<DoublesBuilder> toListMap() {
		return spawn().toListMap();
	}

	public <K> DblObjMap<DoublesBuilder> toListMap(Dbl_Dbl valueFun) {
		return spawn().toListMap(valueFun);
	}

	public <K> Map<K, Double> toMap(Dbl_Obj<K> keyFun) {
		return spawn().toMap(keyFun);
	}

	public <K, V> Map<K, V> toMap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Double> toMultimap(Dbl_Obj<K> keyFun) {
		return spawn().toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return spawn().toMultimap(keyFun, valueFun);
	}

	public DblSet toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public double uniqueResult() {
		return spawn().opt().get();
	}

	private <O> Streamlet<O> concatMap_(Dbl_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).outlet()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Dbl_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).outlet()));
	}

	private <O> Streamlet<O> map_(Dbl_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Dbl_Obj<K> kf, Dbl_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private DblOutlet spawn() {
		return in.source();
	}

}
