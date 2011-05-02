package org.weiqi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.weiqi.Weiqi.Array;
import org.weiqi.Weiqi.Occupation;

import com.google.common.collect.Multimap;

public class Board extends Array<Occupation> {

	public Board() {
		for (Coordinate c : Coordinate.getAll())
			set(c, Occupation.EMPTY);
	}

	public Board(Board board) {
		super(board);
	}

	public void move(Coordinate c, Occupation player) {
		GroupAnalysis ga = new GroupAnalysis(this);
		Occupation current = get(c);

		if (current == Occupation.EMPTY) {
			set(c, player);
			killIfDead(ga, c);
			for (Coordinate neighbour : c.getNeighbours())
				killIfDead(ga, neighbour);
		} else
			throw new RuntimeException("Cannot move on occupied position");
	}

	public List<Coordinate> findAllMoves(Occupation player) {
		List<Coordinate> moves = new ArrayList<Coordinate>( //
				Weiqi.SIZE * Weiqi.SIZE);

		for (Coordinate c : Coordinate.getAll())
			if (get(c) == Occupation.EMPTY) {
				set(c, player);

				boolean hasBreath = false;

				for (Coordinate neighbour : c.getNeighbours())
					if (neighbour.isWithinBoard())
						hasBreath |= !hasBreath(neighbour);

				hasBreath |= hasBreath(c);
			}

		return moves;
	}

	private void killIfDead(GroupAnalysis ga, Coordinate c) {
		Multimap<Integer, Coordinate> groupCoords = ga.getGroupCoords();
		Map<Integer, Integer> groupBreaths = ga.getGroupBreaths();
		Integer groupId = ga.getGroupIdArray().get(c);

		if (groupBreaths.get(groupId) == 0)
			for (Coordinate c1 : groupCoords.get(groupId))
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

	private boolean hasBreath(Coordinate c) {
		return hasBreath(c, get(c));
	}

	private boolean hasBreath(Coordinate c, Occupation player) {
		if (c.isWithinBoard()) {
			Occupation current = get(c);

			if (current == Occupation.EMPTY)
				return true;
			else if (current == player) {
				set(c, null); // Do not re-count
				boolean hasBreath = false;
				for (Coordinate neighbour : c.getNeighbours())
					if (hasBreath(neighbour, player)) {
						hasBreath = true;
						break;
					}
				set(c, current); // Set it back
				return hasBreath;
			} else
				return false;
		} else
			return false;
	}

}
