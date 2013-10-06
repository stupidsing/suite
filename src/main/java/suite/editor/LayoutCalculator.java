package suite.editor;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JComponent;

public class LayoutCalculator {

	public interface Node {
		public Dimension minimum();

		public Dimension maximum();
	}

	private class Leaf implements Node {
		private JComponent component;

		public Dimension minimum() {
			Dimension size = component.getMinimumSize();
			return size != null ? size : new Dimension(0, 0);
		}

		public Dimension maximum() {
			Dimension size = component.getMaximumSize();
			return size != null ? size : new Dimension(65536, 65536);
		}
	}

	private class Layout implements Node {
		private Orientation orientation;
		private List<Node> nodes;

		public Dimension minimum() {
			int minX = 0, minY = 0;

			for (Node node : nodes)
				if (orientation == Orientation.HORIZONTAL) {
					minX += node.minimum().width;
					minY = Math.max(minY, node.minimum().height);
				} else {
					minX = Math.max(minX, node.minimum().width);
					minY += node.minimum().height;
				}

			return new Dimension(minX, minY);
		}

		public Dimension maximum() {
			int maxX = 0, maxY = 0;

			for (Node node : nodes)
				if (orientation == Orientation.HORIZONTAL) {
					maxX += node.maximum().width;
					maxY = Math.max(maxY, node.maximum().height);
				} else {
					maxX = Math.max(maxX, node.maximum().width);
					maxY += node.maximum().height;
				}

			return new Dimension(maxX, maxY);
		}
	}

	private enum Orientation {
		HORIZONTAL, VERTICAL
	};

	public void arrange(Node node, Rectangle rectangle) {
		if (node instanceof Layout) {
			Layout layout = (Layout) node;
			Dimension minimum = layout.minimum();
			Dimension maximum = layout.maximum();

			int extraX = rectangle.width - minimum.width;
			int extraY = rectangle.height - minimum.height;
			int bufferX = maximum.width - minimum.width;
			int bufferY = maximum.height - minimum.height;

			if (layout.orientation == Orientation.HORIZONTAL) {
				double x = rectangle.getMinX();

				for (Node childNode : layout.nodes) {
					int width = childNode.minimum().width + rectangle.width * extraX / bufferX;
					arrange(childNode, new Rectangle(x, rectangle.getMinY(), x + width, rectangle.getMaxY()));
					x += width;
				}
			} else {
			}
		} else {
			Leaf leaf = (Leaf) node;
			leaf.component.setBounds(rectangle);
		}
	}
}
