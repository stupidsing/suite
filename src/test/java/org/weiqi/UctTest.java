package org.weiqi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.weiqi.UctWeiqi.Visitor;
import org.weiqi.Weiqi.Occupation;

public class UctTest {

	@Test
	public void testNoMoveToFind() {
		Board board = blackBoard();
		board.set(new Coordinate(2, 2), Occupation.EMPTY);

		Visitor visitor = new Visitor(board, Occupation.BLACK);
		assertTrue(visitor.findAllMoves().isEmpty());
	}

	@Test
	public void testFindMove() {
		Board board = blackBoard();
		board.set(new Coordinate(3, 3), Occupation.EMPTY);
		board.set(new Coordinate(3, 5), Occupation.EMPTY);
		board.set(new Coordinate(5, 3), Occupation.EMPTY);
		board.set(new Coordinate(5, 5), Occupation.EMPTY);
		board.set(new Coordinate(5, 6), Occupation.WHITE);
		board.set(new Coordinate(8, 8), Occupation.EMPTY);
		board.set(new Coordinate(9, 8), Occupation.EMPTY);

		Visitor visitor = new Visitor(board, Occupation.WHITE);
		assertEquals(2, visitor.findAllMoves().size());
	}

	@Test
	public void testRandom() {
		Board board = new Board();
		new Visitor(board, Occupation.BLACK).evaluateRandomOutcome();
		UserInterface.display(board);
	}

	@Test
	public void testUctSearch() {
		Board board = new Board();
		Visitor visitor = new Visitor(board, Occupation.BLACK);
		new UctSearch<Coordinate>(visitor).search();
	}

	private Board blackBoard() {
		Board board = new Board();
		for (Coordinate c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

}
