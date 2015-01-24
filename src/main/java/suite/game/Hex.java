package suite.game;

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

	public XY toScreenCoord(XY coord) {
		return XY.of(coord.x * 2 + coord.y, coord.y);
	}

	public int distance(XY diff) {
		int dx = diff.x;
		int dy = diff.y;
		XY xy0 = XY.of(-dy, 2 * dy + dx);
		XY xy1 = XY.of(2 * dx + dy, -dx);
		return Math.min(Math.abs(xy0.x) + Math.abs(xy0.y), Math.abs(xy1.x) + Math.abs(xy1.y));
	}

	public XY towards(XY diff) {
		int dx = diff.x;
		int dy = diff.y;

		if (dx > 0)
			return XY.of(1, dy < 0 ? -1 : 0);
		else if (dx < 0)
			return XY.of(-1, dy > 0 ? 1 : 0);
		else
			return XY.of(0, dy < 0 ? -1 : 1);
	}

}
