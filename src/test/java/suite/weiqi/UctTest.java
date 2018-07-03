package suite.weiqi;

import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import suite.cfg.Defaults;
import suite.object.Object_;
import suite.os.Stopwatch;
import suite.sample.Profiler;
import suite.uct.ShuffleUtil;
import suite.uct.UctSearch;
import suite.uct.UctWeiqi;
import suite.weiqi.Weiqi.Occupation;

public class UctTest {

	@Before
	public void before() {
		Weiqi.adjustSize(7);
	}

	@Test
	public void testRandomEvaluation() {
		var seed = 214636368;
		System.out.println("RANDOM SEED = " + seed);
		ShuffleUtil.setSeed(seed);
		var mid = Weiqi.size / 2;

		var corner = evaluateRandomOutcome(Coordinate.c(0, 0));
		var faraway = evaluateRandomOutcome(Coordinate.c(1, 1));
		var center = evaluateRandomOutcome(Coordinate.c(mid, mid));
		System.out.println("CORNER: " + corner //
				+ ", FAR-AWAY: " + faraway //
				+ ", CENTER: " + center);

		// the corner move must be the worst
		assertTrue(Object_.compare(corner, faraway) < 0);
		assertTrue(Object_.compare(faraway, center) < 0);
	}

	private String evaluateRandomOutcome(Coordinate move) {
		var player = Occupation.WHITE;
		int nWins = 0, nTotal = 1000;

		for (var i = 0; i < nTotal; i++) {
			var gameSet = new GameSet(new Board(), player);
			var visitor = UctWeiqi.newVisitor(gameSet);
			visitor.playMove(move);
			nWins += visitor.evaluateRandomOutcome() ? 0 : 1;
		}

		return nWins + "/" + nTotal;
	}

	@Test
	public void testRandomEvaluationTime() {
		var gameSet = new GameSet(new Board(), Occupation.BLACK);
		var i = 0;
		int[] ss = { 1000, 10000, };
		var duration = 0f;

		for (var time = 0; time < 2; time++)
			for (; i < ss[time]; i++)
				duration = Stopwatch.of(() -> {
					var gameSet1 = new GameSet(gameSet);
					var visitor = UctWeiqi.newVisitor(gameSet1);
					visitor.evaluateRandomOutcome();
					return null;
				}).duration;

		var ms = duration / (ss[1] - ss[0]);
		System.out.println("Random move: " + ms + "ms per evaluation");
	}

	@Test
	public void testUctSearchTime() {
		var nSimulations = 1000;
		var duration = 0f;

		for (var time = 0; time < 2; time++) {
			var gameSet = new GameSet(new Board(), Occupation.BLACK);
			var visitor = UctWeiqi.newVisitor(gameSet);
			var search = new UctSearch<>(visitor);
			search.setNumberOfSimulations(nSimulations);

			duration = Stopwatch.of(search::search).duration;
		}

		var ms = duration / nSimulations;
		System.out.println("UCT: " + ms + "ms/simulation");
	}

	@Test
	public void testUctSearch() {
		var gameSet = new GameSet(new Board(), Occupation.BLACK);
		gameSet.play(Coordinate.c(3, 3));

		var visitor = UctWeiqi.newVisitor(gameSet);
		var search = new UctSearch<>(visitor);
		search.setNumberOfSimulations(1000);
		var bestMove = search.search();

		search.dumpSearch();
		System.out.println("BEST MOVE = " + bestMove);
	}

	@Test
	public void testUctFirstMove() {
		var seed = 760903274;
		System.out.println("RANDOM SEED = " + seed);
		ShuffleUtil.setSeed(seed);

		var gameSet = new GameSet(new Board(), Occupation.BLACK);

		var visitor = UctWeiqi.newVisitor(gameSet);
		var search = new UctSearch<>(visitor);
		search.setNumberOfThreads(1);
		search.setNumberOfSimulations(80000);

		var move = search.search();
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
			var df = new DecimalFormat("0.000");
			var nSimulations = 5000; // 20000
			var boundedTime = 300000;
			var seed = new Random().nextInt();

			System.out.println("RANDOM SEED = " + seed);
			ShuffleUtil.setSeed(seed);

			var board = new Board();
			var gameSet = new GameSet(board, Occupation.BLACK);

			while (true) {
				var gameSet1 = new GameSet(gameSet);
				var visitor = UctWeiqi.newVisitor(gameSet1);
				var search = new UctSearch<>(visitor);
				search.setNumberOfThreads(Defaults.nThreads);
				search.setNumberOfSimulations(nSimulations);
				search.setBoundedTime(boundedTime);

				var timed = Stopwatch.of(search::search);
				var move = timed.result;

				if (move == null)
					break;

				var player = gameSet.getNextPlayer();

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
