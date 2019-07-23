package suite.primitive.streamlet;

import static suite.util.Friends.fail;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.object.Object_;
import suite.primitive.DblFunUtil;
import suite.primitive.DblObjFunUtil;
import suite.primitive.DblObj_Dbl;
import suite.primitive.DblPrimitives.DblObjPredicate;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.DblPrimitives.DblTest;
import suite.primitive.adt.map.DblObjMap;
import suite.primitive.adt.map.ObjDblMap;
import suite.primitive.adt.pair.DblObjPair;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Sink2;
import suite.streamlet.Puller;
import suite.streamlet.Puller2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;

public class DblObjStreamlet<V> implements StreamletDefaults<DblObjPair<V>, DblObjPuller<V>> {

	private Source<DblObjPuller<V>> in;

	@SafeVarargs
	public static <V> DblObjStreamlet<V> concat(DblObjStreamlet<V>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return DblObjPuller.of(DblObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> DblObjStreamlet<V> streamlet(Source<DblObjPuller<V>> in) {
		return new DblObjStreamlet<>(in);
	}

	public DblObjStreamlet(Source<DblObjPuller<V>> in) {
		this.in = in;
	}

	public <R> R apply(Fun<DblObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<DblObjPuller<V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public DblObjStreamlet<V> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(DblObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(DblObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> DblObjStreamlet<V1> concatMapDblObj(DblObj_Obj<V, DblObjStreamlet<V1>> fun) {
		return concatMapDblObj_(fun);
	}

	public <V1> DblObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Puller<V1>> f = v -> fun.apply(v).puller();
		return streamlet(() -> DblObjPuller.of(spawn().concatMapValue(f)));
	}

	public DblObjStreamlet<V> cons(double key, V value) {
		return streamlet(() -> spawn().cons(key, value));
	}

	public DblObjStreamlet<V> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public DblObjStreamlet<V> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == DblObjStreamlet.class ? Objects.equals(spawn(), ((DblObjStreamlet<?>) object).spawn())
				: false;
	}

	public DblObjStreamlet<V> filter(DblObjPredicate<V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public DblObjStreamlet<V> filterKey(DblTest fun) {
		return streamlet(() -> spawn().filterKey(fun));
	}

	public DblObjStreamlet<V> filterValue(Predicate<V> fun) {
		return streamlet(() -> spawn().filterValue(fun));
	}

	public DblObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(DblObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public DblObjStreamlet<List<V>> groupBy() {
		return streamlet(() -> spawn().groupBy());
	}

	public <V1> DblObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return streamlet(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(DblObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(DblObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	@Override
	public Iterator<DblObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public DblStreamlet keys() {
		return new DblStreamlet(() -> spawn().keys());
	}

	public DblObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(DblObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Double, V1> map2(DblObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(DblObj_Obj<V, K1> kf, DblObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> DblObjStreamlet<V1> mapDblObj(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return mapDblObj_(kf, vf);
	}

	public <V1> DblObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return streamlet(() -> spawn().mapValue(fun));
	}

	public DblObjPair<V> min(Comparator<DblObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public DblObjPair<V> minOrNull(Comparator<DblObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public DblObjPair<V> opt() {
		return spawn().opt();
	}

	public Streamlet<DblObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<DblObjStreamlet<V>, DblObjStreamlet<V>> partition(DblObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public DblObjPuller<V> puller() {
		return spawn();
	}

	public DblObjStreamlet<V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(Sink2<Double, V> sink) {
		spawn().sink(sink);
	}

	public DblObjStreamlet<V> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public DblObjStreamlet<V> snoc(double key, V value) {
		return streamlet(() -> spawn().snoc(key, value));
	}

	public DblObjStreamlet<V> sort(Comparator<DblObjPair<V>> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> DblObjStreamlet<V> sortBy(DblObj_Obj<V, O> fun) {
		return streamlet(() -> spawn().sortBy(fun));
	}

	public DblObjStreamlet<V> sortByKey(Comparator<Double> comparator) {
		return streamlet(() -> spawn().sortByKey(comparator));
	}

	public DblObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return streamlet(() -> spawn().sortByValue(comparator));
	}

	public DblObjSource<V> source() {
		return spawn().source();
	}

	public DblObjStreamlet<V> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public DblObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<DblObjPair<V>> toList() {
		return toList_();
	}

	public DblObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public DblObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Double, V> toMultimap() {
		return spawn().toMultimap();
	}

	public ObjDblMap<V> toObjDblMap() {
		return spawn().toObjDblMap();
	}

	public Set<DblObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public DblObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public DblObjPair<V> uniqueResult() {
		var pair = spawn().opt();
		if (pair.k != DblFunUtil.EMPTYVALUE)
			return pair;
		else
			return fail("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(DblObj_Obj<V, Streamlet<T>> fun) {
		DblObj_Obj<V, Puller<T>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet<>(() -> Puller.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(DblObj_Obj<V, Streamlet2<K1, V1>> fun) {
		DblObj_Obj<V, Puller2<K1, V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet2<>(() -> Puller2.of(spawn().concatMap2(bf)));
	}

	private <V1> DblObjStreamlet<V1> concatMapDblObj_(DblObj_Obj<V, DblObjStreamlet<V1>> fun) {
		DblObj_Obj<V, DblObjPuller<V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return streamlet(() -> DblObjPuller.of(spawn().concatMapDblObj(bf)));
	}

	private <T> Streamlet<T> map_(DblObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(DblObj_Obj<V, K1> kf, DblObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> DblObjStreamlet<V1> mapDblObj_(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return streamlet(() -> spawn().mapDblObj(kf, vf));
	}

	private List<DblObjPair<V>> toList_() {
		return spawn().toList();
	}

	private DblObjPuller<V> spawn() {
		return in.g();
	}

}
