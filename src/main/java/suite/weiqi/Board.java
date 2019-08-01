package suite.weiqi;

import static primal.statics.Fail.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import primal.Verbs.Build;
import suite.weiqi.Weiqi.Array;
import suite.weiqi.Weiqi.Occupation;

public class Board extends Array<Occupation> {

	public Board() {
		for (var c : Coordinate.all())
			set(c, Occupation.EMPTY);
	}

	public Board(Board board) {
		super(board);
	}

	public enum MoveType {
		PLACEMENT, CAPTURE, INVALID
	}

	/**
	 * Plays a move on the Weiqi board. Do not check for repeats in game state
	 * history since Board do not have them. Use GameSet.moveIfPossible() for
	 * the rule-accordance version.
	 *
	 * This method do not take use of GroupAnalysis for performance reasons.
	 */
	public MoveType playIfSeemsPossible(Coordinate c, Occupation player) {
		MoveType type;
		var current = get(c);
		var opponent = player.opponent();

		if (current == Occupation.EMPTY) {
			type = MoveType.PLACEMENT;
			set(c, player);

			for (var neighbor : c.neighbors)
				if (get(neighbor) == opponent && killIfDead(neighbor))
					type = MoveType.CAPTURE;

			if (!hasBreath(c)) {
				set(c, Occupation.EMPTY);
				type = MoveType.INVALID;
			}
		} else
			type = MoveType.INVALID;

		return type;
	}

	private boolean killIfDead(Coordinate c) {
		var isKilled = !hasBreath(c);

		if (isKilled)
			for (var c1 : findGroup(c))
				set(c1, Occupation.EMPTY);

		return isKilled;
	}

	private boolean hasBreath(Coordinate c) {
		var group = new HashSet<>();
		var color = get(c);
		group.add(c);

		var unexplored = new Stack<Coordinate>();
		unexplored.push(c);

		while (!unexplored.isEmpty())
			for (var c1 : unexplored.pop().neighbors) {
				var color1 = get(c1);

				if (color1 == color) {
					if (group.add(c1))
						unexplored.push(c1);
				} else if (color1 == Occupation.EMPTY)
					return true;
			}

		return false;
	}

	public Set<Coordinate> findGroup(Coordinate c) {
		var group = new HashSet<Coordinate>();
		var color = get(c);
		group.add(c);

		var unexplored = new Stack<Coordinate>();
		unexplored.push(c);

		while (!unexplored.isEmpty())
			for (var c1 : unexplored.pop().neighbors)
				if (get(c1) == color && group.add(c1))
					unexplored.push(c1);

		return group;
	}

	/**
	 * Plays a move on the Weiqi board. Uses group analysis which is slower.
	 */
	public void play1(Coordinate c, Occupation player) {
		var current = get(c);
		var opponent = player.opponent();

		if (current == Occupation.EMPTY) {
			set(c, player);
			var ga = new GroupAnalysis(this);

			for (var neighbor : c.neighbors)
				if (get(neighbor) == opponent)
					killIfDead1(ga, neighbor);

			if (killIfDead1(ga, c))
				fail("cannot perform suicide move");
		} else
			fail("cannot move on occupied position");
	}

	private boolean killIfDead1(GroupAnalysis ga, Coordinate c) {
		var group = ga.getGroup(c);
		var isKilled = group.breathes.isEmpty();

		if (isKilled)
			for (var c1 : group.coords)
				set(c1, Occupation.EMPTY);

		return isKilled;
	}

	@Override
	public String toString() {
		return Build.string(sb -> {
			for (var x = 0; x < Weiqi.size; x++) {
				for (var y = 0; y < Weiqi.size; y++) {
					var c = Coordinate.c(x, y);
					sb.append(get(c).display() + " ");
				}
				sb.append("\n");
			}
		});
	}

}
