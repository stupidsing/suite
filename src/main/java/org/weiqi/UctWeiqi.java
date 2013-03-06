package org.weiqi;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.weiqi.Board.MoveType;
import org.weiqi.GameSet.Move;
import org.weiqi.Weiqi.Occupation;
import org.weiqi.uct.UctVisitor;

public class UctWeiqi {

	public static class Visitor implements UctVisitor<Coordinate> {
		private final GameSet gameSet;
		private final Board board;

		private Visitor(GameSet gameSet) {
			this.gameSet = gameSet;
			this.board = gameSet.getBoard();
		}

		@Override
		public UctVisitor<Coordinate> cloneVisitor() {
			return new Visitor(new GameSet(gameSet));
		}

		@Override
		public Iterable<Coordinate> getAllMovesOnBoard() {
			return Coordinate.all();
		}

		@Override
		public List<Coordinate> elaborateMoves() {
			Move move = new Move();
			RandomableList<Coordinate> captureMoves = new RandomableList<>();
			RandomableList<Coordinate> otherMoves = new RandomableList<>();

			for (Coordinate c : Coordinate.all())
				if (board.get(c) == Occupation.EMPTY) {
					move.position = c;

					if (gameSet.isValidMove(move))
						if (move.type == MoveType.CAPTURE)
							captureMoves.addByRandomSwap(c);
						else
							otherMoves.addByRandomSwap(c);
				}

			// Make capture moves at the head;
			// UctSearch would put them in first few nodes
			captureMoves.addAll(otherMoves);
			return captureMoves;
		}

		@Override
		public void playMove(Coordinate c) {
			gameSet.play(new Move(c));
		}

		/**
		 * The "play till both player passes" Monte Carlo.
		 */
		@Override
		public boolean evaluateRandomOutcome() {
			RandomableList<Coordinate> empties = findAllEmptyPositions();
			Set<Coordinate> capturedPositions = new HashSet<>();
			Occupation me = gameSet.getNextPlayer();
			Move move, chosenMove = null;
			int nPasses = 0;

			// Move until someone cannot move anymore, or maximum number of
			// passes is reached between both players
			while (nPasses < 2) {
				Iterator<Coordinate> iter = empties.iterator();
				chosenMove = null;

				while (chosenMove == null && iter.hasNext()) {
					Coordinate c = iter.next();
					boolean isFillEye = true;

					for (Coordinate c1 : c.neighbours())
						isFillEye &= board.get(c1) == gameSet.getNextPlayer();

					if (!isFillEye && gameSet.playIfValid(move = new Move(c))) {
						iter.remove();
						chosenMove = move;
					}
				}

				if (chosenMove != null) {
					int j = 0;
					capturedPositions.clear();

					// Add captured positions back to empty group
					for (Coordinate c1 : chosenMove.position.neighbours())
						if (chosenMove.neighbourColors[j++] != board.get(c1))
							capturedPositions.addAll(board.findGroup(c1));

					for (Coordinate c2 : capturedPositions)
						empties.addByRandomSwap(c2);

					nPasses = 0;
				} else {
					gameSet.pass();
					nPasses++;
				}
			}

			return Evaluator.evaluate(me, board) > 0;
		}

		/**
		 * The "play till any player cannot move" version of Monte Carlo.
		 */
		public boolean evaluateRandomOutcome0() {
			Occupation me = gameSet.getNextPlayer();
			RandomableList<Coordinate> empties = findAllEmptyPositions();
			Coordinate pos;
			Move move = null;

			// Move until someone cannot move anymore,
			// or maximum iterations reached
			for (int i = 0; i < 4 * Weiqi.area; i++) {
				move = null;

				// Try a random empty position, if that position does not work,
				// calls the heavier possible move method
				if ((pos = empties.last()) != null)
					if (gameSet.playIfValid(move = new Move(pos)))
						empties.removeLast();
					else
						move = null;

				if (move == null)
					move = removePossibleMove(empties.iterator());

				if (move != null) { // Add empty positions back to empty group
					int j = 0;

					for (Coordinate c1 : move.position.neighbours())
						if (move.neighbourColors[j++] != board.get(c1))
							for (Coordinate c2 : board.findGroup(c1))
								empties.addByRandomSwap(c2);
				} else
					break; // No moves can be played, current player lost
			}

			if (move == null)
				return gameSet.getNextPlayer() != me;
			else
				return Evaluator.evaluate(me, board) > 0;
		}

		private Move removePossibleMove(Iterator<Coordinate> iter) {
			while (iter.hasNext()) {
				Move move = new Move(iter.next());

				if (gameSet.playIfValid(move)) {
					iter.remove();
					return move;
				}
			}

			return null;
		}

		private RandomableList<Coordinate> findAllEmptyPositions() {
			RandomableList<Coordinate> moves = new RandomableList<>();

			for (Coordinate c : Coordinate.all())
				if (board.get(c) == Occupation.EMPTY)
					moves.addByRandomSwap(c);

			return moves;
		}
	}

	public static Visitor createVisitor(GameSet gameSet) {
		return new Visitor(gameSet);
	}

}
