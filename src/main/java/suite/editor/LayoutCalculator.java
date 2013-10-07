package suite.editor;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;

public class LayoutCalculator {

	public interface Node {
		public Vector minimum();

		public Vector maximum();
	}

	private class Leaf implements Node {
		private JComponent component;

		public Vector minimum() {
			Vector size = toVector(component.getMinimumSize());
			return size != null ? size : new Vector(0, 0);
		}

		public Vector maximum() {
			Vector size = toVector(component.getMaximumSize());
			return size != null ? size : new Vector(65536, 65536);
		}
	}

	private class Layout implements Node {
		private Orientation orientation;
		private List<Node> nodes;

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

	private static class Vector {
		private int x, y;

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

	private static class Rect {
		private Vector v0, v1;

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

	private enum Orientation {
		HORIZONTAL, VERTICAL
	}

	public void arrange(Node node, Rect rect) {
		if (node instanceof Layout) {
			Layout layout = (Layout) node;
			Orientation ori = layout.orientation;
			Vector minimum = layout.minimum();
			Vector maximum = layout.maximum();

			int extra = rect.width(ori) - minimum.width(ori);
			int buffer = maximum.width(ori) - minimum.width(ori);
			int w = rect.v0.width(ori);

			for (Node childNode : layout.nodes) {
				Vector max = childNode.maximum();
				Vector min = childNode.minimum();
				int w0 = min.width(ori) + extra * (max.width(ori) - min.width(ori)) / buffer;

				Vector v0 = new Vector(ori, w, rect.v0.height(ori));
				Vector v1 = new Vector(ori, w + w0, rect.v1.height(ori));
				arrange(childNode, new Rect(v0, v1));

				w += w0;
			}
		} else
			((Leaf) node).component.setBounds(toRectangle(rect));
	}

	private Rectangle toRectangle(Rect rect) {
		return new Rectangle(rect.v0.x, rect.v1.y, rect.xdiff(), rect.ydiff());
	}

	private Vector toVector(Dimension dim) {
		return dim != null ? new Vector(dim.width, dim.height) : null;
	}

}
