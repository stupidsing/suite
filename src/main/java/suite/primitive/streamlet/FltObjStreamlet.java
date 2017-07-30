package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.FltFunUtil;
import suite.primitive.FltObjFunUtil;
import suite.primitive.FltObj_Flt;
import suite.primitive.FltPrimitives.FltObjPredicate;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.FltPrimitives.FltPredicate;
import suite.primitive.FltPrimitives.Obj_Flt;
import suite.primitive.Flt_Flt;
import suite.primitive.adt.map.FltObjMap;
import suite.primitive.adt.map.ObjFltMap;
import suite.primitive.adt.pair.FltObjPair;
import suite.streamlet.Outlet;
import suite.streamlet.Outlet2;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;

public class FltObjStreamlet<V> implements StreamletDefaults<FltObjPair<V>, FltObjOutlet<V>> {

	private Source<FltObjOutlet<V>> in;

	public static <T, V> Fun<Outlet<T>, FltObjStreamlet<V>> collect(Obj_Flt<T> kf0, Fun<T, V> vf0) {
		Obj_Flt<T> kf1 = kf0.rethrow();
		Fun<T, V> vf1 = vf0.rethrow();
		return outlet -> new FltObjStreamlet<>(() -> {
			Source<T> source = outlet.source();
			return FltObjOutlet.of(pair -> {
				T t = source.source();
				boolean b = t != null;
				if (b) {
					pair.t0 = kf1.apply(t);
					pair.t1 = vf1.apply(t);
				}
				return b;
			});
		});
	}

	@SafeVarargs
	public static <V> FltObjStreamlet<V> concat(FltObjStreamlet<V>... streamlets) {
		return Read.from(streamlets).collect(FltObjStreamlet::concat);
	}

	public static <U> FltObjStreamlet<U> concat(Outlet<FltObjStreamlet<U>> streamlets) {
		return fltObjStreamlet(() -> {
			Source<FltObjStreamlet<U>> source = streamlets.source();
			return FltObjOutlet.of(FltObjFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	private static <V> FltObjStreamlet<V> fltObjStreamlet(Source<FltObjOutlet<V>> in) {
		return new FltObjStreamlet<>(in);
	}

	public FltObjStreamlet(Source<FltObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<FltObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public FltObjStreamlet<V> append(float key, V value) {
		return fltObjStreamlet(() -> spawn().append(key, value));
	}

	public <R> R apply(Fun<FltObjStreamlet<V>, R> fun) {
		return fun.apply(this);
	}

	public Streamlet<FltObjOutlet<V>> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public FltObjStreamlet<V> closeAtEnd(Closeable c) {
		return fltObjStreamlet(() -> {
			FltObjOutlet<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<FltObjOutlet<V>, R> fun) {
		return fun.apply(spawn());
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
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return fltObjStreamlet(() -> FltObjOutlet.of(spawn().concatMapValue(f)));
	}

	public FltObjStreamlet<V> cons(float key, V value) {
		return fltObjStreamlet(() -> spawn().cons(key, value));
	}

	public FltObjStreamlet<V> distinct() {
		return fltObjStreamlet(() -> spawn().distinct());
	}

	public FltObjStreamlet<V> drop(int n) {
		return fltObjStreamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == FltObjStreamlet.class ? Objects.equals(spawn(), ((FltObjStreamlet<?>) object).spawn())
				: false;
	}

	public FltObjStreamlet<V> filter(FltObjPredicate<V> fun) {
		return fltObjStreamlet(() -> spawn().filter(fun));
	}

	public FltObjStreamlet<V> filterKey(FltPredicate fun) {
		return fltObjStreamlet(() -> spawn().filterKey(fun));
	}

	public FltObjStreamlet<V> filterValue(Predicate<V> fun) {
		return fltObjStreamlet(() -> spawn().filterValue(fun));
	}

	public FltObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(FltObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public FltObjStreamlet<List<V>> groupBy() {
		return new FltObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V1> FltObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return new FltObjStreamlet<>(() -> spawn().groupBy(fun));
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

	public Streamlet<Float> keys() {
		return new Streamlet<>(() -> spawn().keys());
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

	public FltObjStreamlet<V> mapKey(Flt_Flt fun) {
		return new FltObjStreamlet<>(() -> spawn().mapKey(fun));
	}

	public <O> Streamlet<O> mapNonNull(FltObj_Obj<V, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public <V1> FltObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return new FltObjStreamlet<>(() -> spawn().mapValue(fun));
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

	public FltObjOutlet<V> outlet() {
		return spawn();
	}

	public Streamlet<FltObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<FltObjStreamlet<V>, FltObjStreamlet<V>> partition(FltObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public FltObjStreamlet<V> reverse() {
		return fltObjStreamlet(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Float, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public FltObjStreamlet<V> skip(int n) {
		return fltObjStreamlet(() -> spawn().skip(n));
	}

	public FltObjStreamlet<V> sort(Comparator<FltObjPair<V>> comparator) {
		return fltObjStreamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> FltObjStreamlet<V> sortBy(FltObj_Obj<V, O> fun) {
		return fltObjStreamlet(() -> spawn().sortBy(fun));
	}

	public FltObjStreamlet<V> sortByKey(Comparator<Float> comparator) {
		return fltObjStreamlet(() -> spawn().sortByKey(comparator));
	}

	public FltObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return fltObjStreamlet(() -> spawn().sortByValue(comparator));
	}

	public FltObjSource<V> source() {
		return spawn().source();
	}

	public FltObjStreamlet<V> take(int n) {
		return fltObjStreamlet(() -> spawn().take(n));
	}

	public FltObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<FltObjPair<V>> toList() {
		return spawn().toList();
	}

	public FltObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public FltObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Float, V> toMultimap() {
		return spawn().toMultimap();
	}

	public ObjFltMap<V> toObjFltMap() {
		return spawn().toObjFltMap();
	}

	public Set<FltObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public FltObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public FltObjPair<V> uniqueResult() {
		FltObjPair<V> pair = spawn().opt();
		if (pair.t0 != FltFunUtil.EMPTYVALUE)
			return pair;
		else
			throw new RuntimeException("no result");
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(FltObj_Obj<V, Streamlet<T>> fun) {
		FltObj_Obj<V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(FltObj_Obj<V, Streamlet2<K1, V1>> fun) {
		FltObj_Obj<V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> FltObjStreamlet<V1> concatMapFltObj_(FltObj_Obj<V, FltObjStreamlet<V1>> fun) {
		FltObj_Obj<V, FltObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).outlet();
		return fltObjStreamlet(() -> FltObjOutlet.of(spawn().concatMapFltObj(bf)));
	}

	private <T> Streamlet<T> map_(FltObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(FltObj_Obj<V, K1> kf, FltObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> FltObjStreamlet<V1> mapFltObj_(FltObj_Flt<V> kf, FltObj_Obj<V, V1> vf) {
		return new FltObjStreamlet<>(() -> spawn().mapFltObj(kf, vf));
	}

	private FltObjOutlet<V> spawn() {
		return in.source();
	}

}
