package suite.primitive;

import java.util.Comparator;

import suite.object.Object_;
import suite.streamlet.FunUtil.Iterate;

public class Coord {

	private static Coord none_ = Coord.of(IntFunUtil.EMPTYVALUE, IntFunUtil.EMPTYVALUE);

	public int x;
	public int y;

	public static Iterate<Coord> mapFst(Int_Int fun) {
		return coord -> of(fun.apply(coord.x), coord.y);
	}

	public static Iterate<Coord> mapSnd(Int_Int fun) {
		return coord -> of(coord.x, fun.apply(coord.y));
	}

	public static Coord none() {
		return none_;
	}

	public static Coord of(int x, int y) {
		return new Coord(x, y);
	}

	protected Coord(int x, int y) {
		update(x, y);
	}

	public static Comparator<Coord> comparator() {
		return (coord0, coord1) -> {
			var c = Boolean.compare(coord0 != null, coord1 != null);
			c = c == 0 ? Integer.compare(coord0.x, coord1.x) : c;
			c = c == 0 ? Integer.compare(coord0.y, coord1.y) : c;
			return c;
		};
	}

	public static Comparator<Coord> comparatorByFirst() {
		return (coord0, coord1) -> {
			var c = Boolean.compare(coord0 != null, coord1 != null);
			c = c == 0 ? Integer.compare(coord0.x, coord1.x) : c;
			return c;
		};
	}

	public static int x(Coord coord) {
		return coord.x;
	}

	public static int y(Coord coord) {
		return coord.y;
	}

	public <O> O map(IntInt_Obj<O> fun) {
		return fun.apply(x, y);
	}

	public void update(int x_, int y_) {
		x = x_;
		y = y_;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == Coord.class) {
			var other = (Coord) object;
			return x == other.x && y == other.y;
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return Integer.hashCode(x) + 31 * Integer.hashCode(y);
	}

	@Override
	public String toString() {
		return x + ":" + y;
	}

}
