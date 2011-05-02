package org.weiqi;

import org.junit.Test;
import org.weiqi.UctWeiqi.Visitor;
import org.weiqi.Weiqi.Occupation;

public class UctTest {

	@Test
	public void testRandom() {
		Board board = new Board();
		new Visitor(board, Occupation.BLACK).evaluateRandomOutcome();
		UserInterface.display(board);
	}

	@Test
	public void testUctSearch() {
		Board board = new Board();
		Visitor visitor = new Visitor(board, Occupation.BLACK);
		new UctSearch<Coordinate>(visitor).search();
	}

}
