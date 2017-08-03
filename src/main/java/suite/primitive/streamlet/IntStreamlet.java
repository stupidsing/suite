package suite.primitive.streamlet;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.adt.map.ListMultimap;
import suite.adt.pair.Pair;
import suite.primitive.IntFunUtil;
import suite.primitive.IntOpt;
import suite.primitive.IntPrimitives.IntComparator;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.IntPredicate;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.Int_Int;
import suite.primitive.Ints;
import suite.primitive.Ints.IntsBuilder;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.set.IntSet;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.streamlet.StreamletDefaults;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
import suite.util.Object_;

public class IntStreamlet implements StreamletDefaults<Integer, IntOutlet> {

	private Source<IntOutlet> in;

	@SafeVarargs
	public static IntStreamlet concat(IntStreamlet... streamlets) {
		return Read.from(streamlets).collect(IntStreamlet::concat);
	}

	public static IntStreamlet concat(Outlet<IntStreamlet> streamlets) {
		return streamlet(() -> {
			Source<IntStreamlet> source = streamlets.source();
			return IntOutlet.of(IntFunUtil.concat(FunUtil.map(st -> st.spawn().source(), source)));
		});
	}

	public static <T> Fun<Outlet<T>, IntStreamlet> of(Obj_Int<T> fun0) {
		Obj_Int<T> fun1 = fun0.rethrow();
		return ts -> {
			IntsBuilder cb = new IntsBuilder();
			T t;
			while ((t = ts.next()) != null)
				cb.append(fun1.apply(t));
			return cb.toInts().streamlet();
		};
	}

	public static IntStreamlet of(int[] ts) {
		return streamlet(() -> IntOutlet.of(ts));
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

	public IntStreamlet append(int c) {
		return streamlet(() -> spawn().append(c));
	}

	public <R> R apply(Fun<IntStreamlet, R> fun) {
		return fun.apply(this);
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

	public <O> Streamlet<O> concatMap(Int_Obj<Streamlet<O>> fun) {
		return concatMap_(fun);
	}

	public <K, V> Streamlet2<K, V> concatMap2(Int_Obj<Streamlet2<K, V>> fun) {
		return concatMap2_(fun);
	}

	public IntStreamlet cons(int c) {
		return streamlet(() -> spawn().cons(c));
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

	public <U, V, W> W forkJoin(Fun<IntStreamlet, U> fork0, Fun<IntStreamlet, V> fork1, Fun2<U, V, W> join) {
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

	public int last() {
		return spawn().last();
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

	public IntStreamlet memoize() {
		Ints list = toList().toInts();
		return streamlet(() -> IntOutlet.of(list));
	}

	public int max() {
		return spawn().max();
	}

	public int min() {
		return spawn().min();
	}

	public int min(IntComparator comparator) {
		return spawn().min(comparator);
	}

	public int minOrEmpty(IntComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public IntOpt opt() {
		return spawn().opt();
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

	public int sum() {
		return spawn().sum();
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

	public IntSet toSet() {
		return spawn().toSet();
	}

	public <K, V> Map<K, Set<V>> toSetMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return spawn().toSetMap(keyFun, valueFun);
	}

	public int uniqueResult() {
		return spawn().opt().get();
	}

	private <O> Streamlet<O> concatMap_(Int_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).outlet()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Int_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).outlet()));
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
