package suite.weiqi;

import suite.object.Object_;
import suite.primitive.adt.set.IntSet;
import suite.util.Fail;
import suite.weiqi.Board.MoveType;
import suite.weiqi.Weiqi.Occupation;

public class GameSet {

	public final Board board;

	private Occupation nextPlayer;
	private IntSet previousStates = new IntSet();

	public GameSet() {
		this(new Board(), Occupation.BLACK);
	}

	public GameSet(GameSet gameSet) {
		this(gameSet.board, gameSet.nextPlayer, gameSet.previousStates.clone());
	}

	/**
	 * Constructs a "left-over" game. Note that the previous state information
	 * will be empty, not suitable for real-play scenario.
	 */
	public GameSet(Board board, Occupation nextPlayer) {
		this(board, nextPlayer, new IntSet());
		previousStates.add(board.hashCode());
	}

	private GameSet(Board board, Occupation nextPlayer, IntSet previousStates) {
		this.board = new Board(board);
		this.nextPlayer = nextPlayer;
		this.previousStates = previousStates;
	}

	/**
	 * Move that can be played or un-played.
	 */
	public static class Move {
		public Coordinate position;
		public MoveType type;
		public Occupation[] neighborColors = new Occupation[4];

		public Move() {
		}

		public Move(Coordinate position) {
			this.position = position;
		}
	}

	public Move play(Coordinate c) {
		var move = new Move(c);
		play(move);
		return move;
	}

	private void play(Move move) {
		if (!playIfValid(move))
			Fail.t("invalid move " + move.position + " for " + nextPlayer + "\n" + this);
	}

	public void undo(Move move) {
		nextPlayer = nextPlayer.opponent();
		unplay(move);
	}

	public boolean isValidMove(Move move) {
		return playIfValid(move, true);
	}

	public boolean playIfValid(Move move) {
		return playIfValid(move, false);
	}

	/**
	 * Plays a move on the Weiqi board. Ensure no repeats in game state history.
	 */
	private boolean playIfValid(Move move, boolean rollBack) {
		var opponent = nextPlayer.opponent();
		var i = 0;

		for (var c1 : move.position.neighbors)
			move.neighborColors[i++] = board.get(c1);

		move.type = board.playIfSeemsPossible(move.position, nextPlayer);
		var success = move.type != MoveType.INVALID;

		if (success) {
			var newHashCode = board.hashCode();
			success &= !previousStates.contains(newHashCode);

			if (success && !rollBack) {
				nextPlayer = opponent;
				previousStates.add(newHashCode);
			} else
				unplay_(move);
		}

		return success;
	}

	public void pass() {
		nextPlayer = nextPlayer.opponent();
	}

	/**
	 * Roll back board status; rejuvenate the pieces being eaten.
	 */
	public void unplay(Move move) {
		previousStates.remove(board.hashCode());
		unplay_(move);
	}

	private void unplay_(Move move) {
		var opponent = nextPlayer.opponent();

		if (move.type == MoveType.CAPTURE) {
			var i = 0;

			for (var c1 : move.position.neighbors)
				if (move.neighborColors[i++] != board.get(c1))
					for (var c2 : board.findGroup(c1))
						board.set(c2, opponent);
		}

		board.set(move.position, Occupation.EMPTY);
	}

	public Occupation getNextPlayer() {
		return nextPlayer;
	}

	@Override
	public boolean equals(Object object) {
		if (Object_.clazz(object) == GameSet.class) {
			var other = (GameSet) object;
			return board.equals(other.board) && nextPlayer == other.nextPlayer && previousStates.equals(other.previousStates);
		} else
			return false;
	}

	@Override
	public int hashCode() {
		return board.hashCode() ^ nextPlayer.hashCode() ^ previousStates.hashCode();
	}

}
