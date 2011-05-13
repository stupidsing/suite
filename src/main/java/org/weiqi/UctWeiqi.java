package org.weiqi;

import java.util.Iterator;
import java.util.List;

import org.weiqi.Board.MoveType;
import org.weiqi.GameSet.MoveCommand;
import org.weiqi.Weiqi.Occupation;
import org.weiqi.uct.UctVisitor;

public class UctWeiqi {

	public static class Visitor implements UctVisitor<Coordinate> {
		private GameSet gameSet;

		public Visitor(GameSet gameSet) {
			this.gameSet = gameSet;
		}

		@Override
		public UctVisitor<Coordinate> cloneVisitor() {
			return new Visitor(new GameSet(gameSet));
		}

		@Override
		public List<Coordinate> elaborateMoves() {
			MoveCommand move = new MoveCommand();
			List<Coordinate> captureMoves = new RandomList<Coordinate>();
			List<Coordinate> otherMoves = new RandomList<Coordinate>();

			for (Coordinate c : Coordinate.all())
				if (gameSet.get(c) == Occupation.EMPTY) {
					move.position = c;

					if (gameSet.isMovePossible(move))
						if (move.type == MoveType.CAPTURE)
							captureMoves.add(c);
						else
							otherMoves.add(c);
				}

			// Make capture moves at the tail;
			// UctSearch would put them in first few nodes
			otherMoves.addAll(captureMoves);
			return otherMoves;
		}

		@Override
		public void playMove(Coordinate c) {
			gameSet.move(new MoveCommand(c));
		}

		@Override
		public boolean evaluateRandomOutcome() {
			Occupation me = gameSet.getNextPlayer();
			RandomList<Coordinate> empties = findAllEmptyPositions();
			Coordinate pos;
			MoveCommand move = null;

			// Move until someone cannot move anymore,
			// or maximum iterations reached
			for (int i = 0; i < 4 * Weiqi.AREA; i++) {
				move = null;

				// Try a random empty position, if that position does not work,
				// calls the heavier possible move method
				if ((pos = empties.last()) != null)
					if (gameSet.moveIfPossible(move = new MoveCommand(pos)))
						empties.remove();
					else
						move = null;

				if (move == null)
					move = removePossibleMove(empties.iterator());

				if (move != null) { // Add empty positions back to empty group
					int j = 0;

					for (Coordinate c1 : move.position.neighbours())
						if (move.neighbourColors[j++] != gameSet.get(c1))
							for (Coordinate c2 : gameSet.findGroup(c1))
								empties.add(c2);
				} else
					break; // No moves can be played, current player lost
			}

			if (move == null)
				return gameSet.getNextPlayer() != me;
			else
				return Evaluator.evaluate(me, gameSet) > 0;
		}

		private MoveCommand removePossibleMove(Iterator<Coordinate> iter) {
			while (iter.hasNext()) {
				MoveCommand move = new MoveCommand(iter.next());

				if (gameSet.moveIfPossible(move)) {
					iter.remove();
					return move;
				}
			}

			return null;
		}

		public RandomList<Coordinate> findAllEmptyPositions() {
			RandomList<Coordinate> moves = new RandomList<Coordinate>();

			for (Coordinate c : Coordinate.all())
				if (gameSet.get(c) == Occupation.EMPTY)
					moves.add(c);

			return moves;
		}
	}

}
