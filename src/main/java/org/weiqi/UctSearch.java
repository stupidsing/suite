package org.weiqi;

import java.util.Random;

/**
 * Based on http://senseis.xmp.net/?UCT
 */
public class UctSearch<Move> {

	/**
	 * Larger values give uniform search; smaller values give very selective
	 * search.
	 */
	private final static int searchRatio = 1;

	private final static int nSimulations = 10000;

	private UctVisitor<Move> visitor;
	private Random random = new Random();

	private static class UctNode<Move> {
		private Move move;
		private int nWins, nVisits;
		private UctNode<Move> child, sibling, bestChild;

		private UctNode() {
		}

		private UctNode(Move move) {
			this.move = move;
		}
	}

	public Move search() {
		UctNode<Move> root = new UctNode<Move>();

		for (int i = 0; i < nSimulations; i++) {
			// TODO clone
			playSimulation(root);
		}

		return root.bestChild.move;
	}

	private boolean playSimulation(UctNode<Move> node) {
		boolean outcome;

		if (node.nVisits != 0) {
			UctNode<Move> childNode = null;

			for (Move move : visitor.elaborateMoves()) {
				UctNode<Move> newChildNode = new UctNode<Move>(move);
				newChildNode.child = childNode;
				childNode = newChildNode;
			}

			node.child = childNode;

			// UCT selection
			UctNode<Move> child = node.child;
			UctNode<Move> bestSelected = null;
			float bestUct = -Float.MAX_VALUE;

			while (child != null) {
				float uct;
				if (child.nVisits > 0)
					uct = uct(node, child);
				else
					uct = 10000f + 1000f * random.nextFloat();

				if (uct > bestUct) {
					bestSelected = child;
					bestUct = uct;
				}

				child = child.sibling;
			}

			visitor.playMove(bestSelected.move);
			outcome = playSimulation(bestSelected);
		} else
			outcome = visitor.evaluateRandomOutcome();

		node.nVisits++;
		node.nWins += outcome ? 1 : 0;
		return outcome;
	}

	private float uct(UctNode<Move> node, UctNode<Move> child) {
		float nWins = child.nWins;
		float nVisits = child.nVisits;
		return nWins / nVisits + searchRatio //
				* (float) Math.sqrt(Math.log(node.nVisits) / (5f * nVisits));
	}

}
