package org.weiqi.uct;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import org.weiqi.Board;
import org.weiqi.Coordinate;
import org.weiqi.GameSet;
import org.weiqi.RandomableList;
import org.weiqi.UctWeiqi;
import org.weiqi.UserInterface;
import org.weiqi.Weiqi;
import org.weiqi.Weiqi.Occupation;

/**
 * How I did profiling:
 * 
 * java -agentlib:hprof=cpu=times,depth=16,interval=1,thread=y
 */
public class UctMain<Move> {

	public static void main(String args[]) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		DecimalFormat df = new DecimalFormat("0.000");
		int nThreads = 2;
		int nSimulations = 20000;
		int boundedTime = 30000;
		Weiqi.adjustSize(7);

		Board board = new Board();
		GameSet gameSet = new GameSet(board, Occupation.BLACK);

		while (true) {
			GameSet gameSet1 = new GameSet(gameSet);
			UctWeiqi.Visitor visitor = UctWeiqi.createVisitor(gameSet1);
			UctSearch<Coordinate> search = new UctSearch<>(visitor);
			search.setNumberOfThreads(nThreads);
			search.setNumberOfSimulations(nSimulations);
			search.setBoundedTime(boundedTime);

			System.out.println("THINKING...");
			long start = System.currentTimeMillis();
			Coordinate move = search.search();
			long end = System.currentTimeMillis();
			if (move == null)
				break;

			Occupation player = gameSet.getNextPlayer();

			System.out.println(player //
					+ " " + move //
					+ " " + df.format(search.getWinningChance()) //
					+ " " + (end - start) + "ms");

			gameSet.play(move);
			UserInterface.display(gameSet);

			while (gameSet.getNextPlayer() == Occupation.WHITE)
				try {
					String pos[] = br.readLine().split(",");
					Integer x = Integer.valueOf(pos[0]);
					Integer y = Integer.valueOf(pos[1]);
					gameSet.play(Coordinate.c(x, y));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
		}
	}

	protected static void deepThink() {
		int seed = 760903274;
		System.out.println("RANDOM SEED = " + seed);
		RandomableList.setSeed(seed);

		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);

		UctWeiqi.Visitor visitor = UctWeiqi.createVisitor(gameSet);
		UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
		search.setNumberOfThreads(1);
		search.setNumberOfSimulations(80000);

		Coordinate move = search.search();
		gameSet.play(move);

		// search.dumpSearch();
		System.out.println(move);
	}

}
