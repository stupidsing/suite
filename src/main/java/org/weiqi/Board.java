package org.weiqi;

import java.util.ArrayList;
import java.util.List;

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
		Occupation opponent = player.opponent();

		if (current == Occupation.EMPTY) {
			set(c, player);
			GroupAnalysis ga = new GroupAnalysis(this);

			for (Coordinate neighbour : c.getNeighbours())
				if (neighbour.isWithinBoard() && get(neighbour) == opponent)
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
						Occupation color = ga.getColor(groupId1);

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

}
