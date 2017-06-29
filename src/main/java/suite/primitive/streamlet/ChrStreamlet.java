package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.Chars;
import suite.primitive.Chars.ChrsBuilder;
import suite.primitive.ChrFunUtil;
import suite.primitive.ChrMutable;
import suite.primitive.ChrOpt;
import suite.primitive.ChrPrimitives.ChrComparator;
import suite.primitive.ChrPrimitives.ChrObj_Obj;
import suite.primitive.ChrPrimitives.ChrPredicate;
import suite.primitive.ChrPrimitives.ChrSink;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.primitive.ChrPrimitives.Obj_Chr;
import suite.primitive.Chr_Chr;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.primitive.adt.map.ChrObjMap;
import suite.streamlet.Outlet;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class ChrStreamlet implements Iterable<Character> {

	private Source<ChrOutlet> in;

	@SafeVarargs
	public static ChrStreamlet concat(ChrStreamlet... streamlets) {
		return streamlet(() -> {
			List<ChrSource> sources = new ArrayList<>();
			for (ChrStreamlet streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return ChrOutlet.of(ChrFunUtil.concat(To.source(sources)));
		});
	}

	public static <T> Fun<Outlet<T>, ChrStreamlet> from(Obj_Chr<T> fun0) {
		Obj_Chr<T> fun1 = fun0.rethrow();
		return ts -> {
			ChrsBuilder cb = new ChrsBuilder();
			T t;
			while ((t = ts.next()) != null)
				cb.append(fun1.apply(t));
			return cb.toChars().streamlet();
		};
	}

	public static ChrStreamlet from(char[] ts) {
		return streamlet(() -> ChrOutlet.of(ts));
	}

	public static ChrStreamlet range(char e) {
		return range((char) 0, e);
	}

	public static ChrStreamlet range(char s, char e) {
		return streamlet(() -> {
			ChrMutable m = ChrMutable.of(s);
			return ChrOutlet.of(() -> {
				char c = m.increment();
				return c < e ? c : ChrFunUtil.EMPTYVALUE;
			});
		});
	}

	private static ChrStreamlet streamlet(Source<ChrOutlet> in) {
		return new ChrStreamlet(in);
	}

	public ChrStreamlet(Source<ChrOutlet> in) {
		this.in = in;
	}

	@Override
	public Iterator<Character> iterator() {
		return spawn().iterator();
	}

	public ChrStreamlet append(char t) {
		return streamlet(() -> spawn().append(t));
	}

	public Streamlet<ChrOutlet> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public ChrStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			ChrOutlet in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<ChrOutlet, R> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(Chr_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Chr_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public ChrStreamlet cons(char t) {
		return streamlet(() -> spawn().cons(t));
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
		return Object_.clazz(object) == ChrStreamlet.class ? Objects.equals(spawn(), ((ChrStreamlet) object).spawn()) : false;
	}

	public ChrStreamlet filter(ChrPredicate fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public char first() {
		return spawn().first();
	}

	public ChrStreamlet flatMap(Chr_Obj<Iterable<Character>> fun) {
		return streamlet(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, ChrObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<ChrStreamlet, U> fork0, Fun<ChrStreamlet, V> fork1, ObjObj_Obj<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> ChrObjStreamlet<ChrsBuilder> groupBy() {
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

	public boolean isAll(ChrPredicate pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(ChrPredicate pred) {
		return spawn().isAny(pred);
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
		return new ChrStreamlet(() -> spawn().mapChr(fun));
	}

	public <K, V> ChrObjStreamlet<V> mapChrObj(Chr_Obj<V> fun0) {
		return new ChrObjStreamlet<>(() -> spawn().mapChrObj(fun0));
	}

	public <O> Streamlet<O> mapNonNull(Chr_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public ChrStreamlet memoize() {
		Chars list = toList().toChars();
		return streamlet(() -> ChrOutlet.of(list));
	}

	public char min(ChrComparator comparator) {
		return spawn().min(comparator);
	}

	public char minOrNull(ChrComparator comparator) {
		return spawn().minOrNull(comparator);
	}

	public ChrOpt opt() {
		return spawn().opt();
	}

	public ChrOutlet outlet() {
		return spawn();
	}

	public Pair<ChrStreamlet, ChrStreamlet> partition(ChrPredicate pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public ChrStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(ChrSink sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().count();
	}

	public ChrStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public ChrStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public ChrSource source() {
		return spawn().source();
	}

	public ChrStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public char[] toArray() {
		return spawn().toArray();
	}

	public ChrsBuilder toList() {
		return spawn().toList();
	}

	public <K> ChrObjMap<ChrsBuilder> toListMap() {
		return spawn().toListMap();
	}

	public <K> ChrObjMap<ChrsBuilder> toListMap(Chr_Chr valueFun) {
		return spawn().toListMap(valueFun);
	}

	public <K> Map<K, Character> toMap(Chr_Obj<K> keyFun) {
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

	public Set<Character> toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public char uniqueResult() {
		return spawn().opt().get();
	}

	private <O> Streamlet<O> concatMap_(Chr_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).outlet()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Chr_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).out()));
	}

	private <O> Streamlet<O> map_(Chr_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Chr_Obj<K> kf, Chr_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private ChrOutlet spawn() {
		return in.source();
	}

}
