package suite.weiqi;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;

import suite.util.Object_;
import suite.weiqi.Weiqi.Occupation;

public class GameSet1 {

	public final Board1 board;

	private Occupation nextPlayer;
	private HashSet<Integer> previousStates = new HashSet<>();
	private Deque<Runnable> undos = new ArrayDeque<>();

	public GameSet1() {
		this(new Board1(), Occupation.BLACK);
	}

	@SuppressWarnings("unchecked")
	public GameSet1(GameSet1 gameSet) {
		this(gameSet.board, gameSet.nextPlayer, (HashSet<Integer>) gameSet.previousStates.clone());
	}

	/**
	 * Constructs a "left-over" game. Note that the previous state information
	 * will be empty, not suitable for real-play scenario.
	 */
	public GameSet1(Board1 board, Occupation nextPlayer) {
		this(board, nextPlayer, new HashSet<>());
		previousStates.add(board.hashCode());
	}

	private GameSet1(Board1 board, Occupation nextPlayer, HashSet<Integer> previousStates) {
		this.board = new Board1(board);
		this.nextPlayer = nextPlayer;
		this.previousStates = previousStates;
	}

	public boolean play(Coordinate c) {
		undos.push(board.move(c, nextPlayer));
		nextPlayer = nextPlayer.opponent();

		boolean isSuccess = previousStates.contains(board.hashCode());
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
		if (Object_.clazz(object) == GameSet1.class) {
			GameSet1 other = (GameSet1) object;
			return board.equals(other.board) && nextPlayer == other.nextPlayer && previousStates.equals(other.previousStates);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return board.hashCode() ^ nextPlayer.hashCode() ^ previousStates.hashCode();
	}

}
