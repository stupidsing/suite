package suite.weiqi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import suite.util.Util;
import suite.weiqi.GroupAnalysis.Group;
import suite.weiqi.Weiqi.Occupation;

public class BoardTest {

	@Before
	public void before() {
		Weiqi.initialize();
	}

	@Test
	public void testNeighbor() {
		Coordinate c = Coordinate.c(10, 10);
		Set<Coordinate> neighbors = new HashSet<>();

		for (Coordinate c1 : c.neighbors)
			neighbors.add(c1);

		assertEquals(Util.set( //
				Coordinate.c(9, 10) //
				, Coordinate.c(11, 10) //
				, Coordinate.c(10, 9) //
				, Coordinate.c(10, 11) //
		), neighbors);
	}

	@Test
	public void testEat() {
		Board board = new Board();
		board.playIfSeemsPossible(Coordinate.c(0, 1), Occupation.BLACK);
		board.playIfSeemsPossible(Coordinate.c(0, 0), Occupation.WHITE);
		board.playIfSeemsPossible(Coordinate.c(1, 0), Occupation.BLACK);
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
		Group blackGroup = ga.getGroup(Coordinate.c(15, 15));
		Group whiteGroup = ga.getGroup(Coordinate.c(17, 0));

		assertEquals(361 - 7, blackGroup.coords.size());
		assertEquals(3, whiteGroup.touches.size());
		assertTrue(blackGroup.touches.contains(whiteGroup));
		assertTrue(whiteGroup.touches.contains(blackGroup));
		assertEquals(4, blackGroup.breathes.size());
	}

	private Board blackBoard() {
		Board board = new Board();
		for (Coordinate c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

}
