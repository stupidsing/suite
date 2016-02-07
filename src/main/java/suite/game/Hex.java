package suite.game;

import java.util.Arrays;
import java.util.List;

public class Hex {

	public static class XY {
		public int x;
		public int y;

		public static XY of(int x, int y) {
			XY xy = new XY();
			xy.x = x;
			xy.y = y;
			return xy;
		}
	}

	public List<XY> directions = Arrays.asList( //
			XY.of(0, -1), XY.of(0, 1) //
			, XY.of(-1, 0), XY.of(1, 0) //
			, XY.of(-1, 1), XY.of(1, -1));

	public XY diff(XY from, XY to) {
		return XY.of(to.x - from.x, to.y - from.y);
	}

	public XY toScreenCoord(XY coord) {
		return XY.of(coord.x * 2 + coord.y, coord.y);
	}

	public int distance(XY diff) {
		int dx = diff.x;
		int dy = diff.y;
		int sum = dx + dy;
		XY xy0 = XY.of(dx - sum, dy + sum);
		XY xy1 = XY.of(dx + sum, dy - sum);
		return Math.min(Math.abs(xy0.x) + Math.abs(xy0.y), Math.abs(xy1.x) + Math.abs(xy1.y));
	}

	public XY towards(XY diff) {
		int dx = diff.x;
		int dy = diff.y;

		if (0 < dx)
			return XY.of(1, dy < 0 ? -1 : 0);
		else if (dx < 0)
			return XY.of(-1, dy <= 0 ? 0 : 1);
		else
			return XY.of(0, dy < 0 ? -1 : 1);
	}

}
