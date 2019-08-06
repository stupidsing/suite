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
import primal.primitive.IntOpt;
import primal.primitive.IntPrim;
import primal.primitive.IntPrim.IntComparator;
import primal.primitive.IntPrim.IntObjSource;
import primal.primitive.IntPrim.IntObj_Obj;
import primal.primitive.IntPrim.IntPred;
import primal.primitive.IntPrim.IntSink;
import primal.primitive.IntPrim.IntSource;
import primal.primitive.IntPrim.Int_Obj;
import primal.primitive.Int_Int;
import primal.primitive.adt.Ints;
import primal.primitive.adt.Ints.IntsBuilder;
import primal.primitive.adt.pair.IntObjPair;
import primal.primitive.puller.IntObjPuller;
import primal.primitive.puller.IntPuller;
import primal.puller.Puller;
import primal.streamlet.StreamletDefaults;
import suite.adt.map.ListMultimap;
import suite.primitive.Ints_;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.map.ObjIntMap;
import suite.primitive.adt.set.IntSet;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;

public class IntStreamlet implements StreamletDefaults<Integer, IntOpt, IntPred, IntPuller, IntSink, IntSource> {

	private Source<IntPuller> in;

	private static IntStreamlet streamlet(Source<IntPuller> in) {
		return new IntStreamlet(in);
	}

	public IntStreamlet(Source<IntPuller> in) {
		this.in = in;
	}

	public <R> R apply(Fun<IntStreamlet, R> fun) {
		return fun.apply(this);
	}

	public int average() {
		return spawn().average();
	}

	public Streamlet<IntPuller> chunk(int n) {
		return new Streamlet<>(() -> spawn().chunk(n));
	}

