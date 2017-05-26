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

import suite.adt.map.ListMultimap;
import suite.adt.map.ShtObjMap;
import suite.adt.pair.Pair;
import suite.adt.pair.ShtObjPair;
import suite.primitive.PrimitiveFun.Obj_Dbl;
import suite.primitive.ShtFun.Obj_Sht;
import suite.primitive.ShtFun.ShtObj_Obj;
import suite.primitive.ShtObjFunUtil;
import suite.primitive.ShtObj_Sht;
import suite.primitive.ShtPredicate.ShtObjPredicate;
import suite.primitive.ShtPredicate.ShtPredicate_;
import suite.primitive.ShtSource.ShtObjSource;
import suite.primitive.Sht_Sht;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class ShtObjStreamlet<V> implements Iterable<ShtObjPair<V>> {

	private Source<ShtObjOutlet<V>> in;

	@SafeVarargs
	public static <V> ShtObjStreamlet<V> concat(ShtObjStreamlet<V>... streamlets) {
		return shtObjStreamlet(() -> {
			List<ShtObjSource<V>> sources = new ArrayList<>();
			for (ShtObjStreamlet<V> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return ShtObjOutlet.of(ShtObjFunUtil.concat(To.source(sources)));
		});
	}

	private static <V> ShtObjStreamlet<V> shtObjStreamlet(Source<ShtObjOutlet<V>> in) {
		return new ShtObjStreamlet<>(in);
	}

	public ShtObjStreamlet(Source<ShtObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<ShtObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public ShtObjStreamlet<V> append(short key, V value) {
		return shtObjStreamlet(() -> spawn().append(key, value));
	}

	public ShtObjStreamlet<V> closeAtEnd(Closeable c) {
		return shtObjStreamlet(() -> {
			ShtObjOutlet<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<ShtObjOutlet<V>, R> fun) {
		return fun.apply(spawn());
	}

	public double collectAsDouble(Obj_Dbl<ShtObjOutlet<V>> fun) {
		return fun.applyAsDouble(spawn());
	}

	public short collectAsShort(Obj_Sht<ShtObjOutlet<V>> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(ShtObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(ShtObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> ShtObjStreamlet<V1> concatMapShtObj(ShtObj_Obj<V, ShtObjStreamlet<V1>> fun) {
		return concatMapShtObj_(fun);
	}

	public <V1> ShtObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return shtObjStreamlet(() -> ShtObjOutlet.of(spawn().concatMapValue(f)));
	}

	public ShtObjStreamlet<V> cons(short key, V value) {
		return shtObjStreamlet(() -> spawn().cons(key, value));
	}

	public ShtObjStreamlet<V> distinct() {
		return shtObjStreamlet(() -> spawn().distinct());
	}

	public ShtObjStreamlet<V> drop(int n) {
		return shtObjStreamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == ShtObjStreamlet.class ? Objects.equals(spawn(), ((ShtObjStreamlet<?>) object).spawn())
				: false;
	}

	public ShtObjStreamlet<V> filter(ShtObjPredicate<V> fun) {
		return shtObjStreamlet(() -> spawn().filter(fun));
	}

	public ShtObjStreamlet<V> filterKey(ShtPredicate_ fun) {
		return shtObjStreamlet(() -> spawn().filterKey(fun));
	}

	public ShtObjStreamlet<V> filterValue(Predicate<V> fun) {
		return shtObjStreamlet(() -> spawn().filterValue(fun));
	}

	public ShtObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(ShtObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public ShtObjStreamlet<List<V>> groupBy() {
		return new ShtObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V1> ShtObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return new ShtObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(ShtObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(ShtObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	public Streamlet<Short> keys() {
		return new Streamlet<>(() -> spawn().keys());
	}

	public ShtObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(ShtObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Short, V1> map2(ShtObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(ShtObj_Obj<V, K1> kf, ShtObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> ShtObjStreamlet<V1> mapShtObj(ShtObj_Sht<V> kf, ShtObj_Obj<V, V1> vf) {
		return mapShtObj_(kf, vf);
	}

	public ShtObjStreamlet<V> mapKey(Sht_Sht fun) {
		return new ShtObjStreamlet<>(() -> spawn().mapKey(fun));
	}

	public <O> Streamlet<O> mapNonNull(ShtObj_Obj<V, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public <V1> ShtObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return new ShtObjStreamlet<>(() -> spawn().mapValue(fun));
	}

	public ShtObjPair<V> min(Comparator<ShtObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public ShtObjPair<V> minOrNull(Comparator<ShtObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public ShtObjOutlet<V> out() {
		return spawn();
	}

	public Streamlet<ShtObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<ShtObjStreamlet<V>, ShtObjStreamlet<V>> partition(ShtObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public ShtObjStreamlet<V> reverse() {
		return shtObjStreamlet(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Short, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public ShtObjStreamlet<V> skip(int n) {
		return shtObjStreamlet(() -> spawn().skip(n));
	}

	public ShtObjStreamlet<V> sort(Comparator<ShtObjPair<V>> comparator) {
		return shtObjStreamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> ShtObjStreamlet<V> sortBy(ShtObj_Obj<V, O> fun) {
		return shtObjStreamlet(() -> spawn().sortBy(fun));
	}

	public ShtObjStreamlet<V> sortByKey(Comparator<Short> comparator) {
		return shtObjStreamlet(() -> spawn().sortByKey(comparator));
	}

	public ShtObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return shtObjStreamlet(() -> spawn().sortByValue(comparator));
	}

	public ShtObjSource<V> source() {
		return spawn().source();
	}

	public ShtObjStreamlet<V> take(int n) {
		return shtObjStreamlet(() -> spawn().take(n));
	}

	public ShtObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<ShtObjPair<V>> toList() {
		return spawn().toList();
	}

	public ShtObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public ShtObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Short, V> toMultimap() {
		return spawn().toMultimap();
	}

	public Set<ShtObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public ShtObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public ShtObjPair<V> uniqueResult() {
		return spawn().uniqueResult();
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(ShtObj_Obj<V, Streamlet<T>> fun) {
		ShtObj_Obj<V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(ShtObj_Obj<V, Streamlet2<K1, V1>> fun) {
		ShtObj_Obj<V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).out();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> ShtObjStreamlet<V1> concatMapShtObj_(ShtObj_Obj<V, ShtObjStreamlet<V1>> fun) {
		ShtObj_Obj<V, ShtObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).out();
		return shtObjStreamlet(() -> ShtObjOutlet.of(spawn().concatMapShtObj(bf)));
	}

	private <T> Streamlet<T> map_(ShtObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(ShtObj_Obj<V, K1> kf, ShtObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> ShtObjStreamlet<V1> mapShtObj_(ShtObj_Sht<V> kf, ShtObj_Obj<V, V1> vf) {
		return new ShtObjStreamlet<>(() -> spawn().mapShtObj(kf, vf));
	}

	private ShtObjOutlet<V> spawn() {
		return in.source();
	}

}
