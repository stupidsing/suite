package suite.primitive.streamlet;

import static primal.statics.Fail.fail;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Pair;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Sink2;
import primal.primitive.FltObj_Flt;
import primal.primitive.FltPrim;
import primal.primitive.FltPrim.FltObjPredicate;
import primal.primitive.FltPrim.FltObjSource;
import primal.primitive.FltPrim.FltObj_Obj;
import primal.primitive.FltPrim.FltTest;
import primal.primitive.adt.pair.FltObjPair;
import primal.primitive.fp.FltObjFunUtil;
import primal.puller.Puller;
import primal.puller.Puller2;
import primal.streamlet.StreamletDefaults;
import suite.adt.map.ListMultimap;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.adt.map.ObjFltMap;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;

public class FltObjStreamlet<V> implements StreamletDefaults<FltObjPair<V>, FltObjPuller<V>> {

	private Source<FltObjPuller<V>> in;

	@SafeVarargs
	public static <V> FltObjStreamlet<V> concat(FltObjStreamlet<V>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return FltObjPuller.of(FltObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> FltObjStreamlet<V> streamlet(Source<FltObjPuller<V>> in) {
		return new FltObjStreamlet<>(in);
	}

	public FltObjStreamlet(Source<FltObjPuller<V>> in) {
		this.in = in;
	}

	public <R> R apply(Fun<FltObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<FltObjPuller<V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public FltObjStreamlet<V> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(FltObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(FltObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> FltObjStreamlet<V1> concatMapFltObj(FltObj_Obj<V, FltObjStreamlet<V1>> fun) {
		return concatMapFltObj_(fun);
	}

	public <V1> FltObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Puller<V1>> f = v -> fun.apply(v).puller();
		return streamlet(() -> FltObjPuller.of(spawn().concatMapValue(f)));
	}

	public FltObjStreamlet<V> cons(float key, V value) {
		return streamlet(() -> spawn().cons(key, value));
	}

	public FltObjStreamlet<V> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public FltObjStreamlet<V> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == FltObjStreamlet.class ? Equals.ab(spawn(), ((FltObjStreamlet<?>) object).spawn())
				: false;
	}

	public FltObjStreamlet<V> filter(FltObjPredicate<V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public FltObjStreamlet<V> filterKey(FltTest fun) {
		return streamlet(() -> spawn().filterKey(fun));
	}

	public FltObjStreamlet<V> filterValue(Predicate<V> fun) {
		return streamlet(() -> spawn().filterValue(fun));
	}

	public FltObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(FltObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public FltObjStreamlet<List<V>> groupBy() {
		return streamlet(this::groupBy_);
	}

	public <V1> FltObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return streamlet(() -> groupBy_().mapValue(list -> fun.apply(Read.from(list))));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(FltObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(FltObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	@Override
	public Iterator<FltObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public FltStreamlet keys() {
		return new FltStreamlet(() -> spawn().keys());
	}

	public FltObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(FltObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Float, V1> map2(FltObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(FltObj_Obj<V, K1> kf, FltObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> FltObjStreamlet<V1> mapFltObj(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return mapFltObj_(kf, vf);
	}

	public <V1> FltObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return streamlet(() -> spawn().mapValue(fun));
	}

	public FltObjPair<V> min(Comparator<FltObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public FltObjPair<V> minOrNull(Comparator<FltObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public FltObjPair<V> opt() {
		return spawn().opt();
	}

	public Streamlet<FltObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<FltObjStreamlet<V>, FltObjStreamlet<V>> partition(FltObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public FltObjPuller<V> puller() {
		return spawn();
	}

	public FltObjStreamlet<V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(Sink2<Float, V> sink) {
		spawn().sink(sink);
	}

	public FltObjStreamlet<V> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public FltObjStreamlet<V> snoc(float key, V value) {
		return streamlet(() -> spawn().snoc(key, value));
	}

	public FltObjStreamlet<V> sort(Comparator<FltObjPair<V>> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> FltObjStreamlet<V> sortBy(FltObj_Obj<V, O> fun) {
		return streamlet(() -> spawn().sortBy(fun));
	}

	public FltObjStreamlet<V> sortByKey(Comparator<Float> comparator) {
		return streamlet(() -> spawn().sortByKey(comparator));
	}

	public FltObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return streamlet(() -> spawn().sortByValue(comparator));
	}

	public FltObjSource<V> source() {
		return spawn().source();
	}

	public FltObjStreamlet<V> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public FltObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<FltObjPair<V>> toList() {
		return toList_();
	}

	public FltObjMap<List<V>> toListMap() {
		return toListMap_();
	}

	public FltObjMap<V> toMap() {
		var source = spawn().source();
		var map = new FltObjMap<V>();
		var pair = FltObjPair.of(FltPrim.EMPTYVALUE, (V) null);
		while (source.source2(pair))
			map.put(pair.k, pair.v);
		return map;
	}

	public ListMultimap<Float, V> toMultimap() {
		var map = new ListMultimap<Float, V>();
		groupBy_().concatMapValue(Puller::of).sink(map::put);
		return map;
	}

	public ObjFltMap<V> toObjFltMap() {
		var source = spawn().source();
		var pair = FltObjPair.of(FltPrim.EMPTYVALUE, (V) null);
		var map = new ObjFltMap<V>();
		while (source.source2(pair))
			map.put(pair.v, pair.k);
		return map;
	}

	public Set<FltObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public FltObjPair<V> uniqueResult() {
		var pair = spawn().opt();
		return pair.k != FltPrim.EMPTYVALUE ? pair : fail("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(FltObj_Obj<V, Streamlet<T>> fun) {
		FltObj_Obj<V, Puller<T>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet<>(() -> Puller.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(FltObj_Obj<V, Streamlet2<K1, V1>> fun) {
		FltObj_Obj<V, Puller2<K1, V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet2<>(() -> Puller2.of(spawn().concatMap2(bf)));
	}

	private <V1> FltObjStreamlet<V1> concatMapFltObj_(FltObj_Obj<V, FltObjStreamlet<V1>> fun) {
		FltObj_Obj<V, FltObjPuller<V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return streamlet(() -> FltObjPuller.of(spawn().concatMapFltObj(bf)));
	}

	private FltObjPuller<List<V>> groupBy_() {
		return FltObjPuller.of(toListMap_().source());
	}

	private <T> Streamlet<T> map_(FltObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(FltObj_Obj<V, K1> kf, FltObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> FltObjStreamlet<V1> mapFltObj_(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return streamlet(() -> spawn().mapFltObj(kf, vf));
	}

	private List<FltObjPair<V>> toList_() {
		return spawn().toList();
	}

	private FltObjMap<List<V>> toListMap_() {
		var source = spawn().source();
		var map = new FltObjMap<List<V>>();
		var pair = FltObjPair.of(FltPrim.EMPTYVALUE, (V) null);
		while (source.source2(pair))
			map.computeIfAbsent(pair.k, k_ -> new ArrayList<>()).add(pair.v);
		return map;
	}

	private FltObjPuller<V> spawn() {
		return in.g();
	}

}
