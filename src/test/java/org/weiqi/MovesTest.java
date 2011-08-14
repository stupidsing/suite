package org.weiqi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.weiqi.UctWeiqi.Visitor;
import org.weiqi.Weiqi.Occupation;

public class MovesTest {

	@Before
	public void before() {
		Weiqi.initialize();
	}

	@Test
	public void testNoMoveToFind() {
		Board board = blackBoard();
		board.set(Coordinate.c(2, 2), Occupation.EMPTY);

		Visitor visitor = new Visitor(new GameSet(board, Occupation.BLACK));
		assertTrue(visitor.elaborateMoves().isEmpty());
	}

	@Test
	public void testFindMove() {
		Board board = blackBoard();
		board.set(Coordinate.c(3, 3), Occupation.EMPTY);
		board.set(Coordinate.c(3, 5), Occupation.EMPTY);
		board.set(Coordinate.c(5, 3), Occupation.EMPTY);
		board.set(Coordinate.c(5, 5), Occupation.EMPTY);
		board.set(Coordinate.c(5, 6), Occupation.WHITE);
		board.set(Coordinate.c(8, 8), Occupation.EMPTY);
		board.set(Coordinate.c(9, 8), Occupation.EMPTY);

		Visitor visitor = new Visitor(new GameSet(board, Occupation.WHITE));
		assertEquals(2, visitor.elaborateMoves().size());
	}

	private Board blackBoard() {
		Board board = new Board();
		for (Coordinate c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

	@Test
	public void testRandom() {
		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);
		Visitor visitor = new Visitor(gameSet);
		boolean isWon = visitor.evaluateRandomOutcome();
		UserInterface.display(gameSet);
		System.out.println(isWon ? "WON" : "LOSS");
	}

}
