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

import suite.adt.map.ChrObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.ChrObjPair;
import suite.adt.pair.Pair;
import suite.primitive.ChrFun.ChrObj_Obj;
import suite.primitive.ChrFun.Obj_Chr;
import suite.primitive.ChrObjFunUtil;
import suite.primitive.ChrObj_Chr;
import suite.primitive.ChrPredicate.ChrObjPredicate;
import suite.primitive.ChrPredicate.ChrPredicate_;
import suite.primitive.ChrSource.ChrObjSource;
import suite.primitive.Chr_Chr;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class ChrObjStreamlet<V> implements Iterable<ChrObjPair<V>> {

	private Source<ChrObjOutlet<V>> in;

	@SafeVarargs
	public static <V> ChrObjStreamlet<V> concat(ChrObjStreamlet<V>... streamlets) {
		return chrObjStreamlet(() -> {
			List<ChrObjSource<V>> sources = new ArrayList<>();
			for (ChrObjStreamlet<V> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return ChrObjOutlet.of(ChrObjFunUtil.concat(To.source(sources)));
		});
	}

	private static <V> ChrObjStreamlet<V> chrObjStreamlet(Source<ChrObjOutlet<V>> in) {
		return new ChrObjStreamlet<>(in);
	}

	public ChrObjStreamlet(Source<ChrObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<ChrObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public ChrObjStreamlet<V> append(char key, V value) {
		return chrObjStreamlet(() -> spawn().append(key, value));
	}

	public ChrObjStreamlet<V> closeAtEnd(Closeable c) {
		return chrObjStreamlet(() -> {
			ChrObjOutlet<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<ChrObjOutlet<V>, R> fun) {
		return fun.apply(spawn());
	}

	public char collectAsChar(Obj_Chr<ChrObjOutlet<V>> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(ChrObj_Obj<V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(ChrObj_Obj<V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> ChrObjStreamlet<V1> concatMapChrObj(ChrObj_Obj<V, ChrObjStreamlet<V1>> fun) {
		return concatMapChrObj_(fun);
	}

	public <V1> ChrObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return chrObjStreamlet(() -> ChrObjOutlet.of(spawn().concatMapValue(f)));
	}

	public ChrObjStreamlet<V> cons(char key, V value) {
		return chrObjStreamlet(() -> spawn().cons(key, value));
	}

	public ChrObjStreamlet<V> distinct() {
		return chrObjStreamlet(() -> spawn().distinct());
	}

	public ChrObjStreamlet<V> drop(int n) {
		return chrObjStreamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == ChrObjStreamlet.class ? Objects.equals(spawn(), ((ChrObjStreamlet<?>) object).spawn())
				: false;
	}

	public ChrObjStreamlet<V> filter(ChrObjPredicate<V> fun) {
		return chrObjStreamlet(() -> spawn().filter(fun));
	}

	public ChrObjStreamlet<V> filterKey(ChrPredicate_ fun) {
		return chrObjStreamlet(() -> spawn().filterKey(fun));
	}

	public ChrObjStreamlet<V> filterValue(Predicate<V> fun) {
		return chrObjStreamlet(() -> spawn().filterValue(fun));
	}

	public ChrObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(ChrObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public ChrObjStreamlet<List<V>> groupBy() {
		return new ChrObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V1> ChrObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return new ChrObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(ChrObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(ChrObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	public Streamlet<Character> keys() {
		return new Streamlet<>(() -> spawn().keys());
	}

	public ChrObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(ChrObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Character, V1> map2(ChrObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(ChrObj_Obj<V, K1> kf, ChrObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> ChrObjStreamlet<V1> mapChrObj(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return mapChrObj_(kf, vf);
	}

	public ChrObjStreamlet<V> mapKey(Chr_Chr fun) {
		return new ChrObjStreamlet<>(() -> spawn().mapKey(fun));
	}

	public <O> Streamlet<O> mapNonNull(ChrObj_Obj<V, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public <V1> ChrObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return new ChrObjStreamlet<>(() -> spawn().mapValue(fun));
	}

	public ChrObjPair<V> min(Comparator<ChrObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public ChrObjPair<V> minOrNull(Comparator<ChrObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public ChrObjOutlet<V> out() {
		return spawn();
	}

	public Streamlet<ChrObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<ChrObjStreamlet<V>, ChrObjStreamlet<V>> partition(ChrObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public ChrObjStreamlet<V> reverse() {
		return chrObjStreamlet(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Character, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public ChrObjStreamlet<V> skip(int n) {
		return chrObjStreamlet(() -> spawn().skip(n));
	}

	public ChrObjStreamlet<V> sort(Comparator<ChrObjPair<V>> comparator) {
		return chrObjStreamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> ChrObjStreamlet<V> sortBy(ChrObj_Obj<V, O> fun) {
		return chrObjStreamlet(() -> spawn().sortBy(fun));
	}

	public ChrObjStreamlet<V> sortByKey(Comparator<Character> comparator) {
		return chrObjStreamlet(() -> spawn().sortByKey(comparator));
	}

	public ChrObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return chrObjStreamlet(() -> spawn().sortByValue(comparator));
	}

	public ChrObjSource<V> source() {
		return spawn().source();
	}

	public ChrObjStreamlet<V> take(int n) {
		return chrObjStreamlet(() -> spawn().take(n));
	}

	public ChrObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<ChrObjPair<V>> toList() {
		return spawn().toList();
	}

	public ChrObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public ChrObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Character, V> toMultimap() {
		return spawn().toMultimap();
	}

	public Set<ChrObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public ChrObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public ChrObjPair<V> uniqueResult() {
		return spawn().uniqueResult();
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(ChrObj_Obj<V, Streamlet<T>> fun) {
		ChrObj_Obj<V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(ChrObj_Obj<V, Streamlet2<K1, V1>> fun) {
		ChrObj_Obj<V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).out();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> ChrObjStreamlet<V1> concatMapChrObj_(ChrObj_Obj<V, ChrObjStreamlet<V1>> fun) {
		ChrObj_Obj<V, ChrObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).out();
		return chrObjStreamlet(() -> ChrObjOutlet.of(spawn().concatMapChrObj(bf)));
	}

	private <T> Streamlet<T> map_(ChrObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(ChrObj_Obj<V, K1> kf, ChrObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> ChrObjStreamlet<V1> mapChrObj_(ChrObj_Chr<V> kf, ChrObj_Obj<V, V1> vf) {
		return new ChrObjStreamlet<>(() -> spawn().mapChrObj(kf, vf));
	}

	private ChrObjOutlet<V> spawn() {
		return in.source();
	}

}
