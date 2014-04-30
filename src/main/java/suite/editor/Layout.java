package suite.editor;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;

public class Layout {

	public static class Leaf implements Node {
		public Component component;
		public Vector min, max;
	}

	public static class Box implements Node {
		public Orientation orientation;
		public List<Node> nodes;
	}

	public interface Node {
	}

	public static class Vector {
		public int x, y;
		public static Vector ORIGIN = new Vector(0, 0);

		public Vector(Orientation orientation, int w, int h) {
			this(orientation == Orientation.HORIZONTAL ? w : h, orientation == Orientation.HORIZONTAL ? h : w);
		}

		public Vector(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int width(Orientation orientation) {
			return orientation == Orientation.HORIZONTAL ? x : y;
		}

		public int height(Orientation orientation) {
			return orientation == Orientation.HORIZONTAL ? y : x;
		}

		public String toString() {
			return String.format("(%d, %d)", x, y);
		}
	}

	public static class Rect {
		public Vector v0, v1;

		public Rect(int x0, int y0, int x1, int y1) {
			this(new Vector(x0, y0), new Vector(x1, y1));
		}

		public Rect(Vector v0, Vector v1) {
			this.v0 = v0;
			this.v1 = v1;
		}

		public int width(Orientation orientation) {
			return orientation == Orientation.HORIZONTAL ? xdiff() : ydiff();
		}

		public int xdiff() {
			return v1.x - v0.x;
		}

		public int ydiff() {
			return v1.y - v0.y;
		}

		public String toString() {
			return String.format("(%s, %s)", v0, v1);
		}
	}

	public enum Orientation {
		HORIZONTAL, VERTICAL
	}

	public static Leaf co(Component component, int w, int h) {
		return leaf(component, new Vector(w, h), new Vector(w << 8, h << 8));
	}

	public static Leaf hb(Component component, int w, int h) {
		return leaf(component, new Vector(w, h), new Vector(w << 8, h));
	}

	public static Leaf vb(Component component, int w, int h) {
		return leaf(component, new Vector(w, h), new Vector(w, h << 8));
	}

	public static Leaf fx(Component component, int w, int h) {
		return leaf(component, new Vector(w, h), new Vector(w, h));
	}

	private static Leaf leaf(Component component, Vector min, Vector max) {
		Leaf leaf = new Leaf();
		leaf.component = component;
		leaf.min = min;
		leaf.max = max;
		return leaf;
	}

	public static Box lay(Orientation ori, Node... nodes) {
		Box box = new Box();
		box.orientation = ori;
		box.nodes = Arrays.asList(nodes);
		return box;
	}

}
