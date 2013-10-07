package suite.editor;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;

public class Layout {

	public static class Leaf implements Node {
		public JComponent component;

		public Vector minimum() {
			Vector size = toVector(component.getMinimumSize());
			return size != null ? size : new Vector(0, 0);
		}

		public Vector maximum() {
			Vector size = toVector(component.getMaximumSize());
			return size != null ? size : new Vector(65536, 65536);
		}
	}

	public static class Box implements Node {
		public Orientation orientation;
		public List<Node> nodes;

		public Vector minimum() {
			int minWidth = 0, minHeight = 0;

			for (Node node : nodes) {
				Vector min = node.minimum();
				minWidth += min.width(orientation);
				minHeight = Math.max(minHeight, min.height(orientation));
			}

			return new Vector(orientation, minWidth, minHeight);
		}

		public Vector maximum() {
			int maxWidth = 0, maxHeight = 0;

			for (Node node : nodes) {
				Vector max = node.maximum();
				maxWidth += max.width(orientation);
				maxHeight = Math.max(maxHeight, max.height(orientation));
			}

			return new Vector(orientation, maxWidth, maxHeight);
		}
	}

	public interface Node {
		public Vector minimum();

		public Vector maximum();
	}

	public static class Vector {
		public int x, y;

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
	}

	public static class Rect {
		public Vector v0, v1;

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
	}

	public enum Orientation {
		HORIZONTAL, VERTICAL
	}

	public Leaf c(JComponent component) {
		Leaf leaf = new Leaf();
		leaf.component = component;
		return leaf;
	}

	public Box layout(Orientation ori, Node... nodes) {
		Box box = new Box();
		box.orientation = ori;
		box.nodes = Arrays.asList(nodes);
		return box;
	}

	private static Vector toVector(Dimension dim) {
		return dim != null ? new Vector(dim.width, dim.height) : null;
	}

}
