package suite.immutable;

import suite.adt.BitmapList;

public class IIntMap<V> {

	private Bl0<V> bl0;

	private static class Bl0<V> extends BitmapList<Bl1<V>> {
		private Bl0(Bl0<V> Bl_, int index, Bl1<V> child) {
			super(Bl_, index, child);
		}
	}

	private static class Bl1<V> extends BitmapList<Bl2<V>> {
		private Bl1(Bl1<V> Bl_, int index, Bl2<V> child) {
			super(Bl_, index, child);
		}
	}

	private static class Bl2<V> extends BitmapList<Bl3<V>> {
		private Bl2(Bl2<V> Bl_, int index, Bl3<V> child) {
			super(Bl_, index, child);
		}
	}

	private static class Bl3<V> extends BitmapList<Bl4<V>> {
		private Bl3(Bl3<V> Bl_, int index, Bl4<V> child) {
			super(Bl_, index, child);
		}
	}

	private static class Bl4<V> extends BitmapList<Bl5<V>> {
		private Bl4(Bl4<V> Bl_, int index, Bl5<V> child) {
			super(Bl_, index, child);
		}
	}

	private static class Bl5<V> extends BitmapList<Bl6<V>> {
		private Bl5(Bl5<V> Bl_, int index, Bl6<V> child) {
			super(Bl_, index, child);
		}
	}

	private static class Bl6<V> extends BitmapList<V> {
		private Bl6(Bl6<V> Bl_, int index, V child) {
			super(Bl_, index, child);
		}
	}

	public IIntMap() {
		this(null);
	}

	private IIntMap(Bl0<V> Bl) {
		this.bl0 = Bl;
	}

	public V get(int key) {
		int k0 = key >> 30;
		int k1 = key >> 25 & 31;
		int k2 = key >> 20 & 31;
		int k3 = key >> 15 & 31;
		int k4 = key >> 10 & 31;
		int k5 = key >> 5 & 31;
		int k6 = key & 31;
		Bl1<V> bl1 = BitmapList.get(bl0, k0);
		Bl2<V> bl2 = BitmapList.get(bl1, k1);
		Bl3<V> bl3 = BitmapList.get(bl2, k2);
		Bl4<V> bl4 = BitmapList.get(bl3, k3);
		Bl5<V> bl5 = BitmapList.get(bl4, k4);
		Bl6<V> bl6 = BitmapList.get(bl5, k5);
		return BitmapList.get(bl6, k6);
	}

	public IIntMap<V> put(int key, V v) {
		int k0 = key >> 30;
		int k1 = key >> 25 & 31;
		int k2 = key >> 20 & 31;
		int k3 = key >> 15 & 31;
		int k4 = key >> 10 & 31;
		int k5 = key >> 5 & 31;
		int k6 = key & 31;
		Bl1<V> bl1 = BitmapList.get(bl0, k0);
		Bl2<V> bl2 = BitmapList.get(bl1, k1);
		Bl3<V> Bl3 = BitmapList.get(bl2, k2);
		Bl4<V> bl4 = BitmapList.get(Bl3, k3);
		Bl5<V> bl5 = BitmapList.get(bl4, k4);
		Bl6<V> bl6 = BitmapList.get(bl5, k5);
		Bl6<V> new6 = new Bl6<>(bl6, k6, v);
		Bl5<V> new5 = new Bl5<>(bl5, k5, new6);
		Bl4<V> new4 = new Bl4<>(bl4, k4, new5);
		Bl3<V> new3 = new Bl3<>(Bl3, k3, new4);
		Bl2<V> new2 = new Bl2<>(bl2, k2, new3);
		Bl1<V> new1 = new Bl1<>(bl1, k1, new2);
		Bl0<V> new0 = new Bl0<>(bl0, k0, new1);
		return new IIntMap<>(new0);
	}

}
