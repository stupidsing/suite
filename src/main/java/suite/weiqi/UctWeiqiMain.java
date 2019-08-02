package suite.weiqi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import primal.Nouns.Utf8;
import primal.Verbs.Is;
import primal.Verbs.Split;
import primal.os.Log_;
import suite.cfg.Defaults;
import suite.os.Stopwatch;
import suite.uct.ShuffleUtil;
import suite.uct.UctSearch;
import suite.uct.UctWeiqi;
import suite.weiqi.Weiqi.Occupation;

/**
 * How I did profiling:
 *
 * java -agentlib:hprof=cpu=times,depth=16,interval=1,thread=y
 */
public class UctWeiqiMain<Move> {

	private static Occupation computerPlayer = Occupation.BLACK;
	private static Occupation humanPlayer = Occupation.WHITE;

	private static Occupation startingPlayer = Occupation.BLACK;

	public static void main(String[] args) {
		var isr = new InputStreamReader(System.in, Utf8.charset);
		var br = new BufferedReader(isr);
		var df = new DecimalFormat("0.000");
		var nThreads = Defaults.nThreads;
		var nSimulations = 10000 * nThreads;
		var boundedTime = 30000;
		Weiqi.adjustSize(7);

		var board = new Board();
		var gameSet = new MovingGameSet(board, startingPlayer);
		var auto = false;
		var quit = false;
		var status = "LET'S PLAY!";

		while (!quit) {
			var gameSet1 = new GameSet(gameSet);
			var visitor = UctWeiqi.newVisitor(gameSet1);
			var search = new UctSearch<>(visitor);
			search.setNumberOfThreads(nThreads);
			search.setNumberOfSimulations(nSimulations);
			search.setBoundedTime(boundedTime);

			if (auto || gameSet.getNextPlayer() == computerPlayer) {
				System.out.println("THINKING...");

				var sw = Stopwatch.of(search::search);
				var coord = sw.result;

				if (coord != null) {
					status = gameSet.getNextPlayer() //
							+ " " + coord //
							+ " " + df.format(search.getWinningChance()) //
							+ " " + sw.duration + "ms";

					gameSet.play(coord);

					if (auto)
						display(gameSet, status);
				} else {
					System.out.println("I LOSE");
					quit = true;
				}
			}

			while (!auto && !quit && gameSet.getNextPlayer() == humanPlayer)
				try {
					display(gameSet, status);

					var line = br.readLine();

					if (line != null)
						switch (line) {
						case "auto":
							auto = true;
							break;
						case "load":
							gameSet = loadGameSet(br);
							break;
						case "undo":
							gameSet.undo();
							gameSet.undo();
							status = "AFTER UNDO:";
							break;
						default:
							if (!Is.blank(line))
								gameSet.play(Split //
										.strl(line, ",") //
										.map((xs, ys) -> Coordinate.c(Integer.parseInt(xs), Integer.parseInt(ys))));
						}
					else
						quit = true;
				} catch (Exception ex) {
					Log_.error(ex);
				}
		}
	}

	private static void display(MovingGameSet gameSet, String status) {
		System.out.println(status);
		UserInterface.display(gameSet);
	}

	private static MovingGameSet loadGameSet(BufferedReader br) throws IOException {
		System.out.println("PLEASE ENTER BOARD DATA AND AN BLANK LINE:\n");
		String s;
		var sb = new StringBuilder();

		do
			sb.append((s = br.readLine()) + "\n");
		while (!Is.blank(s));

		var board = UserInterface.importBoard(sb.toString());
		return new MovingGameSet(board, startingPlayer);
	}

	protected static void deepThink() {
		var seed = 760903274;
		System.out.println("RANDOM SEED = " + seed);
		ShuffleUtil.setSeed(seed);

		var gameSet = new GameSet(new Board(), startingPlayer);

		var visitor = UctWeiqi.newVisitor(gameSet);
		var search = new UctSearch<>(visitor);
		search.setNumberOfThreads(1);
		search.setNumberOfSimulations(80000);

		var move = search.search();
		gameSet.play(move);

		// search.dumpSearch();
		System.out.println(move);
	}

}
