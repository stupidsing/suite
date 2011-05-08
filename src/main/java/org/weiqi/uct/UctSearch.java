package org.weiqi.uct;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.util.LogUtil;

/**
 * Based on http://senseis.xmp.net/?UCT
 */
public class UctSearch<Move> {

	/**
	 * Larger values give uniform search; smaller values give very selective
	 * search.
	 */
	private final static float searchRatio = 0.5f;

	public int numberOfThreads = 2;
	public int numberOfSimulations = 10000;

	private UctVisitor<Move> visitor;
	private UctNode<Move> root;

	private Random random = new Random();

	private static class UctNode<Move> {
		private Move move;
		private int nWins, nVisits;
		private UctNode<Move> child, sibling;

		private UctNode() {
		}

		private UctNode(Move move) {
			this.move = move;
		}
	}

	public UctSearch(UctVisitor<Move> visitor) {
		this.visitor = visitor;
	}

	public Move search() {
		root = new UctNode<Move>();
		Thread threads[] = new Thread[numberOfThreads];
		final AtomicInteger count = new AtomicInteger();

		for (int i = 0; i < numberOfThreads; i++) {
			threads[i] = new Thread() {
				public void run() {
					while (count.getAndIncrement() < numberOfSimulations)
						playSimulation(visitor.cloneVisitor(), root);
				}
			};

			threads[i].start();
		}

		for (int i = 0; i < numberOfThreads; i++)
			try {
				threads[i].join();
			} catch (InterruptedException ex) {
				LogUtil.error("", ex);
			}

		UctNode<Move> node = root.child, best = null;

		while (node != null) {
			if (best == null
					|| node.nWins * best.nVisits > node.nVisits * best.nWins)
				best = node;
			node = node.sibling;
		}

		return best != null ? best.move : null;
	}

	private boolean playSimulation(UctVisitor<Move> visitor, UctNode<Move> node) {
		boolean outcome;

		if (node.nVisits != 0) {

			// Generate moves, if not done before
			synchronized (node) {
				if (node.child == null) {
					UctNode<Move> child = null;

					for (Move move : visitor.elaborateMoves()) {
						UctNode<Move> newChild = new UctNode<Move>(move);
						newChild.sibling = child;
						child = newChild;
					}

					node.child = child;
				}
			}

			// UCT selection
			UctNode<Move> child = node.child;
			UctNode<Move> bestSelected = null;
			float bestUct = -Float.MAX_VALUE;

			while (child != null) {
				float uct;
				if (child.nVisits > 0)
					uct = uct(node, child) + random.nextFloat() * 0.1f;
				else
					uct = 10000f + 1000f * random.nextFloat();

				if (uct > bestUct) {
					bestSelected = child;
					bestUct = uct;
				}

				child = child.sibling;
			}

			if (bestSelected != null) {
				visitor.playMove(bestSelected.move);
				outcome = !playSimulation(visitor, bestSelected);
			} else
				outcome = false;
		} else
			outcome = visitor.evaluateRandomOutcome();

		synchronized (node) {
			node.nVisits++;
			node.nWins += outcome ? 0 : 1;
		}

		return outcome;
	}

	private float uct(UctNode<Move> parent, UctNode<Move> child) {
		float nWins = child.nWins;
		float nVisits = child.nVisits;
		return nWins / nVisits + searchRatio //
				* (float) Math.sqrt(Math.log(parent.nVisits) / (5f * nVisits));
	}

	public void dumpSearch() {
		StringBuilder sb = new StringBuilder();
		dumpSearch(sb, 0, null, root);
		System.out.println(sb);
	}

	private final static DecimalFormat df = new DecimalFormat("0.000");

	private void dumpSearch(StringBuilder sb, int indent, UctNode<Move> parent,
			UctNode<Move> child) {
		if (indent < 3) {
			while (child != null) {
				if (child.nVisits > 0) {
					for (int i = 0; i < indent; i++)
						sb.append('\t');

					float winRate = ((float) child.nWins) / child.nVisits;

					String uct;
					if (parent != null)
						uct = df.format(uct(parent, child));
					else
						uct = "-";

					sb.append(child.move //
							+ ", " + child.nWins + "/" + child.nVisits //
							+ ", winRate = " + df.format(winRate) //
							+ ", UCT = " + uct //
							+ "\n");
					dumpSearch(sb, indent + 1, child, child.child);
				}

				child = child.sibling;
			}
		}
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public void setNumberOfSimulations(int numberOfSimulations) {
		this.numberOfSimulations = numberOfSimulations;
	}

}
