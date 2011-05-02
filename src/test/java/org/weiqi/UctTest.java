package org.weiqi;

import org.junit.Test;
import org.weiqi.UctWeiqi.Visitor;

public class UctTest {

	@Test
	public void test() {
		Board board = new Board();
		new Visitor(board).evaluateRandomOutcome();
		UserInterface.display(board);
	}

}
