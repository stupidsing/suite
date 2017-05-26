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
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.FltPrimitiveFun.Obj_Flt;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.primitive.PrimitiveFun.Obj_Double;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2;
import suite.util.FunUtil2.Source2;
import suite.util.Object_;
import suite.util.To;

public class Streamlet2<K, V> implements Iterable<Pair<K, V>> {

	private Source<Outlet2<K, V>> in;

	@SafeVarargs
	public static <K, V> Streamlet2<K, V> concat(Streamlet2<K, V>... streamlets) {
		return streamlet2(() -> {
			List<Source2<K, V>> sources = new ArrayList<>();
			for (Streamlet2<K, V> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return Outlet2.of(FunUtil2.concat(To.source(sources)));
		});
	}

	private static <K, V> Streamlet2<K, V> streamlet2(Source<Outlet2<K, V>> in) {
		return new Streamlet2<>(in);
	}

	public Streamlet2(Source<Outlet2<K, V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<Pair<K, V>> iterator() {
		return spawn().iterator();
	}

	public Streamlet2<K, V> append(K key, V value) {
		return streamlet2(() -> spawn().append(key, value));
	}

	public Streamlet2<K, V> closeAtEnd(Closeable c) {
		return streamlet2(() -> {
			Outlet2<K, V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<Outlet2<K, V>, R> fun) {
		return fun.apply(spawn());
	}

	public double collectAsDouble(Obj_Double<Outlet2<K, V>> fun) {
		return fun.applyAsDouble(spawn());
	}

	public float collectAsFloat(Obj_Flt<Outlet2<K, V>> fun) {
		return fun.apply(spawn());
	}

	public int collectAsInt(Obj_Int<Outlet2<K, V>> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(ObjObj_Obj<K, V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(ObjObj_Obj<K, V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> Streamlet2<K, V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return streamlet2(() -> Outlet2.of(spawn().concatMapValue(f)));
	}

	public Streamlet2<K, V> cons(K key, V value) {
		return streamlet2(() -> spawn().cons(key, value));
	}

	public Streamlet2<K, V> distinct() {
		return streamlet2(() -> spawn().distinct());
	}

	public Streamlet2<K, V> drop(int n) {
		return streamlet2(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == Streamlet2.class ? Objects.equals(spawn(), ((Streamlet2<?, ?>) object).spawn()) : false;
	}

	public Streamlet2<K, V> filter(BiPredicate<K, V> fun) {
		return streamlet2(() -> spawn().filter(fun));
	}

	public Streamlet2<K, V> filterKey(Predicate<K> fun) {
		return streamlet2(() -> spawn().filterKey(fun));
	}

	public Streamlet2<K, V> filterValue(Predicate<V> fun) {
		return streamlet2(() -> spawn().filterValue(fun));
	}

	public Pair<K, V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(ObjObj_Obj<K, V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public Streamlet2<K, List<V>> groupBy() {
		return new Streamlet2<>(() -> spawn().groupBy());
	}

	public <V1> Streamlet2<K, V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return new Streamlet2<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(BiPredicate<K, V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(BiPredicate<K, V> pred) {
		return spawn().isAny(pred);
	}

	public Streamlet<K> keys() {
		return new Streamlet<>(() -> spawn().keys());
	}

	public Pair<K, V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(ObjObj_Obj<K, V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<K, V1> map2(ObjObj_Obj<K, V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(ObjObj_Obj<K, V, K1> kf, ObjObj_Obj<K, V, V1> vf) {
		return map2_(kf, vf);
	}

	public <K1> Streamlet2<K1, V> mapKey(Fun<K, K1> fun) {
		return new Streamlet2<>(() -> spawn().mapKey(fun));
	}

	public <O> Streamlet<O> mapNonNull(ObjObj_Obj<K, V, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public <V1> Streamlet2<K, V1> mapValue(Fun<V, V1> fun) {
		return new Streamlet2<>(() -> spawn().mapValue(fun));
	}

	public Pair<K, V> min(Comparator<Pair<K, V>> comparator) {
		return spawn().min(comparator);
	}

	public Pair<K, V> minOrNull(Comparator<Pair<K, V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public Outlet2<K, V> out() {
		return spawn();
	}

	public Streamlet<Pair<K, V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<Streamlet2<K, V>, Streamlet2<K, V>> partition(BiPredicate<K, V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public Streamlet2<K, V> reverse() {
		return streamlet2(() -> spawn().reverse());
	}

	public void sink(BiConsumer<K, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public Streamlet2<K, V> skip(int n) {
		return streamlet2(() -> spawn().skip(n));
	}

	public Streamlet2<K, V> sort(Comparator<Pair<K, V>> comparator) {
		return streamlet2(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> Streamlet2<K, V> sortBy(ObjObj_Obj<K, V, O> fun) {
		return streamlet2(() -> spawn().sortBy(fun));
	}

	public Streamlet2<K, V> sortByKey(Comparator<K> comparator) {
		return streamlet2(() -> spawn().sortByKey(comparator));
	}

	public Streamlet2<K, V> sortByValue(Comparator<V> comparator) {
		return streamlet2(() -> spawn().sortByValue(comparator));
	}

	public Source2<K, V> source() {
		return spawn().source();
	}

	public Streamlet2<K, V> take(int n) {
		return streamlet2(() -> spawn().take(n));
	}

	public Pair<K, V>[] toArray() {
		return spawn().toArray();
	}

	public List<Pair<K, V>> toList() {
		return spawn().toList();
	}

	public Map<K, List<V>> toListMap() {
		return spawn().toListMap();
	}

	public Map<K, V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<K, V> toMultimap() {
		return spawn().toMultimap();
	}

	public Set<Pair<K, V>> toSet() {
		return spawn().toSet();
	}

	public Map<K, Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public Pair<K, V> uniqueResult() {
		return spawn().uniqueResult();
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(ObjObj_Obj<K, V, Streamlet<T>> fun) {
		ObjObj_Obj<K, V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <K1, V1> Streamlet2<K1, V1> concatMap2_(ObjObj_Obj<K, V, Streamlet2<K1, V1>> fun) {
		ObjObj_Obj<K, V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).out();
		return streamlet2(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <T> Streamlet<T> map_(ObjObj_Obj<K, V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(ObjObj_Obj<K, V, K1> kf, ObjObj_Obj<K, V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private Outlet2<K, V> spawn() {
		return in.source();
	}

}
