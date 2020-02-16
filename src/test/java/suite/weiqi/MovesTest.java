package suite.weiqi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import suite.uct.UctWeiqi;
import suite.weiqi.Weiqi.Occupation;

public class MovesTest {

	@BeforeEach
	public void before() {
		Weiqi.initialize();
	}

	@Test
	public void testNoMoveToFind() {
		var board = blackBoard();
		board.set(Coordinate.c(2, 2), Occupation.EMPTY);

		var gameSet = new GameSet(board, Occupation.BLACK);
		var visitor = UctWeiqi.newVisitor(gameSet);
		assertTrue(visitor.elaborateMoves().isEmpty());
	}

	@Test
	public void testFindMove() {
		var board = blackBoard();
		board.set(Coordinate.c(3, 3), Occupation.EMPTY);
		board.set(Coordinate.c(3, 5), Occupation.EMPTY);
		board.set(Coordinate.c(5, 3), Occupation.EMPTY);
		board.set(Coordinate.c(5, 5), Occupation.EMPTY);
		board.set(Coordinate.c(5, 6), Occupation.WHITE);
		board.set(Coordinate.c(8, 8), Occupation.EMPTY);
		board.set(Coordinate.c(9, 8), Occupation.EMPTY);

		var gameSet = new GameSet(board, Occupation.WHITE);
		var visitor = UctWeiqi.newVisitor(gameSet);
		assertEquals(2, visitor.elaborateMoves().size());
	}

	private Board blackBoard() {
		var board = new Board();
		for (var c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

	@Test
	public void testRandom() {
		var gameSet = new GameSet(new Board(), Occupation.BLACK);
		var visitor = UctWeiqi.newVisitor(gameSet);
		var isWon = visitor.evaluateRandomOutcome();
		UserInterface.display(gameSet);
		System.out.println(isWon ? "WON" : "LOSS");
	}

}
