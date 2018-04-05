package suite.weiqi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import suite.uct.UctVisitor;
import suite.uct.UctWeiqi;
import suite.weiqi.Weiqi.Occupation;

public class MovesTest {

	@Before
	public void before() {
		Weiqi.initialize();
	}

	@Test
	public void testNoMoveToFind() {
		Board board = blackBoard();
		board.set(Coordinate.c(2, 2), Occupation.EMPTY);

		GameSet gameSet = new GameSet(board, Occupation.BLACK);
		UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet);
		assertTrue(visitor.elaborateMoves().isEmpty());
	}

	@Test
	public void testFindMove() {
		Board board = blackBoard();
		board.set(Coordinate.c(3, 3), Occupation.EMPTY);
		board.set(Coordinate.c(3, 5), Occupation.EMPTY);
		board.set(Coordinate.c(5, 3), Occupation.EMPTY);
		board.set(Coordinate.c(5, 5), Occupation.EMPTY);
		board.set(Coordinate.c(5, 6), Occupation.WHITE);
		board.set(Coordinate.c(8, 8), Occupation.EMPTY);
		board.set(Coordinate.c(9, 8), Occupation.EMPTY);

		GameSet gameSet = new GameSet(board, Occupation.WHITE);
		UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet);
		assertEquals(2, visitor.elaborateMoves().size());
	}

	private Board blackBoard() {
		Board board = new Board();
		for (var c : Coordinate.all())
			board.set(c, Occupation.BLACK);
		return board;
	}

	@Test
	public void testRandom() {
		GameSet gameSet = new GameSet(new Board(), Occupation.BLACK);
		UctVisitor<Coordinate> visitor = UctWeiqi.newVisitor(gameSet);
		boolean isWon = visitor.evaluateRandomOutcome();
		UserInterface.display(gameSet);
		System.out.println(isWon ? "WON" : "LOSS");
	}

}
