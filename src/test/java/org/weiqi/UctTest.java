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
		board.set(Coordinate.c(2, 2), Occupation.EMPTY);

		Visitor visitor = new Visitor(board, Occupation.BLACK);
		assertTrue(visitor.findAllMoves().isEmpty());
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
		UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
		search.setNumberOfSimulations(50);
		System.out.println(search.search());
	}

	@Test
	public void testUctGame() {
		Board board = new Board();
		Occupation player = Occupation.BLACK;

		for (int i = 0; i < Weiqi.SIZE * Weiqi.SIZE; i++) {
			Visitor visitor = new Visitor(board, player);
			UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
			search.setNumberOfSimulations(10000);

			Coordinate move = search.search();
			board.move(move, player);
			player = player.opponent();

			System.out.println(move);
			UserInterface.display(board);
		}
	}

	private Board blackBoard() {
		Board board = new Board();
		for (Coordinate c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

}
