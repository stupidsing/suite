package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import suite.adt.map.DblObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.DblObjPair;
import suite.adt.pair.Pair;
import suite.primitive.DblObjFunUtil;
import suite.primitive.DblObj_Dbl;
import suite.primitive.DblPrimitives.DblObjPredicate;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblObj_Obj;
import suite.primitive.DblPrimitives.DblPredicate_;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Dbl_Dbl;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class DblObjStreamlet<V> implements Iterable<DblObjPair<V>> {

	private Source<DblObjOutlet<V>> in;

	@SafeVarargs
	public static <V> DblObjStreamlet<V> concat(DblObjStreamlet<V>... streamlets) {
		return dblObjStreamlet(() -> {
			List<DblObjSource<V>> sources = new ArrayList<>();
			for (DblObjStreamlet<V> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return DblObjOutlet.of(DblObjFunUtil.concat(To.source(sources)));
		});
	}

	private static <V> DblObjStreamlet<V> dblObjStreamlet(Source<DblObjOutlet<V>> in) {
		return new DblObjStreamlet<>(in);
	}

	public DblObjStreamlet(Source<DblObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<DblObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public DblObjStreamlet<V> append(double key, V value) {
		return dblObjStreamlet(() -> spawn().append(key, value));
	}

	public DblObjStreamlet<V> closeAtEnd(Closeable c) {
		return dblObjStreamlet(() -> {
			DblObjOutlet<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<DblObjOutlet<V>, R> fun) {
		return fun.apply(spawn());
	}

	public double collectAsDouble(Obj_Dbl<DblObjOutlet<V>> fun) {
		return fun.apply(spawn());
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
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return dblObjStreamlet(() -> DblObjOutlet.of(spawn().concatMapValue(f)));
	}

	public DblObjStreamlet<V> cons(double key, V value) {
		return dblObjStreamlet(() -> spawn().cons(key, value));
	}

	public DblObjStreamlet<V> distinct() {
		return dblObjStreamlet(() -> spawn().distinct());
	}

	public DblObjStreamlet<V> drop(int n) {
		return dblObjStreamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == DblObjStreamlet.class ? Objects.equals(spawn(), ((DblObjStreamlet<?>) object).spawn())
				: false;
	}

	public DblObjStreamlet<V> filter(DblObjPredicate<V> fun) {
		return dblObjStreamlet(() -> spawn().filter(fun));
	}

	public DblObjStreamlet<V> filterKey(DblPredicate_ fun) {
		return dblObjStreamlet(() -> spawn().filterKey(fun));
	}

	public DblObjStreamlet<V> filterValue(Predicate<V> fun) {
		return dblObjStreamlet(() -> spawn().filterValue(fun));
	}

	public DblObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(DblObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public DblObjStreamlet<List<V>> groupBy() {
		return new DblObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V1> DblObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return new DblObjStreamlet<>(() -> spawn().groupBy(fun));
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

	public Streamlet<Double> keys() {
		return new Streamlet<>(() -> spawn().keys());
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

	public DblObjStreamlet<V> mapKey(Dbl_Dbl fun) {
		return new DblObjStreamlet<>(() -> spawn().mapKey(fun));
	}

	public <O> Streamlet<O> mapNonNull(DblObj_Obj<V, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public <V1> DblObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return new DblObjStreamlet<>(() -> spawn().mapValue(fun));
	}

	public DblObjPair<V> min(Comparator<DblObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public DblObjPair<V> minOrNull(Comparator<DblObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public DblObjOutlet<V> out() {
		return spawn();
	}

	public Streamlet<DblObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<DblObjStreamlet<V>, DblObjStreamlet<V>> partition(DblObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public DblObjStreamlet<V> reverse() {
		return dblObjStreamlet(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Double, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public DblObjStreamlet<V> skip(int n) {
		return dblObjStreamlet(() -> spawn().skip(n));
	}

	public DblObjStreamlet<V> sort(Comparator<DblObjPair<V>> comparator) {
		return dblObjStreamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> DblObjStreamlet<V> sortBy(DblObj_Obj<V, O> fun) {
		return dblObjStreamlet(() -> spawn().sortBy(fun));
	}

	public DblObjStreamlet<V> sortByKey(Comparator<Double> comparator) {
		return dblObjStreamlet(() -> spawn().sortByKey(comparator));
	}

	public DblObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return dblObjStreamlet(() -> spawn().sortByValue(comparator));
	}

	public DblObjSource<V> source() {
		return spawn().source();
	}

	public DblObjStreamlet<V> take(int n) {
		return dblObjStreamlet(() -> spawn().take(n));
	}

	public DblObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<DblObjPair<V>> toList() {
		return spawn().toList();
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

	public Set<DblObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public DblObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public DblObjPair<V> uniqueResult() {
		return spawn().uniqueResult();
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(DblObj_Obj<V, Streamlet<T>> fun) {
		DblObj_Obj<V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(DblObj_Obj<V, Streamlet2<K1, V1>> fun) {
		DblObj_Obj<V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).out();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> DblObjStreamlet<V1> concatMapDblObj_(DblObj_Obj<V, DblObjStreamlet<V1>> fun) {
		DblObj_Obj<V, DblObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).out();
		return dblObjStreamlet(() -> DblObjOutlet.of(spawn().concatMapDblObj(bf)));
	}

	private <T> Streamlet<T> map_(DblObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(DblObj_Obj<V, K1> kf, DblObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> DblObjStreamlet<V1> mapDblObj_(DblObj_Dbl<V> kf, DblObj_Obj<V, V1> vf) {
		return new DblObjStreamlet<>(() -> spawn().mapDblObj(kf, vf));
	}

	private DblObjOutlet<V> spawn() {
		return in.source();
	}

}
