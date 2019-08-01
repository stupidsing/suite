package suite.weiqi;

import java.util.ArrayDeque;
import java.util.Deque;

import primal.Verbs.Get;
import suite.primitive.adt.set.IntSet;
import suite.weiqi.Weiqi.Occupation;

public class GameSet1 {

	public final Board1 board;

	private Occupation nextPlayer;
	private IntSet previousStates = new IntSet();
	private Deque<Runnable> undos = new ArrayDeque<>();

	public GameSet1() {
		this(new Board1(), Occupation.BLACK);
	}

	public GameSet1(GameSet1 gameSet) {
		this(gameSet.board, gameSet.nextPlayer, gameSet.previousStates.clone());
	}

	/**
	 * Constructs a "left-over" game. Note that the previous state information
	 * will be empty, not suitable for real-play scenario.
	 */
	public GameSet1(Board1 board, Occupation nextPlayer) {
		this(board, nextPlayer, new IntSet());
		previousStates.add(board.hashCode());
	}

	private GameSet1(Board1 board, Occupation nextPlayer, IntSet previousStates) {
		this.board = new Board1(board);
		this.nextPlayer = nextPlayer;
		this.previousStates = previousStates;
	}

	public boolean play(Coordinate c) {
		undos.push(board.move(c, nextPlayer));
		nextPlayer = nextPlayer.opponent();

		var isSuccess = previousStates.contains(board.hashCode());
		if (!isSuccess)
			undo();
		return isSuccess;
	}

	public void undo() {
		previousStates.remove(board.hashCode());
		undos.pop().run();
		nextPlayer = nextPlayer.opponent();
	}

	public Occupation getNextPlayer() {
		return nextPlayer;
	}

	@Override
	public boolean equals(Object object) {
		if (Get.clazz(object) == GameSet1.class) {
			var other = (GameSet1) object;
			return board.equals(other.board) && nextPlayer == other.nextPlayer && previousStates.equals(other.previousStates);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return board.hashCode() ^ nextPlayer.hashCode() ^ previousStates.hashCode();
	}

}
