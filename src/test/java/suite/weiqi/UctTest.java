package suite.weiqi;

import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import suite.Constants;
import suite.os.Stopwatch;
import suite.sample.Profiler;
import suite.uct.ShuffleUtil;
import suite.uct.UctSearch;
import suite.uct.UctVisitor;
import suite.uct.UctWeiqi;
import suite.util.Object_;
import suite.weiqi.Weiqi.Occupation;

public class UctTest {

	@Before
	public void before() {
		Weiqi.adjustSize(7);
	}

	@Test
	public void testRandomEvaluation() {
		int seed = 214636368;
		System.out.println("RANDOM SEED = " + seed);
		ShuffleUtil.setSeed(seed);
		int mid = Weiqi.size / 2;

		String corner = evaluateRandomOutcome(Coordinate.c(0, 0));
		String faraway = evaluateRandomOutcome(Coordinate.c(1, 1));
		String center = evaluateRandomOutcome(Coordinate.c(mid, mid));
		System.out.println("CORNER: " + corner //
				+ ", FAR-AWAY: " + faraway //
				+ ", CENTER: " + center);

		// the corner move must be the worst
		assertTrue(Object_.compare(corner, faraway) < 0);
		assertTrue(Object_.compare(faraway, center) < 0);
	}

	private String evaluateRandomOutcome(Coordinate move) {
		Occupation player = Occupation.WHITE;
		int nWins = 0, nTotal = 1000;

		for (int i = 0; i < nTotal; i++) {
			GameSet gameSet = new GameSet(new Board(), player);
			UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet);
			visitor.playMove(move);
			nWins += visitor.evaluateRandomOutcome() ? 0 : 1;
		}

		return nWins + "/" + nTotal;
	}

	@Test
	public void testRandomEvaluationTime() {
		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);
		int i = 0, ss[] = { 1000, 10000, };
		float duration = 0f;

		for (int time = 0; time < 2; time++)
			for (; i < ss[time]; i++)
				duration = Stopwatch.of(() -> {
					GameSet gameSet1 = new GameSet(gameSet);
					UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet1);
					visitor.evaluateRandomOutcome();
					return null;
				}).duration;

		float ms = duration / (ss[1] - ss[0]);
		System.out.println("Random move: " + ms + "ms per evaluation");
	}

	@Test
	public void testUctSearchTime() {
		int nSimulations = 1000;
		float duration = 0f;

		for (int time = 0; time < 2; time++) {
			GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);
			UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet);
			UctSearch<Coordinate> search = new UctSearch<>(visitor);
			search.setNumberOfSimulations(nSimulations);

			duration = Stopwatch.of(search::search).duration;
		}

		float ms = duration / nSimulations;
		System.out.println("UCT: " + ms + "ms/simulation");
	}

	@Test
	public void testUctSearch() {
		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);
		gameSet.play(Coordinate.c(3, 3));

		UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet);
		UctSearch<Coordinate> search = new UctSearch<>(visitor);
		search.setNumberOfSimulations(1000);
		Coordinate bestMove = search.search();

		search.dumpSearch();
		System.out.println("BEST MOVE = " + bestMove);
	}

	@Test
	public void testUctFirstMove() {
		int seed = 760903274;
		System.out.println("RANDOM SEED = " + seed);
		ShuffleUtil.setSeed(seed);

		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);

		UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet);
		UctSearch<Coordinate> search = new UctSearch<>(visitor);
		search.setNumberOfThreads(1);
		search.setNumberOfSimulations(80000);

		Coordinate move = search.search();
		gameSet.play(move);

		System.out.println(move);
		assertTrue(2 <= move.getX());
		assertTrue(2 <= move.getY());
		assertTrue(move.getX() < Weiqi.size - 2);
		assertTrue(move.getY() < Weiqi.size - 2);
	}

	@Test
	public void testUctGame() {
		new Profiler().profile(() -> {
			DecimalFormat df = new DecimalFormat("0.000");
			int nSimulations = 5000; // 20000
			int boundedTime = 300000;
			int seed = new Random().nextInt();

			System.out.println("RANDOM SEED = " + seed);
			ShuffleUtil.setSeed(seed);

			Board board = new Board();
			GameSet gameSet = new GameSet(board, Occupation.BLACK);

			while (true) {
				GameSet gameSet1 = new GameSet(gameSet);
				UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet1);
				UctSearch<Coordinate> search = new UctSearch<>(visitor);
				search.setNumberOfThreads(Constants.nThreads);
				search.setNumberOfSimulations(nSimulations);
				search.setBoundedTime(boundedTime);

				Stopwatch<Coordinate> timed = Stopwatch.of(search::search);
				Coordinate move = timed.result;

				if (move == null)
					break;

				Occupation player = gameSet.getNextPlayer();

				search.dumpPrincipalVariation();
				System.out.println(player //
						+ " " + move //
						+ " " + df.format(search.getWinningChance()) //
						+ " " + timed.duration + "ms");

				gameSet.play(move);
				UserInterface.display(gameSet);
			}
		});
	}

}
