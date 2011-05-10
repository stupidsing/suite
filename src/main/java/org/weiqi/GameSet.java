package org.weiqi;

import java.util.HashSet;

import org.weiqi.Weiqi.Occupation;

public class GameSet extends Board {

	private Occupation nextPlayer;
	private HashSet<Integer> previousStates = new HashSet<Integer>();

	public GameSet() {
		super();
		nextPlayer = Occupation.BLACK;
		previousStates.add(hashCode());
	}

	/**
	 * Constructs a "left-over" game. Note that the previous state information
	 * will be empty, not suitable for real-play scenario.
	 */
	public GameSet(Board board, Occupation nextPlayer) {
		super(board);
		this.nextPlayer = nextPlayer;
		previousStates.add(hashCode());
	}

	public GameSet(GameSet gameSet) {
		super(gameSet);

		@SuppressWarnings("unchecked")
		HashSet<Integer> p = (HashSet<Integer>) gameSet.previousStates.clone();

		nextPlayer = gameSet.nextPlayer;
		previousStates = p;
	}

	public static class MoveCommand {
		public Coordinate position;
		public Occupation neighbourColors[] = new Occupation[4];

		public MoveCommand() {
		}

		public MoveCommand(Coordinate position) {
			this.position = position;
		}
	}

	public void move(Coordinate c) {
		move(new MoveCommand(c));
	}

	public void move(MoveCommand move) {
		if (!moveIfPossible(move))
			throw new RuntimeException("Invalid move " + move.position
					+ " for " + nextPlayer + "\n" + this);
	}

	public boolean isMovePossible(MoveCommand move) {
		return moveIfPossible(move, true);
	}

	public boolean moveIfPossible(MoveCommand move) {
		return moveIfPossible(move, false);
	}

	private boolean moveIfPossible(MoveCommand move, boolean rollBack) {
		Occupation opponent = nextPlayer.opponent();
		int i = 0;

		for (Coordinate c1 : move.position.neighbours())
			move.neighbourColors[i++] = get(c1);

		boolean success = super.moveIfPossible(move.position, nextPlayer);

		if (success) {
			int newHashCode = super.hashCode();
			success &= !previousStates.contains(newHashCode);

			if (success && !rollBack) {
				nextPlayer = opponent;
				previousStates.add(newHashCode);
			} else
				rollBackMove(move, opponent);
		}

		return success;
	}

	/**
	 * Roll back board status; rejuvenate the pieces being eaten.
	 */
	private void rollBackMove(MoveCommand move, Occupation opponent) {
		int i = 0;
		for (Coordinate c1 : move.position.neighbours())
			if (move.neighbourColors[i++] != get(c1))
				for (Coordinate c2 : findGroup(c1))
					set(c2, opponent);

		set(move.position, Occupation.EMPTY);
	}

	@Override
	public int hashCode() {
		return super.hashCode() //
				^ nextPlayer.hashCode() //
				^ previousStates.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof GameSet) {
			GameSet other = (GameSet) object;
			return super.equals(other) //
					&& nextPlayer == other.nextPlayer //
					&& previousStates.equals(other.previousStates);

		} else
			return false;
	}

	public Occupation getNextPlayer() {
		return nextPlayer;
	}

}
