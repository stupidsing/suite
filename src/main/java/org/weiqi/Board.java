package org.weiqi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.weiqi.Weiqi.Array;
import org.weiqi.Weiqi.Occupation;

public class Board extends Array<Occupation> {

	public Board() {
		for (Coordinate c : Coordinate.getAll())
			set(c, Occupation.EMPTY);
	}

	public Board(Board board) {
		super(board);
	}

	public void move(Coordinate c, Occupation player) {
		Occupation current = get(c);

		if (current == Occupation.EMPTY) {
			set(c, player);
			GroupAnalysis ga = new GroupAnalysis(this);

			for (Coordinate neighbour : c.getNeighbours())
				if (neighbour.isWithinBoard())
					killIfDead(ga, neighbour);
			killIfDead(ga, c);
		} else
			throw new RuntimeException("Cannot move on occupied position");
	}

	public List<Coordinate> findAllMoves(Occupation player) {
		GroupAnalysis ga = new GroupAnalysis(this);

		List<Coordinate> moves = new ArrayList<Coordinate>( //
				Weiqi.SIZE * Weiqi.SIZE);

		for (Coordinate c : Coordinate.getAll())
			if (get(c) == Occupation.EMPTY) {
				Integer groupId = ga.getGroupId(c);
				boolean hasBreath;

				if (ga.getCoords(groupId).size() == 1) {
					hasBreath = false;

					for (Integer groupId1 : ga.getTouches(groupId)) {
						Occupation color = ga.getColors().get(groupId1);

						if (color == Occupation.EMPTY)
							hasBreath = true;
						else if (color == player)
							hasBreath |= ga.getBreathes(groupId1) > 1;
						else
							hasBreath |= ga.getBreathes(groupId1) <= 1;
					}
				} else
					hasBreath = true;

				if (hasBreath)
					moves.add(c);
			}

		return moves;
	}

	private void killIfDead(GroupAnalysis ga, Coordinate c) {
		Integer groupId = ga.getGroupId(c);

		if (ga.getBreathes(groupId) == 0)
			for (Coordinate c1 : ga.getCoords(groupId))
				set(c1, Occupation.EMPTY);
	}

	public Set<Coordinate> findGroup(Coordinate c) {
		Set<Coordinate> group = new HashSet<Coordinate>();
		findGroup(c, get(c), group);
		return group;
	}

	private void findGroup(Coordinate c, Occupation color, Set<Coordinate> group) {
		if (c.isWithinBoard() && get(c) == color && group.add(c))
			for (Coordinate neighbour : c.getNeighbours())
				findGroup(neighbour, color, group);
	}

}
