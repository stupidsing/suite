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
import suite.primitive.IntFunUtil;
import suite.primitive.IntObjFunUtil;
import suite.primitive.IntObj_Int;
import suite.primitive.IntPrimitives.IntObjPredicate;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.IntTest;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.FunUtil;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Sink2;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;

public class IntObjStreamlet<V> implements StreamletDefaults<IntObjPair<V>, IntObjOutlet<V>> {

	private Source<IntObjOutlet<V>> in;

	@SafeVarargs
	public static <V> IntObjStreamlet<V> concat(IntObjStreamlet<V>... streamlets) {
		return streamlet(() -> {
			var source = Read.from(streamlets).outlet().source();
			return IntObjOutlet.of(IntObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> IntObjStreamlet<V> streamlet(Source<IntObjOutlet<V>> in) {
		return new IntObjStreamlet<>(in);
	}

	public IntObjStreamlet(Source<IntObjOutlet<V>> in) {
		this.in = in;
	}

	public <R> R apply(Fun<IntObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<IntObjOutlet<V>> chunk(int n) {
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
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return streamlet(() -> IntObjOutlet.of(spawn().concatMapValue(f)));
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
		return Object_.clazz(object) == IntObjStreamlet.class ? Objects.equals(spawn(), ((IntObjStreamlet<?>) object).spawn())
				: false;
	}

	public IntObjStreamlet<V> filter(IntObjPredicate<V> fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public IntObjStreamlet<V> filterKey(IntTest fun) {
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
		return streamlet(() -> spawn().groupBy());
	}

	public <V1> IntObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return streamlet(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(IntObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(IntObjPredicate<V> pred) {
		return spawn().isAny(pred);
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

	public IntObjPair<V> opt() {
		return spawn().opt();
	}

	public IntObjOutlet<V> outlet() {
		return spawn();
	}

	public Streamlet<IntObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<IntObjStreamlet<V>, IntObjStreamlet<V>> partition(IntObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public IntObjStreamlet<V> reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(Sink2<Integer, V> sink) {
		spawn().sink(sink);
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

	public IntObjSource<V> source() {
		return spawn().source();
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
		return spawn().toListMap();
	}

	public IntObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Integer, V> toMultimap() {
		return spawn().toMultimap();
	}

	public ObjIntMap<V> toObjIntMap() {
		return spawn().toObjIntMap();
	}

	public Set<IntObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public IntObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public IntObjPair<V> uniqueResult() {
		var pair = spawn().opt();
		if (pair.t0 != IntFunUtil.EMPTYVALUE)
			return pair;
		else
			return fail("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(IntObj_Obj<V, Streamlet<T>> fun) {
		IntObj_Obj<V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(IntObj_Obj<V, Streamlet2<K1, V1>> fun) {
		IntObj_Obj<V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> IntObjStreamlet<V1> concatMapIntObj_(IntObj_Obj<V, IntObjStreamlet<V1>> fun) {
		IntObj_Obj<V, IntObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return streamlet(() -> IntObjOutlet.of(spawn().concatMapIntObj(bf)));
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

	private IntObjOutlet<V> spawn() {
		return in.source();
	}

}
