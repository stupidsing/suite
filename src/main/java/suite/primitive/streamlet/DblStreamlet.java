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
import primal.primitive.DblOpt;
import primal.primitive.DblPrim;
import primal.primitive.DblPrim.DblComparator;
import primal.primitive.DblPrim.DblObjSource;
import primal.primitive.DblPrim.DblObj_Obj;
import primal.primitive.DblPrim.DblPred;
import primal.primitive.DblPrim.DblSink;
import primal.primitive.DblPrim.DblSource;
import primal.primitive.DblPrim.Dbl_Obj;
import primal.primitive.Dbl_Dbl;
import primal.primitive.adt.Doubles;
import primal.primitive.adt.Doubles.DoublesBuilder;
import primal.primitive.adt.pair.DblObjPair;
import primal.primitive.puller.DblObjPuller;
import primal.primitive.puller.DblPuller;
import primal.puller.Puller;
import primal.streamlet.StreamletDefaults;
import suite.adt.map.ListMultimap;
import suite.primitive.Doubles_;
import suite.primitive.adt.map.DblObjMap;
import suite.primitive.adt.map.ObjDblMap;
import suite.primitive.adt.set.DblSet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;

public class DblStreamlet implements StreamletDefaults<Double, DblOpt, DblPred, DblPuller, DblSink, DblSource> {

	private Source<DblPuller> in;

	private static DblStreamlet streamlet(Source<DblPuller> in) {
		return new DblStreamlet(in);
	}

	public DblStreamlet(Source<DblPuller> in) {
		this.in = in;
	}

	public <R> R apply(Fun<DblStreamlet, R> fun) {
		return fun.apply(this);
	}

	public double average() {
		return spawn().average();
	}

	public Streamlet<DblPuller> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public DblStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
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

	public DblStreamlet collect() {
		var doubles = toList_();
		return Doubles_.of(doubles.cs, doubles.start, doubles.end, 1);
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
		return Get.clazz(object) == DblStreamlet.class ? Equals.ab(spawn(), ((DblStreamlet) object).spawn()) : false;
	}

	public DblStreamlet filter(DblPred fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public double first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(Dbl_Obj<Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, DblObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<DblStreamlet, U> fork0, Fun<DblStreamlet, V> fork1, Fun2<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> DblObjStreamlet<DoublesBuilder> groupBy() {
		return new DblObjStreamlet<>(this::groupBy_);
	}

	public <V> DblObjStreamlet<V> groupBy(Fun<Doubles, V> fun) {
		return new DblObjStreamlet<>(() -> groupBy_().mapValue(list -> fun.apply(list.toDoubles())));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public DblObjStreamlet<Integer> index() {
		return new DblObjStreamlet<>(() -> DblObjPuller.of(new DblObjSource<>() {
			private DblPuller puller = spawn();
			private int i = 0;

			public boolean source2(DblObjPair<Integer> pair) {
				var c = puller.pull();
				if (c != DblPrim.EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		}));
	}

	@Override
	public Iterator<Double> iterator() {
		return spawn().iterator();
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

	public double max() {
		return spawn().min((c0, c1) -> Double.compare(c1, c0));
	}

	public double min() {
		return spawn().min((c0, c1) -> Double.compare(c0, c1));
	}

	public double min(DblComparator comparator) {
		return spawn().min(comparator);
	}

	public double minOrEmpty(DblComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public Pair<DblStreamlet, DblStreamlet> partition(DblPred pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public DblPuller puller() {
		return spawn();
	}

	public DblStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public DblStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public DblStreamlet snoc(double c) {
		return streamlet(() -> spawn().snoc(c));
	}

	public DblStreamlet sort() {
		return streamlet(() -> spawn().sort());
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

	public Doubles toList() {
		return toList_();
	}

	public <K> DblObjMap<DoublesBuilder> toListMap() {
		return toListMap_();
	}

	public <K> DblObjMap<DoublesBuilder> toListMap(Dbl_Dbl valueFun) {
		return toListMap_(valueFun);
	}

	public <K> ObjDblMap<K> toMap(Dbl_Obj<K> kf0) {
		var puller = spawn();
		var kf1 = kf0.rethrow();
		var map = new ObjDblMap<K>();
		double c;
		while ((c = puller.pull()) != DblPrim.EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Double> toMultimap(Dbl_Obj<K> keyFun) {
		return toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public DblSet toSet() {
		var puller = spawn();
		var set = new DblSet();
		double c;
		while ((c = puller.pull()) != DblPrim.EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Dbl_Obj<K> keyFun, Dbl_Obj<V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public double uniqueResult() {
		return spawn().opt().get();
	}

	public <U, V> Streamlet<V> zip(Iterable<U> list1, DblObj_Obj<U, V> fun) {
		return new Streamlet<>(() -> spawn().zip(Puller.of(list1), fun));
	}

	private <O> Streamlet<O> concatMap_(Dbl_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).puller()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Dbl_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).puller()));
	}

	private <V> DblObjPuller<DoublesBuilder> groupBy_() {
		return DblObjPuller.of(toListMap_().source());
	}

	private <O> Streamlet<O> map_(Dbl_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Dbl_Obj<K> kf, Dbl_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private Doubles toList_() {
		var list = spawn().toList();
		return Doubles.of(list.cs, 0, list.size);
	}

	private DblObjMap<DoublesBuilder> toListMap_() {
		return toListMap_(value -> value);
	}

	private DblObjMap<DoublesBuilder> toListMap_(Dbl_Dbl valueFun) {
		var puller = spawn();
		var map = new DblObjMap<DoublesBuilder>();
		double c;
		while ((c = puller.pull()) != DblPrim.EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new DoublesBuilder()).append(valueFun.apply(c));
		return map;
	}

	private DblPuller spawn() {
		return in.g();
	}

}
