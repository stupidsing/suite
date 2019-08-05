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
import primal.primitive.ChrOpt;
import primal.primitive.ChrPrim;
import primal.primitive.ChrPrim.ChrComparator;
import primal.primitive.ChrPrim.ChrObjSource;
import primal.primitive.ChrPrim.ChrObj_Obj;
import primal.primitive.ChrPrim.ChrPred;
import primal.primitive.ChrPrim.ChrSink;
import primal.primitive.ChrPrim.ChrSource;
import primal.primitive.ChrPrim.Chr_Obj;
import primal.primitive.Chr_Chr;
import primal.primitive.adt.pair.ChrObjPair;
import primal.primitive.puller.ChrObjPuller;
import primal.primitive.puller.ChrPuller;
import primal.puller.Puller;
import primal.streamlet.StreamletDefaults;
import suite.adt.map.ListMultimap;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.Chars_;
import suite.primitive.adt.map.ChrObjMap;
import suite.primitive.adt.map.ObjChrMap;
import suite.primitive.adt.set.ChrSet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;

public class ChrStreamlet implements StreamletDefaults<Character, ChrOpt, ChrPred, ChrPuller, ChrSink, ChrSource> {

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
		var chars = toList_();
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

	public ChrStreamlet filter(ChrPred fun) {
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
		return new ChrObjStreamlet<>(this::groupBy_);
	}

	public <V> ChrObjStreamlet<V> groupBy(Fun<Chars, V> fun) {
		return new ChrObjStreamlet<>(() -> groupBy_().mapValue(list -> fun.apply(list.toChars())));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public ChrObjStreamlet<Integer> index() {
		return new ChrObjStreamlet<>(() -> ChrObjPuller.of(new ChrObjSource<>() {
			private ChrPuller puller = spawn();
			private int i = 0;

			public boolean source2(ChrObjPair<Integer> pair) {
				var c = puller.pull();
				if (c != ChrPrim.EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		}));
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
		return spawn().min((c0, c1) -> Character.compare(c1, c0));
	}

	public char min() {
		return spawn().min((c0, c1) -> Character.compare(c0, c1));
	}

	public char min(ChrComparator comparator) {
		return spawn().min(comparator);
	}

	public char minOrEmpty(ChrComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public Pair<ChrStreamlet, ChrStreamlet> partition(ChrPred pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public ChrPuller puller() {
		return spawn();
	}

	public ChrStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
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

	public char sum() {
		return spawn().sum();
	}

	public ChrStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public char[] toArray() {
		return spawn().toArray();
	}

	public Chars toList() {
		return toList_();
	}

	public <K> ChrObjMap<CharsBuilder> toListMap() {
		return toListMap_();
	}

	public <K> ChrObjMap<CharsBuilder> toListMap(Chr_Chr valueFun) {
		return toListMap_(valueFun);
	}

	public <K> ObjChrMap<K> toMap(Chr_Obj<K> kf0) {
		var puller = spawn();
		var kf1 = kf0.rethrow();
		var map = new ObjChrMap<K>();
		char c;
		while ((c = puller.pull()) != ChrPrim.EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Character> toMultimap(Chr_Obj<K> keyFun) {
		return toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public ChrSet toSet() {
		var puller = spawn();
		var set = new ChrSet();
		char c;
		while ((c = puller.pull()) != ChrPrim.EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Chr_Obj<K> keyFun, Chr_Obj<V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
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

	private <V> ChrObjPuller<CharsBuilder> groupBy_() {
		return ChrObjPuller.of(toListMap_().source());
	}

	private <O> Streamlet<O> map_(Chr_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Chr_Obj<K> kf, Chr_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private Chars toList_() {
		var list = spawn().toList();
		return Chars.of(list.cs, 0, list.size);
	}

	private ChrObjMap<CharsBuilder> toListMap_() {
		return toListMap_(value -> value);
	}

	private ChrObjMap<CharsBuilder> toListMap_(Chr_Chr valueFun) {
		var puller = spawn();
		var map = new ChrObjMap<CharsBuilder>();
		char c;
		while ((c = puller.pull()) != ChrPrim.EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new CharsBuilder()).append(valueFun.apply(c));
		return map;
	}

	private ChrPuller spawn() {
		return in.g();
	}

}
