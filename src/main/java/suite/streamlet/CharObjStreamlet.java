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

import suite.adt.map.CharObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.CharObjPair;
import suite.adt.pair.Pair;
import suite.primitive.CharObjFunUtil;
import suite.primitive.CharPrimitiveFun.CharObj_Char;
import suite.primitive.CharPrimitiveFun.CharObj_Obj;
import suite.primitive.CharPrimitiveFun.Char_Char;
import suite.primitive.CharPrimitivePredicate.CharObjPredicate;
import suite.primitive.CharPrimitivePredicate.CharPredicate_;
import suite.primitive.CharPrimitiveSource.CharObjSource;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.primitive.PrimitiveFun.Obj_Double;
import suite.primitive.PrimitiveFun.Obj_Float;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class CharObjStreamlet<V> implements Iterable<CharObjPair<V>> {

	private Source<CharObjOutlet<V>> in;

	@SafeVarargs
	public static <V> CharObjStreamlet<V> concat(CharObjStreamlet<V>... streamlets) {
		return charbjStreamlet(() -> {
			List<CharObjSource<V>> sources = new ArrayList<>();
			for (CharObjStreamlet<V> streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return CharObjOutlet.of(CharObjFunUtil.concat(To.source(sources)));
		});
	}

	private static <V> CharObjStreamlet<V> charbjStreamlet(Source<CharObjOutlet<V>> in) {
		return new CharObjStreamlet<>(in);
	}

	public CharObjStreamlet(Source<CharObjOutlet<V>> in) {
		this.in = in;
	}

	@Override
	public Iterator<CharObjPair<V>> iterator() {
		return spawn().iterator();
	}

	public CharObjStreamlet<V> append(char key, V value) {
		return charbjStreamlet(() -> spawn().append(key, value));
	}

	public CharObjStreamlet<V> closeAtEnd(Closeable c) {
		return charbjStreamlet(() -> {
			CharObjOutlet<V> in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<CharObjOutlet<V>, R> fun) {
		return fun.apply(spawn());
	}

	public double collectAsDouble(Obj_Double<CharObjOutlet<V>> fun) {
		return fun.applyAsDouble(spawn());
	}

	public float collectAsFloat(Obj_Float<CharObjOutlet<V>> fun) {
		return fun.applyAsFloat(spawn());
	}

	public int collectAsInt(Obj_Int<CharObjOutlet<V>> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(ObjObj_Obj<Character, V, Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K1, V1> Streamlet2<K1, V1> concatMap2(ObjObj_Obj<Character, V, Streamlet2<K1, V1>> fun) {
		return concatMap2_(fun);
	}

	public <V1> CharObjStreamlet<V1> concatMapCharObj(ObjObj_Obj<Character, V, CharObjStreamlet<V1>> fun) {
		return concatMapCharObj_(fun);
	}

	public <V1> CharObjStreamlet<V1> concatMapValue(Fun<V, Streamlet<V1>> fun) {
		Fun<V, Outlet<V1>> f = v -> fun.apply(v).outlet();
		return charbjStreamlet(() -> CharObjOutlet.of(spawn().concatMapValue(f)));
	}

	public CharObjStreamlet<V> cons(char key, V value) {
		return charbjStreamlet(() -> spawn().cons(key, value));
	}

	public CharObjStreamlet<V> distinct() {
		return charbjStreamlet(() -> spawn().distinct());
	}

	public CharObjStreamlet<V> drop(int n) {
		return charbjStreamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == CharObjStreamlet.class ? Objects.equals(spawn(), ((CharObjStreamlet<?>) object).spawn())
				: false;
	}

	public CharObjStreamlet<V> filter(CharObjPredicate<V> fun) {
		return charbjStreamlet(() -> spawn().filter(fun));
	}

	public CharObjStreamlet<V> filterKey(CharPredicate_ fun) {
		return charbjStreamlet(() -> spawn().filterKey(fun));
	}

	public CharObjStreamlet<V> filterValue(Predicate<V> fun) {
		return charbjStreamlet(() -> spawn().filterValue(fun));
	}

	public CharObjPair<V> first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(CharObj_Obj<V, Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public CharObjStreamlet<List<V>> groupBy() {
		return new CharObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V1> CharObjStreamlet<V1> groupBy(Fun<Streamlet<V>, V1> fun) {
		return new CharObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public boolean isAll(CharObjPredicate<V> pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(CharObjPredicate<V> pred) {
		return spawn().isAny(pred);
	}

	public Streamlet<Character> keys() {
		return new Streamlet<>(() -> spawn().keys());
	}

	public CharObjPair<V> last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(CharObj_Obj<V, O> fun) {
		return map_(fun);
	}

	public <V1> Streamlet2<Character, V1> map2(CharObj_Obj<V, V1> vf) {
		return map2_((k, v) -> k, vf);
	}

	public <K1, V1> Streamlet2<K1, V1> map2(CharObj_Obj<V, K1> kf, CharObj_Obj<V, V1> vf) {
		return map2_(kf, vf);
	}

	public <V1> CharObjStreamlet<V1> mapCharObj(CharObj_Char<V> kf, CharObj_Obj<V, V1> vf) {
		return mapCharObj_(kf, vf);
	}

	public CharObjStreamlet<V> mapKey(Char_Char fun) {
		return new CharObjStreamlet<>(() -> spawn().mapKey(fun));
	}

	public <O> Streamlet<O> mapNonNull(CharObj_Obj<V, O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public <V1> CharObjStreamlet<V1> mapValue(Fun<V, V1> fun) {
		return new CharObjStreamlet<>(() -> spawn().mapValue(fun));
	}

	public CharObjPair<V> min(Comparator<CharObjPair<V>> comparator) {
		return spawn().min(comparator);
	}

	public CharObjPair<V> minOrNull(Comparator<CharObjPair<V>> comparator) {
		return spawn().minOrNull(comparator);
	}

	public CharObjOutlet<V> out() {
		return spawn();
	}

	public Streamlet<CharObjPair<V>> pairs() {
		return new Streamlet<>(() -> spawn().pairs());
	}

	public Pair<CharObjStreamlet<V>, CharObjStreamlet<V>> partition(CharObjPredicate<V> pred) {
		return Pair.of(filter(pred), filter((k, v) -> !pred.test(k, v)));
	}

	public CharObjStreamlet<V> reverse() {
		return charbjStreamlet(() -> spawn().reverse());
	}

	public void sink(BiConsumer<Character, V> sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().size();
	}

	public CharObjStreamlet<V> skip(int n) {
		return charbjStreamlet(() -> spawn().skip(n));
	}

	public CharObjStreamlet<V> sort(Comparator<CharObjPair<V>> comparator) {
		return charbjStreamlet(() -> spawn().sort(comparator));
	}

	public <O extends Comparable<? super O>> CharObjStreamlet<V> sortBy(CharObj_Obj<V, O> fun) {
		return charbjStreamlet(() -> spawn().sortBy(fun));
	}

	public CharObjStreamlet<V> sortByKey(Comparator<Character> comparator) {
		return charbjStreamlet(() -> spawn().sortByKey(comparator));
	}

	public CharObjStreamlet<V> sortByValue(Comparator<V> comparator) {
		return charbjStreamlet(() -> spawn().sortByValue(comparator));
	}

	public CharObjSource<V> source() {
		return spawn().source();
	}

	public CharObjStreamlet<V> take(int n) {
		return charbjStreamlet(() -> spawn().take(n));
	}

	public CharObjPair<V>[] toArray() {
		return spawn().toArray();
	}

	public List<CharObjPair<V>> toList() {
		return spawn().toList();
	}

	public CharObjMap<List<V>> toListMap() {
		return spawn().toListMap();
	}

	public CharObjMap<V> toMap() {
		return spawn().toMap();
	}

	public ListMultimap<Character, V> toMultimap() {
		return spawn().toMultimap();
	}

	public Set<CharObjPair<V>> toSet() {
		return spawn().toSet();
	}

	public CharObjMap<Set<V>> toSetMap() {
		return spawn().toSetMap();
	}

	public CharObjPair<V> uniqueResult() {
		return spawn().uniqueResult();
	}

	public Streamlet<V> values() {
		return new Streamlet<>(() -> spawn().values());
	}

	private <T> Streamlet<T> concatMap_(ObjObj_Obj<Character, V, Streamlet<T>> fun) {
		ObjObj_Obj<Character, V, Outlet<T>> bf = (k, v) -> fun.apply(k, v).outlet();
		return new Streamlet<>(() -> Outlet.of(spawn().concatMap(bf)));
	}

	private <V1, K1> Streamlet2<K1, V1> concatMap2_(ObjObj_Obj<Character, V, Streamlet2<K1, V1>> fun) {
		ObjObj_Obj<Character, V, Outlet2<K1, V1>> bf = (k, v) -> fun.apply(k, v).out();
		return new Streamlet2<>(() -> Outlet2.of(spawn().concatMap2(bf)));
	}

	private <V1> CharObjStreamlet<V1> concatMapCharObj_(ObjObj_Obj<Character, V, CharObjStreamlet<V1>> fun) {
		ObjObj_Obj<Character, V, CharObjOutlet<V1>> bf = (k, v) -> fun.apply(k, v).out();
		return charbjStreamlet(() -> CharObjOutlet.of(spawn().concatMapCharObj(bf)));
	}

	private <T> Streamlet<T> map_(CharObj_Obj<V, T> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K1, V1> Streamlet2<K1, V1> map2_(CharObj_Obj<V, K1> kf, CharObj_Obj<V, V1> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private <V1> CharObjStreamlet<V1> mapCharObj_(CharObj_Char<V> kf, CharObj_Obj<V, V1> vf) {
		return new CharObjStreamlet<>(() -> spawn().mapCharObj(kf, vf));
	}

	private CharObjOutlet<V> spawn() {
		return in.source();
	}

}
