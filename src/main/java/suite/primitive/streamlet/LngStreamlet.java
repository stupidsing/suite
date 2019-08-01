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
import primal.primitive.LngOpt;
import primal.primitive.LngPrim.LngComparator;
import primal.primitive.LngPrim.LngObj_Obj;
import primal.primitive.LngPrim.LngSink;
import primal.primitive.LngPrim.LngSource;
import primal.primitive.LngPrim.LngTest;
import primal.primitive.LngPrim.Lng_Lng;
import primal.primitive.LngPrim.Lng_Obj;
import suite.adt.map.ListMultimap;
import suite.primitive.Longs;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.Longs_;
import suite.primitive.adt.map.LngObjMap;
import suite.primitive.adt.map.ObjLngMap;
import suite.primitive.adt.set.LngSet;
import suite.streamlet.Puller;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;

public class LngStreamlet implements StreamletDefaults<Long, LngPuller> {

	private Source<LngPuller> in;

	private static LngStreamlet streamlet(Source<LngPuller> in) {
		return new LngStreamlet(in);
	}

	public LngStreamlet(Source<LngPuller> in) {
		this.in = in;
	}

	public <R> R apply(Fun<LngStreamlet, R> fun) {
		return fun.apply(this);
	}

	public long average() {
		return spawn().average();
	}

	public Streamlet<LngPuller> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public LngStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <O> Streamlet<O> concatMap(Lng_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Lng_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public LngStreamlet cons(long c) {
		return streamlet(() -> spawn().cons(c));
	}

	public LngStreamlet collect() {
		Longs longs = toList_().toLongs();
		return Longs_.of(longs.cs, longs.start, longs.end, 1);
	}

	public <U, O> Streamlet<O> cross(Streamlet<U> st1, LngObj_Obj<U, O> fun) {
		return new Streamlet<>(() -> spawn().cross(st1.toList(), fun));
	}

	public LngStreamlet distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public LngStreamlet drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Get.clazz(object) == LngStreamlet.class ? Equals.ab(spawn(), ((LngStreamlet) object).spawn()) : false;
	}

	public LngStreamlet filter(LngTest fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public long first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(Lng_Obj<Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, LngObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<LngStreamlet, U> fork0, Fun<LngStreamlet, V> fork1, Fun2<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> LngObjStreamlet<LongsBuilder> groupBy() {
		return new LngObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V> LngObjStreamlet<V> groupBy(Fun<Longs, V> fun) {
		return new LngObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public LngObjStreamlet<Integer> index() {
		return new LngObjStreamlet<>(() -> spawn().index());
	}

	public boolean isAll(LngTest pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(LngTest pred) {
		return spawn().isAny(pred);
	}

	@Override
	public Iterator<Long> iterator() {
		return spawn().iterator();
	}

	public <O> Streamlet2<Long, O> join2(Streamlet<O> streamlet) {
		return concatMap2_(t -> streamlet.map2(v -> t, v -> v));
	}

	public long last() {
		return spawn().last();
	}

	public <O> Streamlet<O> map(Lng_Obj<O> fun) {
		return map_(fun);
	}

	public <K, V> Streamlet2<K, V> map2(Lng_Obj<K> kf, Lng_Obj<V> vf) {
		return map2_(kf, vf);
	}

	public LngStreamlet mapLng(Lng_Lng fun) {
		return streamlet(() -> spawn().mapLng(fun));
	}

	public <K, V> LngObjStreamlet<V> mapLngObj(Lng_Obj<V> fun0) {
		return new LngObjStreamlet<>(() -> spawn().mapLngObj(fun0));
	}

	public long max() {
		return spawn().max();
	}

	public long min() {
		return spawn().min();
	}

	public long min(LngComparator comparator) {
		return spawn().min(comparator);
	}

	public long minOrEmpty(LngComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public LngOpt opt() {
		return spawn().opt();
	}

	public Pair<LngStreamlet, LngStreamlet> partition(LngTest pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public LngPuller puller() {
		return spawn();
	}

	public LngStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(LngSink sink) {
		spawn().sink(sink);
	}

	public LngStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public LngStreamlet snoc(long c) {
		return streamlet(() -> spawn().snoc(c));
	}

	public LngStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public LngSource source() {
		return spawn().source();
	}

	public long sum() {
		return spawn().sum();
	}

	public LngStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public long[] toArray() {
		return spawn().toArray();
	}

	public LongsBuilder toList() {
		return toList_();
	}

	public <K> LngObjMap<LongsBuilder> toListMap() {
		return spawn().toListMap();
	}

	public <K> LngObjMap<LongsBuilder> toListMap(Lng_Lng valueFun) {
		return spawn().toListMap(valueFun);
	}

	public <K> ObjLngMap<K> toMap(Lng_Obj<K> keyFun) {
		return spawn().toMap(keyFun);
	}

	public <K, V> Map<K, V> toMap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Long> toMultimap(Lng_Obj<K> keyFun) {
		return spawn().toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return spawn().toMultimap(keyFun, valueFun);
	}

	public LngSet toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public long uniqueResult() {
		return spawn().opt().get();
	}

	public <U, V> Streamlet<V> zip(Iterable<U> list1, LngObj_Obj<U, V> fun) {
		return new Streamlet<>(() -> spawn().zip(Puller.of(list1), fun));
	}

	private <O> Streamlet<O> concatMap_(Lng_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).puller()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Lng_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).puller()));
	}

	private <O> Streamlet<O> map_(Lng_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Lng_Obj<K> kf, Lng_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private LongsBuilder toList_() {
		return spawn().toList();
	}

	private LngPuller spawn() {
		return in.g();
	}

}
