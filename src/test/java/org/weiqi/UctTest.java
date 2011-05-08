package org.weiqi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.util.Util;
import org.weiqi.UctWeiqi.Visitor;
import org.weiqi.Weiqi.Occupation;
import org.weiqi.uct.UctSearch;

public class UctTest {

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

	@Test
	public void testRandom() {
		Board board = new Board();
		Visitor visitor = new Visitor(new GameSet(board, Occupation.BLACK));
		boolean isWon = visitor.evaluateRandomOutcome();
		UserInterface.display(board);
		System.out.println(isWon ? "WON" : "LOSS");
	}

	@Test
	public void testEvaluateRandomGame() {
		int mid = Weiqi.SIZE / 2;

		String corner = evaluateRandomOutcome(Coordinate.c(0, 0));
		String center = evaluateRandomOutcome(Coordinate.c(mid, mid));
		System.out.println("CORNER: " + corner + ", CENTER: " + center);

		// The corner move must be the worst
		assertTrue(Util.compare(corner, center) < 0);
	}

	private String evaluateRandomOutcome(Coordinate move) {
		Occupation player = Occupation.WHITE;
		int nWins = 0, nTotal = 1000;

		for (int i = 0; i < nTotal; i++) {
			Visitor visitor = new Visitor(new GameSet(new Board(), player));
			visitor.playMove(move);
			nWins += visitor.evaluateRandomOutcome() ? 0 : 1;
		}

		String outcome = nWins + "/" + nTotal;
		return outcome;
	}

	@Test
	public void testUctSearch() {
		Board board = new Board();
		Visitor visitor = new Visitor(new GameSet(board, Occupation.BLACK));
		UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
		search.setNumberOfSimulations(10000);
		Coordinate bestMove = search.search();

		search.dumpSearch();
		System.out.println("BEST MOVE = " + bestMove);
	}

	@Test
	public void testUctFirstMove() {
		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);

		Visitor visitor = new Visitor(gameSet);
		UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
		search.setNumberOfSimulations(10000);

		Coordinate move = search.search();
		gameSet.move(move);

		System.out.println(move);
	}

	@Test
	public void testUctGame() {
		Board board = new Board();
		GameSet gameSet = new GameSet(board, Occupation.BLACK);

		for (int i = 0; i < 2 * Weiqi.SIZE * Weiqi.SIZE; i++) {
			Visitor visitor = new Visitor(new GameSet(gameSet));
			UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
			search.setNumberOfSimulations(10000);

			Coordinate move = search.search();
			if (move == null)
				break;

			gameSet.move(move);

			System.out.println(move);
			UserInterface.display(gameSet);
		}
	}

	private Board blackBoard() {
		Board board = new Board();
		for (Coordinate c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

}
