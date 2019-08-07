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
import primal.primitive.IntObj_Int;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntObjPredicate;
import primal.primitive.IntPrim.IntObjSink;
import primal.primitive.IntPrim.IntObjSource;
import primal.primitive.IntPrim.IntObj_Obj;
import primal.primitive.IntPrim.IntPred;
import primal.primitive.adt.map.IntObjMap;
import primal.primitive.adt.map.ObjIntMap;
import primal.primitive.adt.pair.IntObjPair;
import primal.primitive.fp.IntObjFunUtil;
import primal.primitive.puller.IntObjPuller;
import primal.puller.Puller;
import primal.puller.Puller2;
import primal.streamlet.StreamletDefaults;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;

public class IntObjStreamlet<V> implements StreamletDefaults<IntObjPair<V>, IntObjPair<V>, IntObjPredicate<V>, IntObjPuller<V>, IntObjSink<V>, IntObjSource<V>> {

	private Source<IntObjPuller<V>> in;

	@SafeVarargs
	public static <V> IntObjStreamlet<V> concat(IntObjStreamlet<V>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).puller().source();
			return IntObjPuller.of(IntObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> IntObjStreamlet<V> streamlet(Source<IntObjPuller<V>> in) {
		return new IntObjStreamlet<>(in);
	}

	public IntObjStreamlet(Source<IntObjPuller<V>> in) {
		this.in = in;
	}

	public <R> R apply(Fun<IntObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<IntObjPuller<V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public IntObjStreamlet<V> closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(IntObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(IntObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> IntObjStreamlet<V1> concatMapIntObj(IntObj_Obj<V, IntObjStreamlet<V1>> fun) {
		return concatMapIntObj_(fun);
	}

	public <V1> IntObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Puller<V1>> f = v -> fun.apply(v).puller();
		return streamlet(() -> IntObjPuller.of(spawn().concatMapValue(f)));
	}

	public IntObjStreamlet<V> cons(int key, V value) {
		return streamlet(() -> spawn().cons(key, value));
	}

	public IntObjStreamlet<V> distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public IntObjStreamlet<V> drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == IntObjStreamlet.class ? Equals.ab(spawn(), ((IntObjStreamlet<?>) object).spawn())
				: false;
	}

	public IntObjStreamlet<V> filter(IntObjPredicate<V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public IntObjStreamlet<V> filterKey(IntPred fun) {
		return streamlet(() -> spawn().filterKey(fun));
	}

	public IntObjStreamlet<V> filterValue(Predicate<V> fun) {
		return streamlet(() -> spawn().filterValue(fun));
	}

	public IntObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(IntObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public IntObjStreamlet<List<V>> groupBy() {
		return streamlet(this::groupBy_);
	}

	public <V1> IntObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return streamlet(() -> groupBy_().mapValue(list -> fun.apply(Read.from(list))));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	@Override
	public Iterator<IntObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public IntStreamlet keys() {
		return new IntStreamlet(() -> spawn().keys());
	}

	public IntObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(IntObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Integer, V1> map2(IntObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(IntObj_Obj<V, K1> kf, IntObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> IntObjStreamlet<V1> mapIntObj(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return mapIntObj_(kf, vf);
	}

	public <V1> IntObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return streamlet(() -> spawn().mapValue(fun));
	}

	public IntObjPair<V> min(Comparator<IntObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public IntObjPair<V> minOrNull(Comparator<IntObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Streamlet<IntObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<IntObjStreamlet<V>, IntObjStreamlet<V>> partition(IntObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public IntObjPuller<V> puller() {
		return spawn();
	}

	public IntObjStreamlet<V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public IntObjStreamlet<V> skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public IntObjStreamlet<V> snoc(int key, V value) {
		return streamlet(() -> spawn().snoc(key, value));
	}

	public IntObjStreamlet<V> sort(Comparator<IntObjPair<V>> comparator) {
		return streamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> IntObjStreamlet<V> sortBy(IntObj_Obj<V, O> fun) {
		return streamlet(() -> spawn().sortBy(fun));
	}

	public IntObjStreamlet<V> sortByKey(Comparator<Integer> comparator) {
		return streamlet(() -> spawn().sortByKey(comparator));
	}

	public IntObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return streamlet(() -> spawn().sortByValue(comparator));
	}

	public IntObjStreamlet<V> take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public IntObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<IntObjPair<V>> toList() {
		return toList_();
	}

	public IntObjMap<List<V>> toListMap() {
		return toListMap_();
	}

	public IntObjMap<V> toMap() {
		var source = spawn().source();
		var map = new IntObjMap<V>();
		var pair = IntObjPair.of(IntPrim.EMPTYVALUE, (V) null);
		while (source.source2(pair))
			map.put(pair.k, pair.v);
		return map;
	}

	public ListMultimap<Integer, V> toMultimap() {
		var map = new ListMultimap<Integer, V>();
		groupBy_().concatMapValue(Puller::of).sink(map::put);
		return map;
	}

	public ObjIntMap<V> toObjIntMap() {
		var source = spawn().source();
		var pair = IntObjPair.of(IntPrim.EMPTYVALUE, (V) null);
		var map = new ObjIntMap<V>();
		while (source.source2(pair))
			map.put(pair.v, pair.k);
		return map;
	}

	public Set<IntObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public IntObjPair<V> uniqueResult() {
		var pair = spawn().opt();
		return pair.k != IntPrim.EMPTYVALUE ? pair : fail("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(IntObj_Obj<V, Streamlet<T>> fun) {
		IntObj_Obj<V, Puller<T>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet<>(() -> Puller.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(IntObj_Obj<V, Streamlet2<K1, V1>> fun) {
		IntObj_Obj<V, Puller2<K1, V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return new Streamlet2<>(() -> Puller2.of(spawn().concatMap2(bf)));
	}

	private <V1> IntObjStreamlet<V1> concatMapIntObj_(IntObj_Obj<V, IntObjStreamlet<V1>> fun) {
		IntObj_Obj<V, IntObjPuller<V1>> bf = (k, v) -> fun.apply(k, v).puller();
		return streamlet(() -> IntObjPuller.of(spawn().concatMapIntObj(bf)));
	}

	private IntObjPuller<List<V>> groupBy_() {
		return IntObjPuller.of(toListMap_().source());
	}

	private <T> Streamlet<T> map_(IntObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(IntObj_Obj<V, K1> kf, IntObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> IntObjStreamlet<V1> mapIntObj_(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return streamlet(() -> spawn().mapIntObj(kf, vf));
	}

	private List<IntObjPair<V>> toList_() {
		return spawn().toList();
	}

	private IntObjMap<List<V>> toListMap_() {
		var source = spawn().source();
		var map = new IntObjMap<List<V>>();
		var pair = IntObjPair.of(IntPrim.EMPTYVALUE, (V) null);
		while (source.source2(pair))
			map.computeIfAbsent(pair.k, k_ -> new ArrayList<>()).add(pair.v);
		return map;
	}

	private IntObjPuller<V> spawn() {
		return in.g();
	}

}
