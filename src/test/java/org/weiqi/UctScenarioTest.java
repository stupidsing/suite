package org.weiqi;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.weiqi.Weiqi.Occupation;
import org.weiqi.uct.UctSearch;

public class UctScenarioTest {

	@Before
	public void before() {
		Weiqi.adjustSize(7);
	}

	@Test
	public void testCapture() {
		GameSet gameSet = new GameSet(UserInterface.importBoard("" //
				+ ". . . . . . . \n" //
				+ ". . . X O . . \n" //
				+ ". . X . O . . \n" //
				+ ". . X O . . . \n" //
				+ ". . O X O . . \n" //
				+ ". . . X O . . \n" //
				+ ". . . . . . . \n" //
		), Occupation.WHITE);
		testScenario(gameSet, Coordinate.c(5, 2));
	}

	@Test
	public void testLiveAndDeath() {
		GameSet gameSet = new GameSet(UserInterface.importBoard("" //
				+ "X X X X X X X \n" //
				+ "X . . X X . X \n" //
				+ "O X X O X . X \n" //
				+ "O . . O O X X \n" //
				+ "O O O . O O O \n" //
				+ "O O . . . O O \n" //
				+ "O O O . . O O \n" //
		), Occupation.BLACK);
		testScenario(gameSet, Coordinate.c(5, 3));
	}

	@Test
	public void testLiveAndDeath1() {
		GameSet gameSet = new GameSet(UserInterface.importBoard("" //
				+ ". O . O X . . \n" //
				+ "O . . O X . . \n" //
				+ ". . . O X X X \n" //
				+ ". O O O X . . \n" //
				+ "O O X X X O . \n" //
				+ "X X X . O . O \n" //
				+ ". . X . . . . \n" //
		), Occupation.BLACK);
		testScenario(gameSet, Coordinate.c(6, 5));
	}

	private void testScenario(GameSet gameSet, Coordinate bestMove) {
		UctWeiqi.Visitor visitor = UctWeiqi.createVisitor(new GameSet(gameSet));
		UctSearch<Coordinate> search = new UctSearch<Coordinate>(visitor);
		search.setNumberOfThreads(2);
		search.setNumberOfSimulations(20000);

		Coordinate move = search.search();
		search.dumpPrincipalVariation();
		search.dumpSearch();
		search.dumpRave();

		assertEquals(bestMove, move);
	}

}
