package suite.editor;

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
			Vector minimum = box.minimum();
			Vector maximum = box.maximum();

			int extra = rect.width(ori) - minimum.width(ori);
			int buffer = maximum.width(ori) - minimum.width(ori);
			int w = rect.v0.width(ori);

			for (Node childNode : box.nodes) {
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

}
