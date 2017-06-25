package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.map.LngObjMap;
import suite.adt.pair.Pair;
import suite.primitive.LngFunUtil;
import suite.primitive.LngMutable;
import suite.primitive.LngOpt;
import suite.primitive.LngPrimitives.LngComparator;
import suite.primitive.LngPrimitives.LngObj_Obj;
import suite.primitive.LngPrimitives.LngPredicate;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.LngPrimitives.Lng_Obj;
import suite.primitive.Lng_Lng;
import suite.primitive.Longs;
import suite.primitive.Longs.LongsBuilder;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class LngStreamlet implements Iterable<Long> {

	private Source<LngOutlet> in;

	@SafeVarargs
	public static LngStreamlet concat(LngStreamlet... streamlets) {
		return streamlet(() -> {
			List<LngSource> sources = new ArrayList<>();
			for (LngStreamlet streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return LngOutlet.of(LngFunUtil.concat(To.source(sources)));
		});
	}

	public static LngStreamlet range(long e) {
		return range((long) 0, e);
	}

	public static LngStreamlet range(long s, long e) {
		return streamlet(() -> {
			LngMutable m = LngMutable.of(s);
			return LngOutlet.of(() -> {
				long c = m.increment();
				return c < e ? c : LngFunUtil.EMPTYVALUE;
			});
		});
	}

	private static LngStreamlet streamlet(Source<LngOutlet> in) {
		return new LngStreamlet(in);
	}

	public LngStreamlet(Source<LngOutlet> in) {
		this.in = in;
	}

	@Override
	public Iterator<Long> iterator() {
		return spawn().iterator();
	}

	public LngStreamlet append(long t) {
		return streamlet(() -> spawn().append(t));
	}

	public Streamlet<LngOutlet> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public LngStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			LngOutlet in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<LngOutlet, R> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(Lng_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Lng_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public LngStreamlet cons(long t) {
		return streamlet(() -> spawn().cons(t));
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
		return Object_.clazz(object) == LngStreamlet.class ? Objects.equals(spawn(), ((LngStreamlet) object).spawn()) : false;
	}

	public LngStreamlet filter(LngPredicate fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public long first() {
		return spawn().first();
	}

	public LngStreamlet flatMap(Lng_Obj<Iterable<Long>> fun) {
		return streamlet(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, LngObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<LngStreamlet, U> fork0, Fun<LngStreamlet, V> fork1, ObjObj_Obj<U, V, W> join) {
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

	public boolean isAll(LngPredicate pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(LngPredicate pred) {
		return spawn().isAny(pred);
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
		return new LngStreamlet(() -> spawn().mapLng(fun));
	}

	public <K, V> LngObjStreamlet<V> mapLngObj(Lng_Obj<V> fun0) {
		return new LngObjStreamlet<>(() -> spawn().mapLngObj(fun0));
	}

	public <O> Streamlet<O> mapNonNull(Lng_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public LngStreamlet memoize() {
		Longs list = toList().toLongs();
		return streamlet(() -> LngOutlet.of(list));
	}

	public long min(LngComparator comparator) {
		return spawn().min(comparator);
	}

	public long minOrNull(LngComparator comparator) {
		return spawn().minOrNull(comparator);
	}

	public LngOpt opt() {
		return spawn().opt();
	}

	public LngOutlet outlet() {
		return spawn();
	}

	public Pair<LngStreamlet, LngStreamlet> partition(LngPredicate pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public LngStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(LngSink sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().count();
	}

	public LngStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public LngStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public LngSource source() {
		return spawn().source();
	}

	public LngStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public long[] toArray() {
		return spawn().toArray();
	}

	public LongsBuilder toList() {
		return spawn().toList();
	}

	public <K> LngObjMap<LongsBuilder> toListMap() {
		return spawn().toListMap();
	}

	public <K> LngObjMap<LongsBuilder> toListMap(Lng_Lng valueFun) {
		return spawn().toListMap(valueFun);
	}

	public <K> Map<K, Long> toMap(Lng_Obj<K> keyFun) {
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

	public Set<Long> toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Lng_Obj<K> keyFun, Lng_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public long uniqueResult() {
		return spawn().opt().get();
	}

	private <O> Streamlet<O> concatMap_(Lng_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).outlet()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Lng_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).out()));
	}

	private <O> Streamlet<O> map_(Lng_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Lng_Obj<K> kf, Lng_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private LngOutlet spawn() {
		return in.source();
	}

}
