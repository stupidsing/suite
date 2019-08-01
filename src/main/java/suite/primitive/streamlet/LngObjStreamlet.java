package suite.primitive.streamlet;

import static primal.statics.Fail.fail;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Sink2;
import primal.primitive.LngObj_Lng;
import primal.primitive.LngPrim;
import primal.primitive.LngPrim.LngTest;
import primal.primitive.adt.pair.LngObjPair;
import suite.adt.map.ListMultimap;
import suite.primitive.LngObjFunUtil;
import suite.primitive.LngPrimitives.LngObjPredicate;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.LngObj_Obj;
import suite.primitive.adt.map.LngObjMap;
import suite.primitive.adt.map.ObjLngMap;
import suite.streamlet.FunUtil;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;

public class LngObjStreamlet<V> implements StreamletDefaults<LngObjPair<V>, LngObjPuller<V>> {

	private Source<LngObjPuller<V>> in;

	@SafeVarargs
	public static <V> LngObjStreamlet<V> concat(LngObjStreamlet<V>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return LngObjPuller.of(LngObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> LngObjStreamlet<V> streamlet(Source<LngObjPuller<V>> in) {
		return new LngObjStreamlet<>(in);
	}

	public LngObjStreamlet(Source<LngObjPuller<V>> in) {
		this.in = in;
	}

	public <R> R apply(Fun<LngObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<LngObjPuller<V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public LngObjStreamlet<V> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(LngObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(LngObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> LngObjStreamlet<V1> concatMapLngObj(LngObj_Obj<V, LngObjStreamlet<V1>> fun) {
		return concatMapLngObj_(fun);
	}

	public <V1> LngObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Puller<V1>> f = v -> fun.apply(v).puller();
		return streamlet(() -> LngObjPuller.of(spawn().concatMapValue(f)));
	}

	public LngObjStreamlet<V> cons(long key, V value) {
		return streamlet(() -> spawn().cons(key, value));
	}

	public LngObjStreamlet<V> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public LngObjStreamlet<V> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == LngObjStreamlet.class ? Equals.ab(spawn(), ((LngObjStreamlet<?>) object).spawn())
				: false;
	}

	public LngObjStreamlet<V> filter(LngObjPredicate<V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public LngObjStreamlet<V> filterKey(LngTest fun) {
		return streamlet(() -> spawn().filterKey(fun));
	}

	public LngObjStreamlet<V> filterValue(Predicate<V> fun) {
		return streamlet(() -> spawn().filterValue(fun));
	}

	public LngObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(LngObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public LngObjStreamlet<List<V>> groupBy() {
		return streamlet(() -> spawn().groupBy());
	}

	public <V1> LngObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return streamlet(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(LngObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(LngObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	@Override
	public Iterator<LngObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public LngStreamlet keys() {
		return new LngStreamlet(() -> spawn().keys());
	}

	public LngObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(LngObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Long, V1> map2(LngObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(LngObj_Obj<V, K1> kf, LngObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> LngObjStreamlet<V1> mapLngObj(LngObj_Lng<V> kf, LngObj_Obj<V, V1> vf) {
		return mapLngObj_(kf, vf);
	}

	public <V1> LngObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return streamlet(() -> spawn().mapValue(fun));
	}

	public LngObjPair<V> min(Comparator<LngObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public LngObjPair<V> minOrNull(Comparator<LngObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public LngObjPair<V> opt() {
		return spawn().opt();
	}

	public Streamlet<LngObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<LngObjStreamlet<V>, LngObjStreamlet<V>> partition(LngObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public LngObjPuller<V> puller() {
		return spawn();
	}

	public LngObjStreamlet<V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(Sink2<Long, V> sink) {
		spawn().sink(sink);
	}

	public LngObjStreamlet<V> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public LngObjStreamlet<V> snoc(long key, V value) {
		return streamlet(() -> spawn().snoc(key, value));
	}

	public LngObjStreamlet<V> sort(Comparator<LngObjPair<V>> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> LngObjStreamlet<V> sortBy(LngObj_Obj<V, O> fun) {
		return streamlet(() -> spawn().sortBy(fun));
	}

	public LngObjStreamlet<V> sortByKey(Comparator<Long> comparator) {
		return streamlet(() -> spawn().sortByKey(comparator));
	}

	public LngObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return streamlet(() -> spawn().sortByValue(comparator));
	}

	public LngObjSource<V> source() {
		return spawn().source();
	}

	public LngObjStreamlet<V> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public LngObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<LngObjPair<V>> toList() {
		return toList_();
	}

	public LngObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public LngObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Long, V> toMultimap() {
		return spawn().toMultimap();
	}

	public ObjLngMap<V> toObjLngMap() {
		return spawn().toObjLngMap();
	}

	public Set<LngObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public LngObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public LngObjPair<V> uniqueResult() {
		var pair = spawn().opt();
		if (pair.k != LngPrim.EMPTYVALUE)
			return pair;
		else
			return fail("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(LngObj_Obj<V, Streamlet<T>> fun) {
		LngObj_Obj<V, Puller<T>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet<>(() -> Puller.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(LngObj_Obj<V, Streamlet2<K1, V1>> fun) {
		LngObj_Obj<V, Puller2<K1, V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet2<>(() -> Puller2.of(spawn().concatMap2(bf)));
	}

	private <V1> LngObjStreamlet<V1> concatMapLngObj_(LngObj_Obj<V, LngObjStreamlet<V1>> fun) {
		LngObj_Obj<V, LngObjPuller<V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return streamlet(() -> LngObjPuller.of(spawn().concatMapLngObj(bf)));
	}

	private <T> Streamlet<T> map_(LngObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(LngObj_Obj<V, K1> kf, LngObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> LngObjStreamlet<V1> mapLngObj_(LngObj_Lng<V> kf, LngObj_Obj<V, V1> vf) {
		return streamlet(() -> spawn().mapLngObj(kf, vf));
	}

	private List<LngObjPair<V>> toList_() {
		return spawn().toList();
	}

	private LngObjPuller<V> spawn() {
		return in.g();
	}

}
