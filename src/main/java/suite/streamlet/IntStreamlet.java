package suite.streamlet;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.IntObjMap;
import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.IntFunUtil;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntComparator;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.IntPredicate;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.Int_Int;
import suite.primitive.Ints;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Object_;
import suite.util.To;

public class IntStreamlet implements Iterable<Integer> {

	private Source<IntOutlet> in;

	@SafeVarargs
	public static IntStreamlet concat(IntStreamlet... streamlets) {
		return streamlet(() -> {
			List<IntSource> sources = new ArrayList<>();
			for (IntStreamlet streamlet : streamlets)
				sources.add(streamlet.in.source().source());
			return IntOutlet.of(IntFunUtil.concat(To.source(sources)));
		});
	}

	public static IntStreamlet range(int e) {
		return range((int) 0, e);
	}

	public static IntStreamlet range(int s, int e) {
		return streamlet(() -> {
			IntMutable m = IntMutable.of(s);
			return IntOutlet.of(() -> {
				int c = m.increment();
				return c < e ? c : IntFunUtil.EMPTYVALUE;
			});
		});
	}

	private static IntStreamlet streamlet(Source<IntOutlet> in) {
		return new IntStreamlet(in);
	}

	public IntStreamlet(Source<IntOutlet> in) {
		this.in = in;
	}

	@Override
	public Iterator<Integer> iterator() {
		return spawn().iterator();
	}

	public IntStreamlet append(int t) {
		return streamlet(() -> spawn().append(t));
	}

	public Streamlet<IntOutlet> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public IntStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			IntOutlet in = spawn();
			in.closeAtEnd(c);
			return in;
		});
	}

	public <R> R collect(Fun<IntOutlet, R> fun) {
		return fun.apply(spawn());
	}

	public <O> Streamlet<O> concatMap(Int_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Int_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public IntStreamlet cons(int t) {
		return streamlet(() -> spawn().cons(t));
	}

	public <U, O> Streamlet<O> cross(Streamlet<U> st1, IntObj_Obj<U, O> fun) {
		return new Streamlet<>(() -> spawn().cross(st1.toList(), fun));
	}

	public IntStreamlet distinct() {
		return streamlet(() -> spawn().distinct());
	}

	public IntStreamlet drop(int n) {
		return streamlet(() -> spawn().drop(n));
	}

	@Override
	public boolean equals(Object object) {
		return Object_.clazz(object) == IntStreamlet.class ? Objects.equals(spawn(), ((IntStreamlet) object).spawn()) : false;
	}

	public IntStreamlet filter(IntPredicate fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public int first() {
		return spawn().first();
	}

	public IntStreamlet flatMap(Int_Obj<Iterable<Integer>> fun) {
		return streamlet(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, IntObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<IntStreamlet, U> fork0, Fun<IntStreamlet, V> fork1, ObjObj_Obj<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> IntObjStreamlet<IntsBuilder> groupBy() {
		return new IntObjStreamlet<>(() -> spawn().groupBy());
	}

	public <V> IntObjStreamlet<V> groupBy(Fun<Ints, V> fun) {
		return new IntObjStreamlet<>(() -> spawn().groupBy(fun));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public IntObjStreamlet<Integer> index() {
		return new IntObjStreamlet<>(() -> spawn().index());
	}

	public boolean isAll(IntPredicate pred) {
		return spawn().isAll(pred);
	}

	public boolean isAny(IntPredicate pred) {
		return spawn().isAny(pred);
	}

	public <O> Streamlet2<Integer, O> join2(Streamlet<O> streamlet) {
		return concatMap2_(t -> streamlet.map2(v -> t, v -> v));
	}

	public <O> Streamlet<O> map(Int_Obj<O> fun) {
		return map_(fun);
	}

	public <K, V> Streamlet2<K, V> map2(Int_Obj<K> kf, Int_Obj<V> vf) {
		return map2_(kf, vf);
	}

	public IntStreamlet mapInt(Int_Int fun) {
		return new IntStreamlet(() -> spawn().mapInt(fun));
	}

	public <K, V> IntObjStreamlet<V> mapIntObj(Int_Obj<V> fun0) {
		return new IntObjStreamlet<>(() -> spawn().mapIntObj(fun0));
	}

	public <O> Streamlet<O> mapNonNull(Int_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().mapNonNull(fun));
	}

	public int last() {
		return spawn().last();
	}

	public IntStreamlet memoize() {
		Ints list = toList().toInts();
		return streamlet(() -> IntOutlet.of(list));
	}

	public int min(IntComparator comparator) {
		return spawn().min(comparator);
	}

	public int minOrNull(IntComparator comparator) {
		return spawn().minOrNull(comparator);
	}

	public IntOutlet outlet() {
		return spawn();
	}

	public Pair<IntStreamlet, IntStreamlet> partition(IntPredicate pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public IntStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public void sink(IntSink sink) {
		spawn().sink(sink);
	}

	public int size() {
		return spawn().count();
	}

	public IntStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public IntStreamlet sort() {
		return streamlet(() -> spawn().sort());
	}

	public IntSource source() {
		return spawn().source();
	}

	public IntStreamlet take(int n) {
		return streamlet(() -> spawn().take(n));
	}

	public int[] toArray() {
		return spawn().toArray();
	}

	public IntsBuilder toList() {
		return spawn().toList();
	}

	public <K> IntObjMap<IntsBuilder> toListMap() {
		return spawn().toListMap();
	}

	public <K> IntObjMap<IntsBuilder> toListMap(Int_Int valueFun) {
		return spawn().toListMap(valueFun);
	}

	public <K> Map<K, Integer> toMap(Int_Obj<K> keyFun) {
		return spawn().toMap(keyFun);
	}

	public <K, V> Map<K, V> toMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Integer> toMultimap(Int_Obj<K> keyFun) {
		return spawn().toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return spawn().toMultimap(keyFun, valueFun);
	}

	public Set<Integer> toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public int uniqueResult() {
		return spawn().uniqueResult();
	}

	private <O> Streamlet<O> concatMap_(Int_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).outlet()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Int_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).out()));
	}

	private <O> Streamlet<O> map_(Int_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Int_Obj<K> kf, Int_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private IntOutlet spawn() {
		return in.source();
	}

}
