package org.weiqi.uct;

import org.weiqi.Board;
import org.weiqi.Coordinate;
import org.weiqi.GameSet;
import org.weiqi.RandomList;
import org.weiqi.UctWeiqi.Visitor;
import org.weiqi.Weiqi.Occupation;

/**
 * How I did profiling:
 * 
 * java -agentlib:hprof=cpu=times,depth=16,interval=1,thread=y
 */
public class UctMain<Move> {

	public static void main(String args[]) {
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
