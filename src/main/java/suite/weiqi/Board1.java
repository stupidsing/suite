package suite.weiqi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;

import suite.streamlet.FunUtil.Fun;
import suite.weiqi.Weiqi.Occupation;

/**
 * A (better) Weiqi board implementation using groups and union-find algorithm.
 *
 * @author ywsing
 */
public class Board1 {

	private Group[] board = new Group[Weiqi.size << Weiqi.shift];
	private int hashCode;

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

	public Board1() {
	}

	public Board1(Board1 board1) {
		var map = new IdentityHashMap<Group, Group>();
		var clone = new ArrayList<Fun<Group, Group>>();
		clone.add(group0 -> {
			Group group1;
			if (group0 != null) {
				if ((group1 = map.get(group0)) == null) {
					map.put(group0, group1 = new Group(group0.occupation));
					group1.rank = group0.rank;
					group1.nBreaths = group0.nBreaths;
					group1.parent = clone.get(0).apply(group0.parent);
				}
			} else
				group1 = null;
			return group1;
		});

		for (var i = 0; i < board1.board.length; i++)
			setGroup(i, clone.get(0).apply(board1.board[i]));
	}

	public Runnable move(Coordinate c, Occupation o) {
		var group = new Group(o);
		setGroup(c, group);

		for (var c1 : c.neighbors) {
			var group1 = getGroup(c1);
			if (group1 != null) {
				group1.nBreaths--;
				if (group1.occupation == o)
					group = merge(group, group1);
			} else
				group.nBreaths++;
		}

		var list = new ArrayList<Coordinate>();

		for (var c1 : c.neighbors) {
			var group1 = getGroup(c1);
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
				removeGroup(getGroup(c), c);
				for (var c1 : list)
					fillGroup(new Group(o.opponent()), c1);
			};
	}

	public Occupation get(Coordinate c) {
		return getGroup(c).occupation;
	}

	public int hashCode() {
		return hashCode;
	}

	private Group merge(Group g0, Group g1) {
		if (g0.rank < g1.rank) {
			var tmp = g0;
			g0 = g1;
			g1 = tmp;
		}

		if (g0.rank == g1.rank)
			g0.rank++;

		g0.nBreaths += g1.nBreaths;
		return g1.parent = g0;
	}

	private void fillGroup(Group group, Coordinate c) {
		var neighbourGroups = new HashSet<>();
		setGroup(c, group);

		for (var c1 : c.neighbors) {
			var g = getGroup(c1);
			if (g == null)
				fillGroup(group, c1);
			else if (g != group && neighbourGroups.add(g))
				g.nBreaths--;
		}
	}

	private void removeGroup(Group group, Coordinate c) {
		var neighbourGroups = new HashSet<>();
		setGroup(c, null);

		for (var c1 : c.neighbors) {
			var g = getGroup(c1);
			if (g == group)
				removeGroup(g, c1);
			else if (g != null && neighbourGroups.add(g))
				g.nBreaths++;
		}
	}

	private void setGroup(Coordinate c, Group group) {
		setGroup(c.index(), group);
	}

	private void setGroup(int i, Group group1) {
		var group0 = board[i];
		board[i] = group1;
		if (group0 != null)
			hashCode ^= group0.occupation == Occupation.BLACK ? i : Integer.rotateLeft(i, 16);
		if (group1 != null)
			hashCode ^= group1.occupation == Occupation.BLACK ? i : Integer.rotateLeft(i, 16);
	}

	private Group getGroup(Coordinate c) {
		var index = c.index();
		var group = board[index];
		if (group != null)
			group = board[index] = group.root();
		return group;
	}

}
