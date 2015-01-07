package suite.weiqi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

	public Runnable move(Coordinate c, Occupation o) {
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

		List<Coordinate> list = new ArrayList<>();

		for (Coordinate c1 : c.neighbors) {
			Group group1 = get(c1);
			if (group1 != null && group1.nBreaths == 0) {
				removeGroup(group1, c1);
				list.add(c1);
			}
		}

		if (group.nBreaths == 0) {
			removeGroup(group, c);
			return () -> {
			};
		} else
			return () -> {
				removeGroup(get(c), c);
				for (Coordinate c1 : list)
					fillGroup(new Group(o.opponent()), c1);
			};
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

	private void fillGroup(Group group, Coordinate c) {
		Set<Group> neighbourGroups = new HashSet<>();
		board[c.index()] = group;

		for (Coordinate c1 : c.neighbors) {
			Group g = get(c1);
			if (g == null)
				fillGroup(group, c1);
			else if (g != group && neighbourGroups.add(g))
				g.nBreaths--;
		}
	}

	private void removeGroup(Group group, Coordinate c) {
		Set<Group> neighbourGroups = new HashSet<>();
		board[c.index()] = null;

		for (Coordinate c1 : c.neighbors) {
			Group g = get(c1);
			if (g == group)
				removeGroup(g, c1);
			else if (g != null && neighbourGroups.add(g))
				g.nBreaths++;
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
