package suite.editor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import primal.MoreVerbs.Read;

public class LayoutCalculator {

	private Container container;

	private Space space = new Space();

	public class HBox extends Group implements Node {
		private HBox(List<Portion> portions) {
			super(portions);
		}

		public void assign(Rect rect) {
			arrange(this, rect.v0.x, rect.v1.x, (portion, start, end) -> {
				portion.node.assign(new Rect(new Vector(start, rect.v0.y), new Vector(end, rect.v1.y)));
			});
		}
	}

	public class VBox extends Group implements Node {
		private VBox(List<Portion> portions) {
			super(portions);
		}

		public void assign(Rect rect) {
			arrange(this, rect.v0.y, rect.v1.y, (portion, start, end) -> {
				portion.node.assign(new Rect(new Vector(rect.v0.x, start), new Vector(rect.v1.x, end)));
			});
		}
	}

	private abstract class Group implements Node {
		private List<Portion> portions = new ArrayList<>();

		private Group(List<Portion> portions) {
			this.portions = portions;
		}

		public boolean isVisible() {
			return portions.stream()
					.filter(portion -> !(portion.node instanceof Space))
					.anyMatch(portion -> portion.node.isVisible());
		}
	}

	public class Leaf implements Node {
		private Component component;

		private Leaf(Component component) {
			this.component = component;
		}

		public boolean isVisible() {
			return component == null || component.isVisible();
		}

		public void assign(Rect r) {
			var awtRect = new Rectangle(r.v0.x, r.v0.y, r.v1.x - r.v0.x, r.v1.y - r.v0.y);
			component.setBounds(awtRect);
			container.add(component);
		}
	}

	public class Space implements Node {
		public boolean isVisible() {
			return true;
		}

		public void assign(Rect r) {
		}
	}

	public interface Node {
		public boolean isVisible();

		public void assign(Rect rect);
	}

	public class Portion {
		private int minUnit;
		private int maxUnit;
		private Node node;

		private Portion(int minUnit, int maxUnit, Node node) {
			this.minUnit = minUnit;
			this.maxUnit = maxUnit;
			this.node = node;
		}
	}

	public enum Orientation {
		HORIZONTAL, VERTICAL
	};

	public class Rect {
		private Vector v0, v1;

		public Rect(Vector v0, Vector v1) {
			this.v0 = v0;
			this.v1 = v1;
		}
	}

	public class Vector {
		private int x, y;

		public Vector(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	private interface AssignToPortion {
		public void assign(Portion portion, int start, int end);
	}

	public LayoutCalculator(Container container) {
		this.container = container;
	}

	public Node boxh(Portion... portions) {
		return new HBox(List.of(portions));
	}

	public Node boxv(Portion... portions) {
		return new VBox(List.of(portions));
	}

	/**
	 * Fixed-size portion.
	 */
	public Portion fx(int unit, Node node) {
		return p(unit, unit, node);
	}

	/**
	 * Extensible portion.
	 */
	public Portion ex(int unit, Node node) {
		return p(unit, unit << 8, node);
	}

	public Portion p(int minUnit, int maxUnit, Node node) {
		return new Portion(minUnit, maxUnit, node);
	}

	public Node c(Component component) {
		return new Leaf(component);
	}

	/**
	 * Blank space.
	 */
	public Node b() {
		return space;
	}

	public void arrange(Node node) {
		container.setLayout(null);
		var size = container.getSize();
		var rect = new Rect(new Vector(0, 0), new Vector(size.width, size.height));
		node.assign(rect);
	}

	private void arrange(Group group, int startPos, int endPos, AssignToPortion assignToPortion) {
		var totalAssigned = endPos - startPos;

		var portions = Read
				.from(group.portions)
				.filter(portion -> portion.node.isVisible())
				.toList();

		var totalMin = portions.stream().mapToInt(p -> p.minUnit).sum();
		var totalMax = portions.stream().mapToInt(p -> p.maxUnit).sum();

		if (totalMin < totalAssigned) {
			var num = totalAssigned - totalMin;
			var denom = totalMax - totalMin;
			var ratio = (float) num / denom;
			var accumulatedBase = 0;
			var accumulatedExpand = 0;
			var assignedPos = 0;

			for (var portion : portions) {
				var assignedPos0 = assignedPos;
				accumulatedBase += portion.minUnit;
				accumulatedExpand += portion.maxUnit - portion.minUnit;
				assignedPos = startPos + accumulatedBase + (int) (accumulatedExpand * ratio);
				assignToPortion.assign(portion, assignedPos0, assignedPos);
			}
		} else {
			var nom = totalAssigned;
			var denom = totalMin;
			var ratio = nom / denom;
			var accumulated = 0;
			var assignedPos = 0;

			for (var portion : portions) {
				var assignedPos0 = assignedPos;
				accumulated += portion.minUnit;
				assignedPos = startPos + (int) (accumulated * ratio);
				assignToPortion.assign(portion, assignedPos0, assignedPos);
			}
		}
	}

}