	public IntStreamlet closeAtEnd(Closeable c) {
		return streamlet(() -> {
			var in = spawn();
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

	public IntStreamlet collect() {
		var ints = toList_();
		return Ints_.of(ints.cs, ints.start, ints.end, 1);
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
		return Get.clazz(object) == IntStreamlet.class ? Equals.ab(spawn(), ((IntStreamlet) object).spawn()) : false;
	}

	public IntStreamlet filter(IntPred fun) {
		return streamlet(() -> spawn().filter(fun));
	}

	public int first() {
		return spawn().first();
	}

	public <O> Streamlet<O> flatMap(Int_Obj<Iterable<O>> fun) {
		return new Streamlet<>(() -> spawn().flatMap(fun));
	}

	public <R> R fold(R init, IntObj_Obj<R, R> fun) {
		return spawn().fold(init, fun);
	}

	public <U, V, W> W forkJoin(Fun<IntStreamlet, U> fork0, Fun<IntStreamlet, V> fork1, Fun2<U, V, W> join) {
		return join.apply(fork0.apply(this), fork1.apply(this));
	}

	public <V> IntObjStreamlet<IntsBuilder> groupBy() {
		return new IntObjStreamlet<>(this::groupBy_);
	}

	public <V> IntObjStreamlet<V> groupBy(Fun<Ints, V> fun) {
		return new IntObjStreamlet<>(() -> groupBy_().mapValue(list -> fun.apply(list.toInts())));
	}

	@Override
	public int hashCode() {
		return spawn().hashCode();
	}

	public IntObjStreamlet<Integer> index() {
		return new IntObjStreamlet<>(() -> IntObjPuller.of(new IntObjSource<>() {
			private IntPuller puller = spawn();
			private int i = 0;

			public boolean source2(IntObjPair<Integer> pair) {
				var c = puller.pull();
				if (c != IntPrim.EMPTYVALUE) {
					pair.update(c, i++);
					return true;
				} else
					return false;
			}
		}));
	}

	@Override
	public Iterator<Integer> iterator() {
		return spawn().iterator();
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
		return streamlet(() -> spawn().mapInt(fun));
	}

	public <K, V> IntObjStreamlet<V> mapIntObj(Int_Obj<V> fun0) {
		return new IntObjStreamlet<>(() -> spawn().mapIntObj(fun0));
	}

	public int max() {
		return spawn().min((c0, c1) -> Integer.compare(c1, c0));
	}

	public int min() {
		return spawn().min((c0, c1) -> Integer.compare(c0, c1));
	}

	public int min(IntComparator comparator) {
		return spawn().min(comparator);
	}

	public int minOrEmpty(IntComparator comparator) {
		return spawn().minOrEmpty(comparator);
	}

	public Pair<IntStreamlet, IntStreamlet> partition(IntPred pred) {
		return Pair.of(filter(pred), filter(t -> !pred.test(t)));
	}

	public IntPuller puller() {
		return spawn();
	}

	public IntStreamlet reverse() {
		return streamlet(() -> spawn().reverse());
	}

	public IntStreamlet skip(int n) {
		return streamlet(() -> spawn().skip(n));
	}

	public IntStreamlet snoc(int c) {
		return streamlet(() -> spawn().snoc(c));
	}

	public IntStreamlet sort() {
		return streamlet(() -> spawn().sort());
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

	public Ints toList() {
		return toList_();
	}

	public <K> IntObjMap<IntsBuilder> toListMap() {
		return toListMap_();
	}

	public <K> IntObjMap<IntsBuilder> toListMap(Int_Int valueFun) {
		return toListMap_(valueFun);
	}

	public <K> ObjIntMap<K> toMap(Int_Obj<K> kf0) {
		var puller = spawn();
		var kf1 = kf0.rethrow();
		var map = new ObjIntMap<K>();
		int c;
		while ((c = puller.pull()) != IntPrim.EMPTYVALUE)
			map.put(kf1.apply(c), c);
		return map;
	}

	public <K, V> Map<K, V> toMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return spawn().toMap(keyFun, valueFun);
	}

	public <K> ListMultimap<K, Integer> toMultimap(Int_Obj<K> keyFun) {
		return toMultimap(keyFun);
	}

	public <K, V> ListMultimap<K, V> toMultimap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().collect(As::multimap);
	}

	public IntSet toSet() {
		var puller = spawn();
		var set = new IntSet();
		int c;
		while ((c = puller.pull()) != IntPrim.EMPTYVALUE)
			set.add(c);
		return set;
	}

	public <K, V> Map<K, Set<V>> toSetMap(Int_Obj<K> keyFun, Int_Obj<V> valueFun) {
		return spawn().map2(keyFun, valueFun).groupBy().mapValue(values -> Read.from(values).toSet()).toMap();
	}

	public int uniqueResult() {
		return spawn().opt().get();
	}

	public <U, V> Streamlet<V> zip(Iterable<U> list1, IntObj_Obj<U, V> fun) {
		return new Streamlet<>(() -> spawn().zip(Puller.of(list1), fun));
	}

	private <O> Streamlet<O> concatMap_(Int_Obj<Streamlet<O>> fun) {
		return new Streamlet<>(() -> spawn().concatMap(t -> fun.apply(t).puller()));
	}

	private <K, V> Streamlet2<K, V> concatMap2_(Int_Obj<Streamlet2<K, V>> fun) {
		return new Streamlet2<>(() -> spawn().concatMap2(t -> fun.apply(t).puller()));
	}

	private <V> IntObjPuller<IntsBuilder> groupBy_() {
		return IntObjPuller.of(toListMap_().source());
	}

	private <O> Streamlet<O> map_(Int_Obj<O> fun) {
		return new Streamlet<>(() -> spawn().map(fun));
	}

	private <K, V> Streamlet2<K, V> map2_(Int_Obj<K> kf, Int_Obj<V> vf) {
		return new Streamlet2<>(() -> spawn().map2(kf, vf));
	}

	private Ints toList_() {
		var list = spawn().toList();
		return Ints.of(list.cs, 0, list.size);
	}

	private IntObjMap<IntsBuilder> toListMap_() {
		return toListMap_(value -> value);
	}

	private IntObjMap<IntsBuilder> toListMap_(Int_Int valueFun) {
		var puller = spawn();
		var map = new IntObjMap<IntsBuilder>();
		int c;
		while ((c = puller.pull()) != IntPrim.EMPTYVALUE)
			map.computeIfAbsent(c, k_ -> new IntsBuilder()).append(valueFun.apply(c));
		return map;
	}

	private IntPuller spawn() {
		return in.g();
	}

}
