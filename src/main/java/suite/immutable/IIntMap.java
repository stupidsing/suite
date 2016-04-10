package suite.immutable;

import java.util.function.BiFunction;

import suite.util.FunUtil.Fun;

public class IIntMap<V> {

	private Bl<Bl<Bl<Bl<Bl<Bl<V>>>>>> bl0;

	public static <V> IIntMap<V> merge(IIntMap<V> map0, IIntMap<V> map1) {
		BiFunction<V, V, V> f5 //
				= (v0, v1) -> v0 != null ? v0 : v1;
		BiFunction<Bl<V>, Bl<V>, Bl<V>> f4 //
				= (m0, m1) -> Bl.merge(m0, m1, f5);
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

	public IIntMap() {
		this(null);
	}

	private IIntMap(Bl<Bl<Bl<Bl<Bl<Bl<V>>>>>> Bl) {
		this.bl0 = Bl;
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
