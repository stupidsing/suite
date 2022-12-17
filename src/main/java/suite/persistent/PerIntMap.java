package suite.persistent;

import java.util.ArrayList;
import java.util.List;

import primal.fp.Funs.Iterate;
import primal.fp.Funs2.BinOp;
import primal.primitive.adt.pair.IntObjPair;
import primal.streamlet.Streamlet;

public class PerIntMap<V> {

	private Bl<Bl<Bl<Bl<Bl<Bl<V>>>>>> bl0;

	public static <V> PerIntMap<V> meld(PerIntMap<V> map0, PerIntMap<V> map1, BinOp<V> f) {
		BinOp<Bl<V>> f4 = (m0, m1) -> Bl.meld(m0, m1, f);
		BinOp<Bl<Bl<V>>> f3 = (m0, m1) -> Bl.meld(m0, m1, f4);
		BinOp<Bl<Bl<Bl<V>>>> f2 = (m0, m1) -> Bl.meld(m0, m1, f3);
		BinOp<Bl<Bl<Bl<Bl<V>>>>> f1 = (m0, m1) -> Bl.meld(m0, m1, f2);
		BinOp<Bl<Bl<Bl<Bl<Bl<V>>>>>> f0 = (m0, m1) -> Bl.meld(m0, m1, f1);
		return new PerIntMap<>(Bl.meld(map0.bl0, map1.bl0, f0));
	}

	public static <V> PerIntMap<V> of(List<IntObjPair<V>> list) {
		var list6 = new ArrayList<>(list);
		list6.sort((p0, p1) -> Integer.compare(p0.k, p1.k));
		var list5 = consolidate(list6);
		var list4 = consolidate(list5);
		var list3 = consolidate(list4);
		var list2 = consolidate(list3);
		var list1 = consolidate(list2);
		return new PerIntMap<>(Bl.of(list1.subList(0, list1.size())));
	}

	private static <V> List<IntObjPair<Bl<V>>> consolidate(List<IntObjPair<V>> list0) {
		var list1 = new ArrayList<IntObjPair<Bl<V>>>();
		int size = list0.size(), i0 = 0, prevKey = 0, key;
		for (var i = 0; i < size; i++) {
			if (prevKey != (key = list0.get(i).k & 63)) {
				list1.add(IntObjPair.of(prevKey >>> 6, Bl.of(list0.subList(i0, i))));
				i0 = i;
				prevKey = key;
			}
		}
		list1.add(IntObjPair.of(prevKey >>> 6, Bl.of(list0.subList(i0, size))));
		return list1;
	}

	public PerIntMap() {
		this(null);
	}

	private PerIntMap(Bl<Bl<Bl<Bl<Bl<Bl<V>>>>>> Bl) {
		this.bl0 = Bl;
	}

	public Streamlet<V> streamlet() {
		return Bl.stream(bl0) //
				.concatMap(Bl::stream) //
				.concatMap(Bl::stream) //
				.concatMap(Bl::stream) //
				.concatMap(Bl::stream) //
				.concatMap(Bl::stream);
	}

	public V get(int key) {
		var k0 = key >>> 30 & 63;
		var k1 = key >>> 24 & 63;
		var k2 = key >>> 18 & 63;
		var k3 = key >>> 12 & 63;
		var k4 = key >>> 6 & 63;
		var k5 = key >>> 0 & 63;
		var bl1 = Bl.get(bl0, k0);
		var bl2 = Bl.get(bl1, k1);
		var bl3 = Bl.get(bl2, k2);
		var bl4 = Bl.get(bl3, k3);
		var bl5 = Bl.get(bl4, k4);
		return Bl.get(bl5, k5);
	}

	public PerIntMap<V> update(int key, Iterate<V> fun) {
		var k0 = key >>> 30 & 63;
		var k1 = key >>> 24 & 63;
		var k2 = key >>> 18 & 63;
		var k3 = key >>> 12 & 63;
		var k4 = key >>> 6 & 63;
		var k5 = key >>> 0 & 63;
		var bl1 = Bl.get(bl0, k0);
		var bl2 = Bl.get(bl1, k1);
		var bl3 = Bl.get(bl2, k2);
		var bl4 = Bl.get(bl3, k3);
		var bl5 = Bl.get(bl4, k4);
		var v0 = Bl.get(bl5, k5);
		var v1 = fun.apply(v0);
		var new5 = Bl.update(bl5, k5, v1);
		var new4 = Bl.update(bl4, k4, new5);
		var new3 = Bl.update(bl3, k3, new4);
		var new2 = Bl.update(bl2, k2, new3);
		var new1 = Bl.update(bl1, k1, new2);
		var new0 = Bl.update(bl0, k0, new1);
		return new PerIntMap<>(new0);
	}

}
