package suite.editor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;

import suite.editor.Layout.Box;
import suite.editor.Layout.Leaf;
import suite.editor.Layout.Node;
import suite.editor.Layout.Orientation;
import suite.editor.Layout.Rect;
import suite.editor.Layout.Vector;

public class LayoutCalculator {

	public void arrange(Container container, Node node) {
		container.setLayout(null);

		Rect rect = new Rect(new Vector(0, 0), toVector(container.getSize()));
		arrange(container, rect, node);
	}

	private void arrange(Container container, Rect rect, Node node) {
		if (node instanceof Box) {
			Box box = (Box) node;
			Orientation ori = box.orientation;
			Vector minimum = minimum(box);
			Vector maximum = maximum(box);

			int extra = rect.width(ori) - minimum.width(ori);
			int buffer = maximum.width(ori) - minimum.width(ori);
			int w = rect.v0.width(ori);

			if (buffer == 0) // Avoids division by zero
				buffer = Integer.MAX_VALUE;

			for (Node childNode : box.nodes) {
				Vector max = maximum(childNode);
				Vector min = minimum(childNode);
				int w0 = min.width(ori) + extra * (max.width(ori) - min.width(ori)) / buffer;

				Vector v0 = new Vector(ori, w, rect.v0.height(ori));
				Vector v1 = new Vector(ori, w + w0, rect.v1.height(ori));
				arrange(container, new Rect(v0, v1), childNode);

				w += w0;
			}
		} else if (node instanceof Leaf) {
			Component component = ((Leaf) node).component;

			if (component != null) {
				component.setBounds(toRectangle(rect));
				container.add(component);
			}
		}
	}

	private Vector maximum(Node node) {
		Vector maximum;

		if (node instanceof Box) {
			Box box = (Box) node;
			Orientation ori = box.orientation;
			int maxWidth = 0, maxHeight = 0;

			for (Node childNode : box.nodes) {
				Vector max = maximum(childNode);
				maxWidth += max.width(ori);
				maxHeight = Math.max(maxHeight, max.height(ori));
			}

			maximum = new Vector(ori, maxWidth, maxHeight);
		} else {
			Leaf leaf = (Leaf) node;
			boolean isVisible = leaf.component == null || leaf.component.isVisible();
			Vector size = isVisible ? leaf.max : new Vector(0, 0);
			maximum = size != null ? size : new Vector(65536, 65536);
		}

		return maximum;
	}

	private Vector minimum(Node node) {
		Vector minimum;

		if (node instanceof Box) {
			Box box = (Box) node;
			Orientation ori = box.orientation;
			int minWidth = 0, minHeight = 0;

			for (Node childNode : box.nodes) {
				Vector min = minimum(childNode);
				minWidth += min.width(ori);
				minHeight = Math.max(minHeight, min.height(ori));
			}

			minimum = new Vector(ori, minWidth, minHeight);
		} else {
			Leaf leaf = (Leaf) node;
			boolean isVisible = leaf.component == null || leaf.component.isVisible();
			Vector size = isVisible ? leaf.min : null;
			minimum = size != null ? size : new Vector(0, 0);
		}

		return minimum;
	}

	private Rectangle toRectangle(Rect rect) {
		return new Rectangle(rect.v0.x, rect.v0.y, rect.xdiff(), rect.ydiff());
	}

	private static Vector toVector(Dimension dim) {
		return dim != null ? new Vector(dim.width, dim.height) : null;
	}

}
