package org.weiqi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.Random;

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

	@Test
	public void testRandomEvaluation() {
		int mid = Weiqi.SIZE / 2;

		String corner = evaluateRandomOutcome(Coordinate.c(0, 0));
		String faraway = evaluateRandomOutcome(Coordinate.c(1, 1));
		String center = evaluateRandomOutcome(Coordinate.c(mid, mid));
		System.out.println("CORNER: " + corner //
				+ ", FAR-AWAY: " + faraway //
				+ ", CENTER: " + center);

		// The corner move must be the worst
		assertTrue(Util.compare(corner, faraway) < 0);
		assertTrue(Util.compare(faraway, center) < 0);
	}

	private String evaluateRandomOutcome(Coordinate move) {
		Occupation player = Occupation.WHITE;
		int nWins = 0, nTotal = 1000;

		for (int i = 0; i < nTotal; i++) {
			Visitor visitor = new Visitor(new GameSet(new Board(), player));
			visitor.playMove(move);
			nWins += visitor.evaluateRandomOutcome() ? 0 : 1;
		}

		return nWins + "/" + nTotal;
	}

	@Test
	public void testRandomEvaluationTime() {
		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);
		int i = 0, ss[] = { 1000, 10000 };
		long start = 0, end = 0;

		for (int time = 0; time < 2; time++) {
			start = System.currentTimeMillis();
			for (; i < ss[1]; i++) {
				Visitor visitor = new Visitor(new GameSet(gameSet));
				visitor.evaluateRandomOutcome();
			}
			end = System.currentTimeMillis();
		}

		float duration = end - start;
		float ms = duration / (ss[1] - ss[0]);
		System.out.println("Random move: " + ms + "ms per evaluation");
	}

	@Test
	public void testUctSearchTime() {
		int nSimulations = 1000;
		long start = 0, end = 0;

		for (int time = 0; time < 2; time++) {
			Board board = new Board();
			Visitor visitor = new Visitor(new GameSet(board, Occupation.BLACK));
			UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
			search.setNumberOfSimulations(nSimulations);
			start = System.currentTimeMillis();
			search.search();
			end = System.currentTimeMillis();
		}

		float duration = end - start;
		float ms = duration / nSimulations;
		System.out.println("UCT: " + ms + "ms/simulation");
	}

	@Test
	public void testUctSearch() {
		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);
		gameSet.move(Coordinate.c(3, 3));

		Visitor visitor = new Visitor(gameSet);
		UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
		search.setNumberOfSimulations(1000);
		Coordinate bestMove = search.search();

		search.dumpSearch();
		System.out.println("BEST MOVE = " + bestMove);
	}

	@Test
	public void testUctFirstMove() {
		int seed = 760903274;
		System.out.println("RANDOM SEED = " + seed);
		RandomList.setSeed(seed);

		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);

		Visitor visitor = new Visitor(gameSet);
		UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
		search.setNumberOfThreads(1);
		search.setNumberOfSimulations(80000);

		Coordinate move = search.search();
		gameSet.move(move);

		// search.dumpSearch();
		System.out.println(move);
		assertTrue(move.getX() >= 2);
		assertTrue(move.getY() >= 2);
		assertTrue(move.getX() < Weiqi.SIZE - 2);
		assertTrue(move.getY() < Weiqi.SIZE - 2);
	}

	@Test
	public void testUctGame() {
		DecimalFormat df = new DecimalFormat("0.000");
		int nThreads = 2;
		int nSimulations = 20000;
		int boundedTime = 300000;
		int seed = new Random().nextInt();

		System.out.println("RANDOM SEED = " + seed);
		RandomList.setSeed(seed);

		Board board = new Board();
		GameSet gameSet = new GameSet(board, Occupation.BLACK);
		long current = System.currentTimeMillis();

		while (true) {
			Visitor visitor = new Visitor(new GameSet(gameSet));
			UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
			search.setNumberOfThreads(nThreads);
			search.setNumberOfSimulations(nSimulations);
			search.setBoundedTime(boundedTime);

			Coordinate move = search.search();
			if (move == null)
				break;

			long current0 = current;
			current = System.currentTimeMillis();
			Occupation player = gameSet.getNextPlayer();

			System.out.println("PRINCIPAL VARIATION: "
					+ search.dumpPrincipalVariation());
			System.out.println(player //
					+ " " + move //
					+ " " + df.format(search.getWinningChance()) //
					+ " " + (current - current0) + "ms");

			gameSet.move(move);
			UserInterface.display(gameSet);
		}
	}

}
