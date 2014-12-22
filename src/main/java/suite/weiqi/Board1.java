package suite.weiqi;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import suite.weiqi.Weiqi.Occupation;

/**
 * A (better) Weiqi board implementation using groups and union-find algorithm.
 *
 * @author ywsing
 */
public class Board1 {

	private Group board[] = new Group[Weiqi.size << Weiqi.shift];

	private class Group {
		private Occupation occupation;
		private int rank;
		private int nBreaths;
		private Group parent;

		private Group(Occupation occupation) {
			this.occupation = occupation;
		}

		private Group root() {
			return parent != null ? parent.root() : null;
		}
	}

	public void move(Coordinate c, Occupation o) {
		Group group = new Group(o);
		board[c.index()] = group;

		for (Coordinate c1 : c.neighbors) {
			Group group1 = get(c1);
			if (group1 != null) {
				group1.nBreaths--;
				if (group1.occupation == o)
					group = merge(group, group1);
			} else
				group.nBreaths++;
		}

		for (Coordinate c1 : c.neighbors) {
			Group group1 = get(c1);
			if (group1 != null && group1.nBreaths == 0)
				removeGroup(group1, c1);
		}

		if (group.nBreaths == 0)
			removeGroup(group, c);
	}

	private Group merge(Group g0, Group g1) {
		if (g0.rank < g1.rank) {
			Group tmp = g0;
			g0 = g1;
			g1 = tmp;
		}

		if (g0.rank == g1.rank)
			g0.rank++;

		g0.nBreaths += g1.nBreaths;
		return g1.parent = g0;
	}

	private void removeGroup(Group group, Coordinate c) {
		int index = c.index();

		if (board[index] == group) {
			board[index] = null;
			Set<Group> neighbourGroups = new HashSet<>();
			for (Coordinate c1 : c.neighbors) {
				Group g = get(c1);
				if (g == group)
					removeGroup(g, c1);
				else if (g != null && g != group)
					neighbourGroups.add(g);
			}
			for (Group neighbourGroup : neighbourGroups)
				neighbourGroup.nBreaths++;
		}
	}

	public int hashCode() {
		int i = 0;
		for (Coordinate c : Coordinate.all()) {
			Group g = get(c);
			Occupation o = g != null ? g.occupation : Occupation.EMPTY;
			i = i * 31 + Objects.hashCode(o);
		}
		return i;
	}

	private Group get(Coordinate c) {
		int index = c.index();
		Group group = board[index];
		if (group != null)
			group = board[index] = group.root();
		return group;
	}

}
