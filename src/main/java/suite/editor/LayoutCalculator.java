package suite.editor;

import java.awt.Dimension;
import java.awt.Rectangle;

import suite.editor.Layout.Box;
import suite.editor.Layout.Leaf;
import suite.editor.Layout.Node;
import suite.editor.Layout.Orientation;
import suite.editor.Layout.Rect;
import suite.editor.Layout.Vector;

public class LayoutCalculator {

	public void arrange(Node node, Rect rect) {
		if (node instanceof Box) {
			Box box = (Box) node;
			Orientation ori = box.orientation;
			Vector minimum = minimum(box);
			Vector maximum = maximum(box);

			int extra = rect.width(ori) - minimum.width(ori);
			int buffer = maximum.width(ori) - minimum.width(ori);
			int w = rect.v0.width(ori);

			for (Node childNode : box.nodes) {
				Vector max = maximum(childNode);
				Vector min = minimum(childNode);
				int w0 = min.width(ori) + extra * (max.width(ori) - min.width(ori)) / buffer;

				Vector v0 = new Vector(ori, w, rect.v0.height(ori));
				Vector v1 = new Vector(ori, w + w0, rect.v1.height(ori));
				arrange(childNode, new Rect(v0, v1));

				w += w0;
			}
		} else
			((Leaf) node).component.setBounds(toRectangle(rect));
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
			Vector size = toVector(leaf.component.getMaximumSize());
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
			Vector size = toVector(leaf.component.getMinimumSize());
			minimum = size != null ? size : new Vector(0, 0);
		}

		return minimum;
	}

	private Rectangle toRectangle(Rect rect) {
		return new Rectangle(rect.v0.x, rect.v1.y, rect.xdiff(), rect.ydiff());
	}

	private static Vector toVector(Dimension dim) {
		return dim != null ? new Vector(dim.width, dim.height) : null;
	}

}
