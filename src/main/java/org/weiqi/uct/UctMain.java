package org.weiqi.uct;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

import org.weiqi.Board;
import org.weiqi.Coordinate;
import org.weiqi.GameSet;
import org.weiqi.RandomList;
import org.weiqi.UctWeiqi.Visitor;
import org.weiqi.UserInterface;
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
		int boundedTime = 300000;

		Board board = new Board();
		GameSet gameSet = new GameSet(board, Occupation.BLACK);

		while (true) {
			Visitor visitor = new Visitor(new GameSet(gameSet));
			UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
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
					+ " " + (start - end) + "ms");

			gameSet.move(move);
			UserInterface.display(gameSet);

			while (gameSet.getNextPlayer() == Occupation.WHITE)
				try {
					String pos[] = br.readLine().split(",");
					Integer x = Integer.valueOf(pos[0]);
					Integer y = Integer.valueOf(pos[1]);
					gameSet.move(Coordinate.c(x, y));
				} catch (Exception ex) {
					ex.printStackTrace();
				}
		}
	}

	protected static void deepThink() {
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
	}

}
