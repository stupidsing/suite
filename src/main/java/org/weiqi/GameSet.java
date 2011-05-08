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

	public void move(Coordinate c) {
		if (!moveIfPossible(c))
			throw new RuntimeException("Invalid move " + c + " for "
					+ nextPlayer + "\n" + this);
	}

	public boolean isMovePossible(Coordinate c) {
		return moveIfPossible(c, true);
	}

	public boolean moveIfPossible(Coordinate c) {
		return moveIfPossible(c, false);
	}

	public boolean moveIfPossible(Coordinate c, boolean rollBack) {
		Occupation opponent = nextPlayer.opponent();
		Occupation neighbourColors[] = new Occupation[4];
		int i = 0;

		for (Coordinate c1 : c.neighbours())
			neighbourColors[i++] = get(c1);

		boolean success = super.moveIfPossible(c, nextPlayer);

		if (success) {
			int newHashCode = super.hashCode();
			success &= !previousStates.contains(newHashCode);

			if (!success || rollBack) {

				// Roll back board status; rejuvenate the pieces being eaten
				i = 0;
				for (Coordinate c1 : c.neighbours())
					if (neighbourColors[i++] != get(c1))
						for (Coordinate c2 : findGroup(c1))
							set(c2, opponent);

				set(c, Occupation.EMPTY);
			} else {
				nextPlayer = opponent;
				previousStates.add(newHashCode);
			}
		}

		return success;
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
