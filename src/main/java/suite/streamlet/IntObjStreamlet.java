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
import suite.primitive.PrimitivePredicate.IntObjPredicate;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.IntObjFunUtil;
import suite.util.To;
import suite.util.Util;

public class IntObjStreamlet<V> implements Iterable<IntObjPair<V>> {

	private Source<IntObjOutlet<V>> in;

	@SafeVarargs
	public static <V> IntObjStreamlet<V> concat(IntObjStreamlet<V>... streamlets) {
		return streamlet2(() -> {
			List<IntObjSource<V>> sources = new ArrayList<>();
			for (IntObjStreamlet<V> streamlet : streamlets)
				sources.add(streamlet.in.source().source2());
			return IntObjOutlet.of(IntObjFunUtil.concat(To.source(sources)));
		});
	}

	public static <V> IntObjStreamlet<V> of(IntObjSource<V> source) {
		return streamlet2(() -> IntObjOutlet.of(source));
	}

	private static <V> IntObjStreamlet<V> streamlet2(Source<IntObjOutlet<V>> in) {
		return new IntObjStreamlet<>(in);
	}

	public IntObjStreamlet(Source<IntObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<IntObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public IntObjStreamlet<V> closeAtEnd(Closeable c) {
		return streamlet2(() -> {
			IntObjOutlet<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<IntObjOutlet<V>, R> fun) {
		return fun.apply(spawn());
	}

	public <T> Streamlet<T> concatMap(BiFunction<Integer, V, Streamlet<T>> fun) {
		BiFunction<Integer, V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	public <K1, V1> IntObjStreamlet<V1> concatMap2(BiFunction<Integer, V, IntObjStreamlet<V1>> fun) {
		BiFunction<Integer, V, IntObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).outlet2();
		return streamlet2(() -> IntObjOutlet.of(spawn().concatMap2(bf)));
	}

	public <V1> IntObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return streamlet2(() -> IntObjOutlet.of(spawn().concatMapValue(f)));
	}

	public IntObjStreamlet<V> cons(Integer key, V value) {
		return streamlet2(() -> spawn().cons(key, value));
	}

	public IntObjStreamlet<V> distinct() {
		return streamlet2(() -> spawn().distinct());
	}

	public IntObjStreamlet<V> drop(int n) {
		return streamlet2(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Util.clazz(object) == IntObjStreamlet.class ? Objects.equals(spawn(), ((IntObjStreamlet<?>) object).spawn()) : false;
	}

	public IntObjStreamlet<V> filter(IntObjPredicate<V> fun) {
		return streamlet2(() -> spawn().filter(fun));
	}

	public IntObjStreamlet<V> filterKey(IntPredicate fun) {
		return streamlet2(() -> spawn().filterKey(fun));
	}

	public IntObjStreamlet<V> filterValue(Predicate<V> fun) {
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

	public boolean isAll(IntObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(IntObjPredicate<V> pred) {
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

	public <V1> IntObjStreamlet<V1> mapIntObj(IntObj_Int<V> kf, IntObj_Obj<V, V1> vf) {
		return new IntObjStreamlet<>(() -> spawn().mapIntObj(kf, vf));
	}

	public IntObjStreamlet<V> mapKey(Int_Int fun) {
		return new IntObjStreamlet<>(() -> spawn().mapKey(fun));
	}

	public <V1> IntObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return new IntObjStreamlet<>(() -> spawn().mapValue(fun));
	}

	public IntObjPair<V> min(Comparator<IntObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public IntObjPair<V> minOrNull(Comparator<IntObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public IntObjOutlet<V> outlet2() {
		return spawn();
	}

	public Streamlet<IntObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<IntObjStreamlet<V>, IntObjStreamlet<V>> partition(IntObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public IntObjStreamlet<V> reverse() {
		return streamlet2(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Integer, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public IntObjStreamlet<V> skip(int n) {
		return streamlet2(() -> spawn().skip(n));
	}

	public IntObjStreamlet<V> sort(Comparator<IntObjPair<V>> comparator) {
		return streamlet2(() -> spawn().sort(comparator));
	}

	public IntObjStreamlet<V> sortByKey(Comparator<Integer> comparator) {
		return streamlet2(() -> spawn().sortByKey(comparator));
	}

	public IntObjSource<V> source() {
		return spawn().source2();
	}

	public IntObjStreamlet<V> take(int n) {
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

	private IntObjOutlet<V> spawn() {
		return in.source();
	}

}
