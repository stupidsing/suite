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
import primal.adt.map.ListMultimap;
import primal.fp.FunUtil;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.primitive.ChrObj_Chr;
import primal.primitive.ChrPrim;
import primal.primitive.ChrPrim.ChrObjPredicate;
import primal.primitive.ChrPrim.ChrObjSink;
import primal.primitive.ChrPrim.ChrObjSource;
import primal.primitive.ChrPrim.ChrObj_Obj;
import primal.primitive.ChrPrim.ChrPred;
import primal.primitive.adt.map.ChrObjMap;
import primal.primitive.adt.map.ObjChrMap;
import primal.primitive.adt.pair.ChrObjPair;
import primal.primitive.fp.ChrObjFunUtil;
import primal.primitive.puller.ChrObjPuller;
import primal.puller.Puller;
import primal.puller.Puller2;
import primal.streamlet.StreamletDefaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;

public class ChrObjStreamlet<V> implements StreamletDefaults<ChrObjPair<V>, ChrObjPair<V>, ChrObjPredicate<V>, ChrObjPuller<V>, ChrObjSink<V>, ChrObjSource<V>> {

	private Source<ChrObjPuller<V>> in;

	@SafeVarargs
	public static <V> ChrObjStreamlet<V> concat(ChrObjStreamlet<V>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return ChrObjPuller.of(ChrObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> ChrObjStreamlet<V> streamlet(Source<ChrObjPuller<V>> in) {
		return new ChrObjStreamlet<>(in);
	}

	public ChrObjStreamlet(Source<ChrObjPuller<V>> in) {
		this.in = in;
	}

	public <R> R apply(Fun<ChrObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<ChrObjPuller<V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public ChrObjStreamlet<V> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(ChrObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(ChrObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> ChrObjStreamlet<V1> concatMapChrObj(ChrObj_Obj<V, ChrObjStreamlet<V1>> fun) {
		return concatMapChrObj_(fun);
	}

	public <V1> ChrObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Puller<V1>> f = v -> fun.apply(v).puller();
		return streamlet(() -> ChrObjPuller.of(spawn().concatMapValue(f)));
	}

	public ChrObjStreamlet<V> cons(char key, V value) {
		return streamlet(() -> spawn().cons(key, value));
	}

	public ChrObjStreamlet<V> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public ChrObjStreamlet<V> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == ChrObjStreamlet.class ? Equals.ab(spawn(), ((ChrObjStreamlet<?>) object).spawn())
				: false;
	}

	public ChrObjStreamlet<V> filter(ChrObjPredicate<V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public ChrObjStreamlet<V> filterKey(ChrPred fun) {
		return streamlet(() -> spawn().filterKey(fun));
	}

	public ChrObjStreamlet<V> filterValue(Predicate<V> fun) {
		return streamlet(() -> spawn().filterValue(fun));
	}

	public ChrObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(ChrObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public ChrObjStreamlet<List<V>> groupBy() {
		return streamlet(this::groupBy_);
	}

	public <V1> ChrObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return streamlet(() -> groupBy_().mapValue(list -> fun.apply(Read.from(list))));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	@Override
	public Iterator<ChrObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public ChrStreamlet keys() {
		return new ChrStreamlet(() -> spawn().keys());
	}

	public ChrObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(ChrObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Character, V1> map2(ChrObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(ChrObj_Obj<V, K1> kf, ChrObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> ChrObjStreamlet<V1> mapChrObj(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return mapChrObj_(kf, vf);
	}

	public <V1> ChrObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return streamlet(() -> spawn().mapValue(fun));
	}

	public ChrObjPair<V> min(Comparator<ChrObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public ChrObjPair<V> minOrNull(Comparator<ChrObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Streamlet<ChrObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<ChrObjStreamlet<V>, ChrObjStreamlet<V>> partition(ChrObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public ChrObjPuller<V> puller() {
		return spawn();
	}

	public ChrObjStreamlet<V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public ChrObjStreamlet<V> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public ChrObjStreamlet<V> snoc(char key, V value) {
		return streamlet(() -> spawn().snoc(key, value));
	}

	public ChrObjStreamlet<V> sort(Comparator<ChrObjPair<V>> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> ChrObjStreamlet<V> sortBy(ChrObj_Obj<V, O> fun) {
		return streamlet(() -> spawn().sortBy(fun));
	}

	public ChrObjStreamlet<V> sortByKey(Comparator<Character> comparator) {
		return streamlet(() -> spawn().sortByKey(comparator));
	}

	public ChrObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return streamlet(() -> spawn().sortByValue(comparator));
	}

	public ChrObjStreamlet<V> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public ChrObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<ChrObjPair<V>> toList() {
		return toList_();
	}

	public ChrObjMap<List<V>> toListMap() {
		return toListMap_();
	}

	public ChrObjMap<V> toMap() {
		var source = spawn().source();
		var map = new ChrObjMap<V>();
		var pair = ChrObjPair.of(ChrPrim.EMPTYVALUE, (V) null);
		while (source.source2(pair))
			map.put(pair.k, pair.v);
		return map;
	}

	public ListMultimap<Character, V> toMultimap() {
		var map = new ListMultimap<Character, V>();
		groupBy_().concatMapValue(Puller::of).sink(map::put);
		return map;
	}

	public ObjChrMap<V> toObjChrMap() {
		var source = spawn().source();
		var pair = ChrObjPair.of(ChrPrim.EMPTYVALUE, (V) null);
		var map = new ObjChrMap<V>();
		while (source.source2(pair))
			map.put(pair.v, pair.k);
		return map;
	}

	public Set<ChrObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public ChrObjPair<V> uniqueResult() {
		var pair = spawn().opt();
		return pair.k != ChrPrim.EMPTYVALUE ? pair : fail("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(ChrObj_Obj<V, Streamlet<T>> fun) {
		ChrObj_Obj<V, Puller<T>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet<>(() -> Puller.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(ChrObj_Obj<V, Streamlet2<K1, V1>> fun) {
		ChrObj_Obj<V, Puller2<K1, V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet2<>(() -> Puller2.of(spawn().concatMap2(bf)));
	}

	private <V1> ChrObjStreamlet<V1> concatMapChrObj_(ChrObj_Obj<V, ChrObjStreamlet<V1>> fun) {
		ChrObj_Obj<V, ChrObjPuller<V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return streamlet(() -> ChrObjPuller.of(spawn().concatMapChrObj(bf)));
	}

	private ChrObjPuller<List<V>> groupBy_() {
		return ChrObjPuller.of(toListMap_().source());
	}

	private <T> Streamlet<T> map_(ChrObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(ChrObj_Obj<V, K1> kf, ChrObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> ChrObjStreamlet<V1> mapChrObj_(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return streamlet(() -> spawn().mapChrObj(kf, vf));
	}

	private List<ChrObjPair<V>> toList_() {
		return spawn().toList();
	}

	private ChrObjMap<List<V>> toListMap_() {
		var source = spawn().source();
		var map = new ChrObjMap<List<V>>();
		var pair = ChrObjPair.of(ChrPrim.EMPTYVALUE, (V) null);
		while (source.source2(pair))
			map.computeIfAbsent(pair.k, k_ -> new ArrayList<>()).add(pair.v);
		return map;
	}

	private ChrObjPuller<V> spawn() {
		return in.g();
	}

}
