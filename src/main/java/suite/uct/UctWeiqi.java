package suite.uct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import primal.Verbs.Last;
import suite.weiqi.Board;
import suite.weiqi.Board.MoveType;
import suite.weiqi.Coordinate;
import suite.weiqi.Evaluator;
import suite.weiqi.GameSet;
import suite.weiqi.GameSet.Move;
import suite.weiqi.Weiqi;
import suite.weiqi.Weiqi.Occupation;

public class UctWeiqi {

	public static class Visitor implements UctVisitor<Coordinate> {
		private GameSet gameSet;
		private Board board;

		private Visitor(GameSet gameSet) {
			this.gameSet = gameSet;
			board = gameSet.board;
		}

		@Override
		public Iterable<Coordinate> getAllMoves() {
			return Coordinate.all();
		}

		@Override
		public List<Coordinate> elaborateMoves() {
			var move = new Move();
			var captureMoves = new ArrayList<Coordinate>();
			var otherMoves = new ArrayList<Coordinate>();

			for (var c : Coordinate.all())
				if (board.get(c) == Occupation.EMPTY) {
					move.position = c;

					if (gameSet.isValidMove(move))
						if (move.type == MoveType.CAPTURE)
							ShuffleUtil.add(captureMoves, c);
						else
							ShuffleUtil.add(otherMoves, c);
				}

			// make capture moves at the head;
			// uctSearch would put them in first few nodes
			captureMoves.addAll(otherMoves);
			return captureMoves;
		}

		@Override
		public void playMove(Coordinate c) {
			gameSet.play(c);
		}

		/**
		 * The "play till both passes" Monte Carlo, with some customizations:
		 *
		 * - Consider capture moves first;
		 *
		 * - Would not fill a single-eye.
		 */
		@Override
		public boolean evaluateRandomOutcome() {
			var empties = findAllEmptyPositions();
			var capturedPositions = new HashSet<Coordinate>();
			var me = gameSet.getNextPlayer();
			Move move, chosenMove;
			var nPasses = 0;

			// move until someone cannot move anymore, or maximum number of
			// passes is reached between both players
			while (nPasses < 2) {
				var iter = empties.iterator();
				chosenMove = null;

				while (chosenMove == null && iter.hasNext()) {
					var c = iter.next();
					var isFillEye = true;

					for (var c1 : c.neighbors)
						isFillEye &= board.get(c1) == gameSet.getNextPlayer();

					if (!isFillEye && gameSet.playIfValid(move = new Move(c))) {
						iter.remove();
						chosenMove = move;
					}
				}

				if (chosenMove != null) {
					if (chosenMove.type == MoveType.CAPTURE) {
						var i = 0;
						capturedPositions.clear();

						// add captured positions back to empty group
						for (var c1 : chosenMove.position.neighbors) {
							var neighborColor = chosenMove.neighborColors[i++];
							if (neighborColor != board.get(c1))
								capturedPositions.addAll(board.findGroup(c1));
						}

						for (var c2 : capturedPositions)
							ShuffleUtil.add(empties, c2);
					}

					nPasses = 0;
				} else {
					gameSet.pass();
					nPasses++;
				}
			}

			return 0 < Evaluator.evaluate(me, board);
		}

		/**
		 * The "play till any player cannot move" version of Monte Carlo.
		 */
		public boolean evaluateRandomOutcome_() {
			var me = gameSet.getNextPlayer();
			var empties = findAllEmptyPositions();
			Coordinate pos;
			Move move = null;

			// move until someone cannot move anymore,
			// or maximum iterations reached
			for (var i = 0; i < 4 * Weiqi.area; i++) {
				move = null;

				// try a random empty position, if that position does not work,
				// calls the heavier possible move method
				if ((pos = Last.of(empties)) != null)
					if (gameSet.playIfValid(move = new Move(pos)))
						empties.remove(empties.size() - 1);
					else
						move = null;

				if (move == null)
					move = removePossibleMove(empties.iterator());

				if (move != null) { // add empty positions back to empty group
					var j = 0;

					for (var c1 : move.position.neighbors)
						if (move.neighborColors[j++] != board.get(c1))
							for (var c2 : board.findGroup(c1))
								ShuffleUtil.add(empties, c2);
				} else
					break; // no moves can be played, current player lost
			}

			if (move == null)
				return gameSet.getNextPlayer() != me;
			else
				return 0 < Evaluator.evaluate(me, board);
		}

		private Move removePossibleMove(Iterator<Coordinate> iter) {
			while (iter.hasNext()) {
				var move = new Move(iter.next());

				if (gameSet.playIfValid(move)) {
					iter.remove();
					return move;
				}
			}

			return null;
		}

		private List<Coordinate> findAllEmptyPositions() {
			var moves = new ArrayList<Coordinate>();

			for (var c : Coordinate.all())
				if (board.get(c) == Occupation.EMPTY)
					ShuffleUtil.add(moves, c);

			return moves;
		}

		@Override
		public UctVisitor<Coordinate> cloneVisitor() {
			return new Visitor(new GameSet(gameSet));
		}
	}

	public static Visitor newVisitor(GameSet gameSet) {
		return new Visitor(gameSet);
	}

}
