package suite.weiqi;

import suite.weiqi.Weiqi.Occupation;

import java.util.*;

public class GroupAnalysis {

	private Board board;

	private Map<Coordinate, Group> groupByCoord = new HashMap<>();
	private Set<Group> groups = new TreeSet<>();

	public static class Group implements Comparable<Group> {
		public Occupation color;
		public Collection<Coordinate> coords = new ArrayList<>();
		public Collection<Group> touches = new TreeSet<>();
		public Collection<Coordinate> breathes = new TreeSet<>();
		private int id;
		private Group parent;

		private Group(int id, Occupation color) {
			this.id = id;
			this.color = color;
		}

		private Group root() {
			var root = this;
			while (root.parent != null)
				root = root.parent;
			return root;
		}

		@Override
		public int compareTo(Group group) {
			return id - group.id;
		}
	}

	public GroupAnalysis(Board board) {
		this.board = board;
		assignGroups();
		assignGroupSurroundings();
	}

	private void assignGroups() {
		var nGroups = 0;

		for (var c : Coordinate.all()) {
			var color = board.get(c);
			Group group = null; // must be root

			for (var c1 : c.leftOrUp)
				if (board.get(c1) == color) {
					var group1 = groupByCoord.get(c1).root();

					if (group != null) {
						if (group != group1)
							group1.parent = group;
					} else
						group = group1;
				}

			if (group == null)
				group = new Group(nGroups++, color);

			groupByCoord.put(c, group);
		}

		for (var c : Coordinate.all()) {
			var group = groupByCoord.get(c).root();
			groupByCoord.put(c, group);
			group.coords.add(c);
			groups.add(group);
		}
	}

	private void assignGroupSurroundings() {
		for (var c : Coordinate.all()) {
			var group = groupByCoord.get(c);

			for (var c1 : c.leftOrUp) {
				var group1 = groupByCoord.get(c1);

				if (group != group1) {
					group.touches.add(group1);
					group1.touches.add(group);
				}
			}

			if (board.get(c) == Occupation.EMPTY)
				for (var c1 : c.neighbors)
					groupByCoord.get(c1).breathes.add(c);
		}
	}

	public Set<Group> getGroups() {
		return groups;
	}

	public Group getGroup(Coordinate c) {
		return groupByCoord.get(c);
	}

}
