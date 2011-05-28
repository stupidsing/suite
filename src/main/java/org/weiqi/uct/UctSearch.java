package org.weiqi.uct;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.util.LogUtil;
import org.util.Util;
import org.weiqi.Weiqi;

/**
 * Based on http://senseis.xmp.net/?UCT
 */
public class UctSearch<Move> {

	/**
	 * Larger values give uniform search; smaller values give very selective
	 * search.
	 */
	private final static float explorationFactor = 0.4f;
	private final static float raveFactor = 5f;
	private final static boolean rave = true;
	private final static int maxRaveDepth = 4;

	public int numberOfThreads = 2;
	public int numberOfSimulations = 10000;
	public int boundedTime = 10000;

	private UctVisitor<Move> visitor;
	private UctNode<Move> root, best;
	private Map<Move, AtomicInteger> nRaveWins = Util.createHashMap();
	private Map<Move, AtomicInteger> nRaveVisits = Util.createHashMap();

	public static class UctNode<Move> {
		private Move move;
		private int nWins, nVisits;
		private UctNode<Move> child, sibling, bestChild;

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
		for (Move move : visitor.getAllMovesOnBoard()) {
			nRaveWins.put(move, new AtomicInteger());
			nRaveVisits.put(move, new AtomicInteger());
		}

		root = new UctNode<Move>();
		Thread threads[] = new Thread[numberOfThreads];
		final AtomicInteger count = new AtomicInteger();
		final long end = System.currentTimeMillis() + boundedTime;

		for (int i = 0; i < numberOfThreads; i++)
			(threads[i] = new SearchThread(count, end)).start();

		try {
			for (int i = 0; i < numberOfThreads; i++)
				threads[i].join();
		} catch (InterruptedException ex) {
			LogUtil.error("", ex);
		}

		// Finds best node
		best = root.bestChild;
		return best != null ? best.move : null;
	}

	private final class SearchThread extends Thread {
		private AtomicInteger count;
		private long end;

		private SearchThread(AtomicInteger count, long end) {
			this.count = count;
			this.end = end;
		}

		public void run() {
			int i = 0;

			while (count.getAndIncrement() < numberOfSimulations) {
				playSimulation(visitor.cloneVisitor(), root, 0);

				if (++i > 10) {
					i = 0;
					if (System.currentTimeMillis() > end)
						break;
				}
			}
		}
	}

	/**
	 * Plays a simulation UCT.
	 * 
	 * @return true if the next player will win after UCT selections and
	 *         evaluation after random moves.
	 */
	private boolean playSimulation(UctVisitor<Move> visitor,
			UctNode<Move> node, int depth) {
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
			int pnRaveVisits = getMoveRave(nRaveVisits, node.move);
			double lnPnVisits = Math.log(node.nVisits + 1);
			double lnPnRaveVisits = Math.log(pnRaveVisits + 1);
			float bestUct = -Float.MAX_VALUE;

			while (child != null) {
				float uct;

				// Only calculates UCT when required, that is, if all children
				// have been evaluated at least once
				if (child.nVisits > 0) {
					if ((uct = uct(child, lnPnVisits, lnPnRaveVisits)) > bestUct) {
						bestSelected = child;
						bestUct = uct;
					}
				} else {
					bestSelected = child;
					break;
				}

				child = child.sibling;
			}

			if (bestSelected != null) {
				node.bestChild = bestSelected;
				visitor.playMove(bestSelected.move);
				outcome = playSimulation(visitor, bestSelected, depth + 1);
			} else
				outcome = true; // No possible move for opponent
		} else
			outcome = !visitor.evaluateRandomOutcome();

		// Updates rave statistics
		if (node.move != null && depth < maxRaveDepth) {
			incrementMoveRave(nRaveVisits, node.move);
			if (outcome)
				incrementMoveRave(nRaveWins, node.move);
		}

		synchronized (node) {
			node.nVisits++;
			node.nWins += outcome ? 1 : 0;
		}

		return !outcome;
	}

	private float uct(UctNode<Move> child, double lnParentVisits,
			double lnParentRaveVisits) {
		float beta = rave ? (float) (lnParentVisits / raveFactor) : 1f;
		beta = Math.min(Math.max(beta, 0f), 1f);

		float raveWins = getMoveRave(nRaveWins, child.move);
		float raveVisits = getMoveRave(nRaveVisits, child.move);
		float rave = raveWins / raveVisits + explorationFactor
				* (float) Math.sqrt(lnParentRaveVisits / (5f * raveVisits));

		float wins = child.nWins;
		float visits = child.nVisits;
		float uct = wins / visits + explorationFactor
				* (float) Math.sqrt(lnParentVisits / (5f * visits));

		return (1f - beta) * rave + beta * uct;
	}

	private int getMoveRave(Map<Move, AtomicInteger> raveMap, Move move) {
		return move != null ? raveMap.get(move).get() : 0;
	}

	private void incrementMoveRave(Map<Move, AtomicInteger> raveMap, Move move) {
		raveMap.get(move).incrementAndGet();
	}

	private final static DecimalFormat df3 = new DecimalFormat("0.000");

	public void dumpSearch() {
		StringBuilder sb = new StringBuilder();
		dumpSearch(sb, 0, null, root);
		System.out.println(sb);
	}

	private void dumpSearch(StringBuilder sb, int indent, UctNode<Move> parent,
			UctNode<Move> child) {
		if (indent > 9)
			return;

		while (child != null) {
			if (child.nVisits > 0) {
				for (int i = 0; i < indent; i++)
					sb.append('\t');

				float winRate = ((float) child.nWins) / child.nVisits;
				String uct;
				if (parent != null)
					uct = df3.format(uct(child //
							, Math.log(parent.nVisits) //
							, Math.log(getMoveRave(nRaveVisits, parent.move))));
				else
					uct = "-";

				sb.append(child.move //
						+ ", " + child.nWins + "/" + child.nVisits //
						+ ", winRate = " + df3.format(winRate) //
						+ ", UCT = " + uct //
						+ "\n");

				if (parent == null || parent.bestChild == child)
					dumpSearch(sb, indent + 1, child, child.child);
			}

			child = child.sibling;
		}
	}

	public void dumpPrincipalVariation() {
		System.out.println("PRINCIPAL-VARIATION" + getPrincipalVar(root));
	}

	private String getPrincipalVar(UctNode<Move> node) {
		UctNode<Move> bestChild = node.bestChild;
		return (node.move != null ? node.move : "") //
				+ (bestChild != null ? " => " + getPrincipalVar(bestChild) : "");
	}

	public void dumpRave() {
		int n = 0;
		for (Move move : visitor.getAllMovesOnBoard()) {
			float nWins = getMoveRave(nRaveWins, move);
			float nTotals = getMoveRave(nRaveVisits, move);
			String s = nTotals > 0 ? df3.format(nWins / nTotals) : "  -  ";
			System.out.print(s + " ");

			if (++n % Weiqi.SIZE == 0)
				System.out.println();
		}
	}

	public float getWinningChance() {
		return ((float) best.nWins) / best.nVisits;
	}

	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public void setNumberOfSimulations(int numberOfSimulations) {
		this.numberOfSimulations = numberOfSimulations;
	}

	public void setBoundedTime(int boundedTime) {
		this.boundedTime = boundedTime;
	}

}
