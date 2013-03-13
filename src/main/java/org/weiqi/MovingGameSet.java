package org.weiqi;

import java.util.ArrayDeque;
import java.util.Deque;

import org.weiqi.Weiqi.Occupation;

public class MovingGameSet extends GameSet {

	private final Deque<Move> moves = new ArrayDeque<>();

	public MovingGameSet() {
		super();
	}

	public MovingGameSet(GameSet gameSet) {
		super(gameSet);
	}

	public MovingGameSet(Board board, Occupation nextPlayer) {
		super(board, nextPlayer);
	}

	@Override
	public Move play(Coordinate c) {
		Move move = super.play(c);
		moves.push(move);
		return move;
	}

	public void undo() {
		unplay(moves.pop());
	}

}
