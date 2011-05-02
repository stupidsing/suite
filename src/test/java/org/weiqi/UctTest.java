package org.weiqi;

import org.junit.Test;
import org.weiqi.UctWeiqi.Visitor;
import org.weiqi.Weiqi.Occupation;

public class UctTest {

	@Test
	public void test() {
		Board board = new Board();
		new Visitor(board, Occupation.BLACK).evaluateRandomOutcome();
		UserInterface.display(board);
	}

}
