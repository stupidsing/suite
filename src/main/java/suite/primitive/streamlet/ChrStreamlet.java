package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import suite.adt.map.ListMultimap;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.Chars_;
import suite.primitive.ChrOpt;
import suite.primitive.ChrPrimitives.ChrComparator;
import suite.primitive.ChrPrimitives.ChrObj_Obj;
import suite.primitive.ChrPrimitives.ChrSink;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.ChrPrimitives.ChrTest;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.primitive.Chr_Chr;
import suite.primitive.adt.map.ChrObjMap;
import suite.primitive.adt.map.ObjChrMap;
import suite.primitive.adt.set.ChrSet;
import suite.streamlet.Puller;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;

public class ChrStreamlet implements StreamletDefaults<Character, ChrPuller> {

	private Source<ChrPuller> in;

	private static ChrStreamlet streamlet(Source<ChrPuller> in) {
		return new ChrStreamlet(in);
	}

	public ChrStreamlet(Source<ChrPuller> in) {
		this.in = in;
	}

	public <R> R apply(Fun<ChrStreamlet, R> fun) {
		return fun.apply(this);
	}

	public char average() {
		return spawn().average();
	}

	public Streamlet<ChrPuller> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public ChrStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(Chr_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Chr_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public ChrStreamlet cons(char c) {
		return streamlet(() -> spawn().cons(c));
	}

	public ChrStreamlet collect() {
		Chars chars = toList_().toChars();
		return Chars_.of(chars.cs, chars.start, chars.end, 1);
	}

	public <U, O> Streamlet<O> cross(Streamlet<U> st1, ChrObj_Obj<U, O> fun) {
		return new Streamlet<>(() -> spawn().cross(st1.toList(), fun));
	}

	public ChrStreamlet distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public ChrStreamlet drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == ChrStreamlet.class ? Equals.ab(spawn(), ((ChrStreamlet) object).spawn()) : false;
	}

	public ChrStreamlet filter(ChrTest fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public char first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(Chr_Obj<Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, ChrObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<ChrStreamlet, U> fork0, Fun<ChrStreamlet, V> fork1, Fun2<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> ChrObjStreamlet<CharsBuilder> groupBy() {
		return new ChrObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V> ChrObjStreamlet<V> groupBy(Fun<Chars, V> fun) {
		return new ChrObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public ChrObjStreamlet<Integer> index() {
		return new ChrObjStreamlet<>(() -> spawn().index());
	}

	public boolean isAll(ChrTest pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(ChrTest pred) {
		return spawn().isAny(pred);
	}

	@Override
	public Iterator<Character> iterator() {
		return spawn().iterator();
	}

	public <O> Streamlet2<Character, O> join2(Streamlet<O> streamlet) {
		return concatMap2_(t -> streamlet.map2(v -> t, v -> v));
	}

	public char last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(Chr_Obj<O> fun) {
		return map_(fun);
	}

	public <K, V> Streamlet2<K, V> map2(Chr_Obj<K> kf, Chr_Obj<V> vf) {
		return map2_(kf, vf);
	}

	public ChrStreamlet mapChr(Chr_Chr fun) {
		return streamlet(() -> spawn().mapChr(fun));
	}

	public <K, V> ChrObjStreamlet<V> mapChrObj(Chr_Obj<V> fun0) {
		return new ChrObjStreamlet<>(() -> spawn().mapChrObj(fun0));
	}

	public char max() {
		return spawn().max();
	}

	public char min() {
		return spawn().min();
	}

	public char min(ChrComparator comparator) {
		return spawn().min(comparator);
	}

	public char minOrEmpty(ChrComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public ChrOpt opt() {
		return spawn().opt();
	}

	public Pair<ChrStreamlet, ChrStreamlet> partition(ChrTest pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public ChrPuller puller() {
		return spawn();
	}

	public ChrStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(ChrSink sink) {
		spawn().sink(sink);
	}

	public ChrStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public ChrStreamlet snoc(char c) {
		return streamlet(() -> spawn().snoc(c));
	}

	public ChrStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public ChrSource source() {
		return spawn().source();
	}

	public char sum() {
		return spawn().sum();
	}

	public ChrStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public char[] toArray() {
		return spawn().toArray();
	}

	public CharsBuilder toList() {
		return toList_();
	}

	public <K> ChrObjMap<CharsBuilder> toListMap() {
		return spawn().toListMap();
	}

	public <K> ChrObjMap<CharsBuilder> toListMap(Chr_Chr valueFun) {
		return spawn().toListMap(valueFun);
	}

	public <K> ObjChrMap<K> toMap(Chr_Obj<K> keyFun) {
		return spawn().toMap(keyFun);
	}

	public <K, V> Map<K, V> toMap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Character> toMultimap(Chr_Obj<K> keyFun) {
		return spawn().toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return spawn().toMultimap(keyFun, valueFun);
	}

	public ChrSet toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public char uniqueResult() {
		return spawn().opt().get();
	}

	public <U, V> Streamlet<V> zip(Iterable<U> list1, ChrObj_Obj<U, V> fun) {
		return new Streamlet<>(() -> spawn().zip(Puller.of(list1), fun));
	}

	private <O> Streamlet<O> concatMap_(Chr_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).puller()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Chr_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).puller()));
	}

	private <O> Streamlet<O> map_(Chr_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Chr_Obj<K> kf, Chr_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private CharsBuilder toList_() {
		return spawn().toList();
	}

	private ChrPuller spawn() {
		return in.g();
	}

}
