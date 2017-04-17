package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import suite.adt.IntObjPair;
import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.primitive.PrimitiveFun.IntObj_Int;
import suite.primitive.PrimitiveFun.IntObj_Obj;
import suite.primitive.PrimitiveFun.Int_Int;
import suite.primitive.PrimitivePredicate.IntObjPredicate2;
import suite.primitive.PrimitiveSource.IntObjSource2;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.IntObjFunUtil2;
import suite.util.To;
import suite.util.Util;

public class IntObjStreamlet2<V> implements Iterable<IntObjPair<V>> {

	private Source<IntObjOutlet2<V>> in;

	@SafeVarargs
	public static <V> IntObjStreamlet2<V> concat(IntObjStreamlet2<V>... streamlets) {
		return streamlet2(() -> {
			List<IntObjSource2<V>> sources = new ArrayList<>();
			for (IntObjStreamlet2<V> streamlet : streamlets)
				sources.add(streamlet.in.source().source2());
			return IntObjOutlet2.of(IntObjFunUtil2.concat(To.source(sources)));
		});
	}

	public static <V> IntObjStreamlet2<V> of(IntObjSource2<V> source) {
		return streamlet2(() -> IntObjOutlet2.of(source));
	}

	private static <V> IntObjStreamlet2<V> streamlet2(Source<IntObjOutlet2<V>> in) {
		return new IntObjStreamlet2<>(in);
	}

	public IntObjStreamlet2(Source<IntObjOutlet2<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<IntObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public IntObjStreamlet2<V> closeAtEnd(Closeable c) {
		return streamlet2(() -> {
			IntObjOutlet2<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<IntObjOutlet2<V>, R> fun) {
		return fun.apply(spawn());
	}

	public <T> Streamlet<T> concatMap(BiFunction<Integer, V, Streamlet<T>> fun) {
		BiFunction<Integer, V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	public <K1, V1> IntObjStreamlet2<V1> concatMap2(BiFunction<Integer, V, IntObjStreamlet2<V1>> fun) {
		BiFunction<Integer, V, IntObjOutlet2<V1>> bf = (k, v) -> fun.apply(k, v).outlet2();
		return streamlet2(() -> IntObjOutlet2.of(spawn().concatMap2(bf)));
	}

	public <V1> IntObjStreamlet2<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return streamlet2(() -> IntObjOutlet2.of(spawn().concatMapValue(f)));
	}

	public IntObjStreamlet2<V> cons(Integer key, V value) {
		return streamlet2(() -> spawn().cons(key, value));
	}

	public IntObjStreamlet2<V> distinct() {
		return streamlet2(() -> spawn().distinct());
	}

	public IntObjStreamlet2<V> drop(int n) {
		return streamlet2(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == IntObjStreamlet2.class ? Objects.equals(spawn(), ((IntObjStreamlet2<?>) object).spawn())
				: false;
	}

	public IntObjStreamlet2<V> filter(IntObjPredicate2<V> fun) {
		return streamlet2(() -> spawn().filter(fun));
	}

	public IntObjStreamlet2<V> filterKey(IntPredicate fun) {
		return streamlet2(() -> spawn().filterKey(fun));
	}

	public IntObjStreamlet2<V> filterValue(Predicate<V> fun) {
		return streamlet2(() -> spawn().filterValue(fun));
	}

	public IntObjPair<V> first() {
		return spawn().first();
	}

	public Streamlet2<Integer, List<V>> groupBy() {
		return new Streamlet2<>(() -> spawn().groupBy());
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(IntObjPredicate2<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(IntObjPredicate2<V> pred) {
		return spawn().isAny(pred);
	}

	public IntObjPair<V> last() {
		return spawn().last();
	}

	public <T> Streamlet<T> map(IntObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	public <K1, V1> Streamlet2<K1, V1> mapEntry(IntObj_Obj<V, K1> kf, IntObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().mapEntry(kf, vf));
	}

	public <V1> IntObjStreamlet2<V1> mapIntObj(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return new IntObjStreamlet2<>(() -> spawn().mapIntObj(kf, vf));
	}

	public IntObjStreamlet2<V> mapKey(Int_Int fun) {
		return new IntObjStreamlet2<>(() -> spawn().mapKey(fun));
	}

	public <V1> IntObjStreamlet2<V1> mapValue(Fun<V, V1> fun) {
		return new IntObjStreamlet2<>(() -> spawn().mapValue(fun));
	}

	public IntObjPair<V> min(Comparator<IntObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public IntObjPair<V> minOrNull(Comparator<IntObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public IntObjOutlet2<V> outlet2() {
		return spawn();
	}

	public Streamlet<IntObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<IntObjStreamlet2<V>, IntObjStreamlet2<V>> partition(IntObjPredicate2<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public IntObjStreamlet2<V> reverse() {
		return streamlet2(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Integer, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public IntObjStreamlet2<V> skip(int n) {
		return streamlet2(() -> spawn().skip(n));
	}

	public IntObjStreamlet2<V> sort(Comparator<IntObjPair<V>> comparator) {
		return streamlet2(() -> spawn().sort(comparator));
	}

	public IntObjStreamlet2<V> sortByKey(Comparator<Integer> comparator) {
		return streamlet2(() -> spawn().sortByKey(comparator));
	}

	public IntObjSource2<V> source() {
		return spawn().source2();
	}

	public IntObjStreamlet2<V> take(int n) {
		return streamlet2(() -> spawn().take(n));
	}

	public IntObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<IntObjPair<V>> toList() {
		return spawn().toList();
	}

	public Map<Integer, List<V>> toListMap() {
		return spawn().toListMap();
	}

	public Map<Integer, V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Integer, V> toMultimap() {
		return spawn().toMultimap();
	}

	public Set<IntObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public Map<Integer, Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public IntObjPair<V> uniqueResult() {
		return spawn().uniqueResult();
	}

	private IntObjOutlet2<V> spawn() {
		return in.source();
	}

}
