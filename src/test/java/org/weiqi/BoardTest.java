package org.weiqi;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.util.Util;
import org.weiqi.Weiqi.Occupation;

public class BoardTest {

	@Test
	public void testNeighbour() {
		Coordinate c = Coordinate.c(10, 10);
		Set<Coordinate> neighbours = Util.createHashSet();

		for (Coordinate c1 : c.neighbours())
			neighbours.add(c1);

		assertEquals(new HashSet<Coordinate>( //
				Arrays.asList( //
						Coordinate.c(9, 10) //
						, Coordinate.c(11, 10) //
						, Coordinate.c(10, 9) //
						, Coordinate.c(10, 11) //
				)), neighbours);
	}

	@Test
	public void testEat() {
		Board board = new Board();
		board.move(Coordinate.c(0, 1), Occupation.BLACK);
		board.move(Coordinate.c(0, 0), Occupation.WHITE);
		board.move(Coordinate.c(1, 0), Occupation.BLACK);
		assertEquals(board.get(Coordinate.c(0, 0)), Occupation.EMPTY);
	}

	@Test
	public void testGroupAnalysis() {
		Board board = blackBoard();
		board.set(Coordinate.c(3, 3), Occupation.EMPTY);
		board.set(Coordinate.c(3, 4), Occupation.EMPTY);
		board.set(Coordinate.c(7, 3), Occupation.EMPTY);
		board.set(Coordinate.c(18, 0), Occupation.EMPTY);
		board.set(Coordinate.c(17, 1), Occupation.EMPTY);
		board.set(Coordinate.c(17, 0), Occupation.WHITE);
		board.set(Coordinate.c(18, 1), Occupation.WHITE);

		GroupAnalysis ga = new GroupAnalysis(board);
		Integer groupId = ga.getGroupId(Coordinate.c(15, 15));
		assertEquals(4, ga.getNumberOfBreathes(groupId));
	}

	private Board blackBoard() {
		Board board = new Board();
		for (Coordinate c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

}
