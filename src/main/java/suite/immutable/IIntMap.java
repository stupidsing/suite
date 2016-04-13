package suite.immutable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import suite.adt.Pair;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

public class IIntMap<V> {

	private Bl<Bl<Bl<Bl<Bl<Bl<V>>>>>> bl0;

	public static <V> IIntMap<V> merge(IIntMap<V> map0, IIntMap<V> map1, BiFunction<V, V, V> f) {
		BiFunction<Bl<V>, Bl<V>, Bl<V>> f4 //
				= (m0, m1) -> Bl.merge(m0, m1, f);
		BiFunction<Bl<Bl<V>>, Bl<Bl<V>>, Bl<Bl<V>>> f3 //
				= (m0, m1) -> Bl.merge(m0, m1, f4);
		BiFunction<Bl<Bl<Bl<V>>>, Bl<Bl<Bl<V>>>, Bl<Bl<Bl<V>>>> f2 //
				= (m0, m1) -> Bl.merge(m0, m1, f3);
		BiFunction<Bl<Bl<Bl<Bl<V>>>>, Bl<Bl<Bl<Bl<V>>>>, Bl<Bl<Bl<Bl<V>>>>> f1 //
				= (m0, m1) -> Bl.merge(m0, m1, f2);
		BiFunction<Bl<Bl<Bl<Bl<Bl<V>>>>>, Bl<Bl<Bl<Bl<Bl<V>>>>>, Bl<Bl<Bl<Bl<Bl<V>>>>>> f0 //
				= (m0, m1) -> Bl.merge(m0, m1, f1);
		return new IIntMap<>(Bl.merge(map0.bl0, map1.bl0, f0));
	}

	public static <V> IIntMap<V> of(List<Pair<Integer, V>> list) {
		List<Pair<Integer, V>> list6 = new ArrayList<>(list);
		list6.sort((p0, p1) -> p0.t0.compareTo(p1.t0));
		int k_;

		List<Pair<Integer, Bl<V>>> list5 = new ArrayList<>();
		{
			int i5 = 0, k5 = 0;
			for (int i = 0; i < list6.size(); i++) {
				if (k5 != (k_ = list6.get(i).t0 & 63)) {
					list5.add(Pair.of(k5 >>> 6, Bl.of(list6.subList(i5, i))));
					i5 = i;
					k5 = k_;
				}
			}
			list5.add(Pair.of(k5 >>> 6, Bl.of(list6.subList(i5, list6.size()))));
		}

		List<Pair<Integer, Bl<Bl<V>>>> list4 = new ArrayList<>();
		{
			int i4 = 0, k4 = 0;
			for (int i = 0; i < list5.size(); i++) {
				if (k4 != (k_ = list5.get(i).t0 & 63)) {
					list4.add(Pair.of(k4 >>> 6, Bl.of(list5.subList(i4, i))));
					i4 = i;
					k4 = k_;
				}
			}
			list4.add(Pair.of(k4 >>> 6, Bl.of(list5.subList(i4, list5.size()))));
		}

		List<Pair<Integer, Bl<Bl<Bl<V>>>>> list3 = new ArrayList<>();
		{
			int i3 = 0, k3 = 0;
			for (int i = 0; i < list4.size(); i++) {
				if (k3 != (k_ = list4.get(i).t0 & 63)) {
					list3.add(Pair.of(k3 >>> 6, Bl.of(list4.subList(i3, i))));
					i3 = i;
					k3 = k_;
				}
			}
			list3.add(Pair.of(k3 >>> 6, Bl.of(list4.subList(i3, list4.size()))));
		}

		List<Pair<Integer, Bl<Bl<Bl<Bl<V>>>>>> list2 = new ArrayList<>();
		{
			int i2 = 0, k2 = 0;
			for (int i = 0; i < list3.size(); i++) {
				if (k2 != (k_ = list3.get(i).t0 & 63)) {
					list2.add(Pair.of(k2 >>> 6, Bl.of(list3.subList(i2, i))));
					i2 = i;
					k2 = k_;
				}
			}
			list2.add(Pair.of(k2 >>> 6, Bl.of(list3.subList(i2, list3.size()))));
		}

		List<Pair<Integer, Bl<Bl<Bl<Bl<Bl<V>>>>>>> list1 = new ArrayList<>();
		{
			int i1 = 0, k1 = 0;
			for (int i = 0; i < list2.size(); i++) {
				if (k1 != (k_ = list2.get(i).t0 & 63)) {
					list1.add(Pair.of(k1 >>> 6, Bl.of(list2.subList(i1, i))));
					i1 = i;
					k1 = k_;
				}
			}
			list1.add(Pair.of(k1 >>> 6, Bl.of(list2.subList(i1, list2.size()))));
		}

		return new IIntMap<>(Bl.of(list1.subList(0, list1.size())));
	}

	public IIntMap() {
		this(null);
	}

	private IIntMap(Bl<Bl<Bl<Bl<Bl<Bl<V>>>>>> Bl) {
		this.bl0 = Bl;
	}

	public Streamlet<V> stream() {
		return Bl.stream(bl0) //
				.concatMap(Bl::stream) //
				.concatMap(Bl::stream) //
				.concatMap(Bl::stream) //
				.concatMap(Bl::stream) //
				.concatMap(Bl::stream);
	}

	public V get(int key) {
		int k0 = key >>> 30 & 63;
		int k1 = key >>> 24 & 63;
		int k2 = key >>> 18 & 63;
		int k3 = key >>> 12 & 63;
		int k4 = key >>> 6 & 63;
		int k5 = key >>> 0 & 63;
		Bl<Bl<Bl<Bl<Bl<V>>>>> bl1 = Bl.get(bl0, k0);
		Bl<Bl<Bl<Bl<V>>>> bl2 = Bl.get(bl1, k1);
		Bl<Bl<Bl<V>>> bl3 = Bl.get(bl2, k2);
		Bl<Bl<V>> bl4 = Bl.get(bl3, k3);
		Bl<V> bl5 = Bl.get(bl4, k4);
		return Bl.get(bl5, k5);
	}

	public IIntMap<V> update(int key, Fun<V, V> fun) {
		int k0 = key >>> 30 & 63;
		int k1 = key >>> 24 & 63;
		int k2 = key >>> 18 & 63;
		int k3 = key >>> 12 & 63;
		int k4 = key >>> 6 & 63;
		int k5 = key >>> 0 & 63;
		Bl<Bl<Bl<Bl<Bl<V>>>>> bl1 = Bl.get(bl0, k0);
		Bl<Bl<Bl<Bl<V>>>> Bl2 = Bl.get(bl1, k1);
		Bl<Bl<Bl<V>>> bl3 = Bl.get(Bl2, k2);
		Bl<Bl<V>> bl4 = Bl.get(bl3, k3);
		Bl<V> bl5 = Bl.get(bl4, k4);
		V v0 = Bl.get(bl5, k5);
		V v1 = fun.apply(v0);
		Bl<V> new5 = Bl.update(bl5, k5, v1);
		Bl<Bl<V>> new4 = Bl.update(bl4, k4, new5);
		Bl<Bl<Bl<V>>> new3 = Bl.update(bl3, k3, new4);
		Bl<Bl<Bl<Bl<V>>>> new2 = Bl.update(Bl2, k2, new3);
		Bl<Bl<Bl<Bl<Bl<V>>>>> new1 = Bl.update(bl1, k1, new2);
		Bl<Bl<Bl<Bl<Bl<Bl<V>>>>>> new0 = Bl.update(bl0, k0, new1);
		return new IIntMap<>(new0);
	}

}
