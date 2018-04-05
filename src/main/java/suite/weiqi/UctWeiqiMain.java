package suite.weiqi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import suite.Constants;
import suite.os.LogUtil;
import suite.os.Stopwatch;
import suite.uct.ShuffleUtil;
import suite.uct.UctSearch;
import suite.uct.UctVisitor;
import suite.uct.UctWeiqi;
import suite.util.String_;
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
		InputStreamReader isr = new InputStreamReader(System.in, Constants.charset);
		var br = new BufferedReader(isr);
		var df = new DecimalFormat("0.000");
		var nThreads = Constants.nThreads;
		var nSimulations = 10000 * nThreads;
		var boundedTime = 30000;
		Weiqi.adjustSize(7);

		var board = new Board();
		MovingGameSet gameSet = new MovingGameSet(board, startingPlayer);
		var auto = false;
		var quit = false;
		var status = "LET'S PLAY!";

		while (!quit) {
			var gameSet1 = new GameSet(gameSet);
			UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet1);
			UctSearch<Coordinate> search = new UctSearch<>(visitor);
			search.setNumberOfThreads(nThreads);
			search.setNumberOfSimulations(nSimulations);
			search.setBoundedTime(boundedTime);

			if (auto || gameSet.getNextPlayer() == computerPlayer) {
				System.out.println("THINKING...");

				Stopwatch<Coordinate> sw = Stopwatch.of(search::search);
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
							if (!String_.isBlank(line)) {
								var pos = String_.split2(line, ",");
								var x = Integer.parseInt(pos.t0);
								var y = Integer.parseInt(pos.t1);
								gameSet.play(Coordinate.c(x, y));
							}
						}
					else
						quit = true;
				} catch (Exception ex) {
					LogUtil.error(ex);
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
		while (!String_.isBlank(s));

		var board = UserInterface.importBoard(sb.toString());
		return new MovingGameSet(board, startingPlayer);
	}

	protected static void deepThink() {
		var seed = 760903274;
		System.out.println("RANDOM SEED = " + seed);
		ShuffleUtil.setSeed(seed);

		GameSet gameSet = new GameSet(new Board(), startingPlayer);

		UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet);
		UctSearch<Coordinate> search = new UctSearch<>(visitor);
		search.setNumberOfThreads(1);
		search.setNumberOfSimulations(80000);

		var move = search.search();
		gameSet.play(move);

		// search.dumpSearch();
		System.out.println(move);
	}

}
