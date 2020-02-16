package suite.weiqi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import suite.weiqi.Weiqi.Occupation;

public class EvaluatorTest {

	@BeforeEach
	public void before() {
		Weiqi.initialize();
	}

	@Test
	public void testOneWhite() {
		var board = new Board();
		board.set(Coordinate.c(9, 9), Occupation.WHITE);
		assertEquals(36110, Evaluator.evaluate(Occupation.WHITE, board));
		assertEquals(-36110, Evaluator.evaluate(Occupation.BLACK, board));
	}

	@Test
	public void testEqualPower() {
		var board = new Board();
		board.set(Coordinate.c(3, 3), Occupation.WHITE);
		board.set(Coordinate.c(15, 15), Occupation.BLACK);
		assertEquals(0, Evaluator.evaluate(Occupation.WHITE, board));
		assertEquals(0, Evaluator.evaluate(Occupation.BLACK, board));
	}

	@Test
	public void testTerritory() {
		var board = new Board();
		board.set(Coordinate.c(0, 3), Occupation.WHITE);
		board.set(Coordinate.c(1, 3), Occupation.WHITE);
		board.set(Coordinate.c(2, 3), Occupation.WHITE);
		board.set(Coordinate.c(3, 2), Occupation.WHITE);
		board.set(Coordinate.c(3, 1), Occupation.WHITE);
		board.set(Coordinate.c(3, 0), Occupation.WHITE);
		board.set(Coordinate.c(15, 15), Occupation.BLACK);
		assertEquals(1450, Evaluator.evaluate(Occupation.WHITE, board));
		assertEquals(-1450, Evaluator.evaluate(Occupation.BLACK, board));
	}

}
