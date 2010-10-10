package org.weiqi;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.weiqi.Weiqi.Occupation;

public class BoardTest {

	@Test
	public void testEat() {
		Board board = new Board();
		board.move(new Coordinate(0, 1), Occupation.BLACK);
		board.move(new Coordinate(0, 0), Occupation.WHITE);
		board.move(new Coordinate(1, 0), Occupation.BLACK);
		assertEquals(board.get(new Coordinate(0, 0)), Occupation.EMPTY);
	}

}
