package suite.uct;

import static java.lang.Math.log1p;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static suite.util.Streamlet_.forInt;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import primal.Verbs.Build;
import suite.streamlet.As;
import suite.weiqi.Weiqi;

/**
 * Based on http://senseis.xmp.net/?UCT
 */
public class UctSearch<Move> {

	/**
	 * Larger values give uniform search; smaller values give very selective
	 * search.
	 */
	private static float explorationFactor = .4f;
	private static float raveFactor = 5f;
	private static boolean rave = true;
	private static int maxRaveDepth = 4;

	private int numberOfThreads = 1;
	private int numberOfSimulations = 10000;
	private int boundedTime = 10000;

	private UctVisitor<Move> visitor;
	private UctNode<Move> root, best;
	private Map<Move, AtomicInteger> nRaveWins = new HashMap<>();
	private Map<Move, AtomicInteger> nRaveVisits = new HashMap<>();

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

	public UctSearch(UctVisitor<Move> visitor) {
		this.visitor = visitor;
	}

	public Move search() {
		for (var move : visitor.getAllMoves()) {
			nRaveWins.put(move, new AtomicInteger());
			nRaveVisits.put(move, new AtomicInteger());
		}

		root = new UctNode<>();
		var count = new AtomicInteger();
		var end = System.currentTimeMillis() + boundedTime;

		forInt(numberOfThreads).collect(As.executeThreadsByInt(i -> {
			var j = 0;

			while (count.getAndIncrement() < numberOfSimulations) {
				playSimulation(visitor.cloneVisitor(), root, 0);

				if (100 < ++j) {
					j = 0;
					if (end < System.currentTimeMillis())
						break;
				}
			}
		}));

		// finds best node
		best = root.bestChild;
		return best != null ? best.move : null;
	}

	/**
	 * Plays a simulation UCT.
	 *
	 * @return true if the next player will win after UCT selections and
	 *         evaluation after random moves.
	 */
	private boolean playSimulation(UctVisitor<Move> visitor, UctNode<Move> node, int depth) {
		boolean outcome;

		if (node.nVisits != 0) {

			// generate moves, if not done before
			synchronized (node) {
				if (node.child == null) {
					UctNode<Move> child = null;

					for (var move : visitor.elaborateMoves()) {
						UctNode<Move> newChild = new UctNode<>(move);
						newChild.sibling = child;
						child = newChild;
					}

					node.child = child;
				}
			}

			// UCT selection
			var child = node.child;
			UctNode<Move> bestSelected = null;
			var pnRaveVisits = getMoveRave(nRaveVisits, node.move);
			var lnPnVisits = log1p(node.nVisits);
			var lnPnRaveVisits = log1p(pnRaveVisits);
			var bestUct = -Double.MAX_VALUE;

			while (child != null) {
				double uct;

				// only calculate UCT when required, that is, if all children
				// have been evaluated at least once
				if (0 < child.nVisits) {
					if (bestUct < (uct = uct(child, lnPnVisits, lnPnRaveVisits))) {
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
				outcome = true; // no possible move for opponent
		} else
			outcome = !visitor.evaluateRandomOutcome();

		// update rave statistics
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

	private double uct(UctNode<Move> child, double lnParentVisits, double lnParentRaveVisits) {
		var beta = rave ? lnParentVisits / raveFactor : 1d;
		beta = min(max(beta, 0d), 1d);

		double raveWins = getMoveRave(nRaveWins, child.move);
		double raveVisits = getMoveRave(nRaveVisits, child.move);
		var rave = raveWins / raveVisits + (float) explorationFactor * sqrt(lnParentRaveVisits / (5d * raveVisits));

		double wins = child.nWins;
		double visits = child.nVisits;
		var uct = wins / visits + explorationFactor * (float) sqrt(lnParentVisits / (5f * visits));

		return (1d - beta) * rave + beta * uct;
	}

	private void incrementMoveRave(Map<Move, AtomicInteger> raveMap, Move move) {
		raveMap.get(move).incrementAndGet();
	}

	private static DecimalFormat df3 = new DecimalFormat("0.000");

	public void dumpSearch() {
		System.out.println(Build.string(sb -> dumpSearch(sb, 0, null, root)));
	}

	private void dumpSearch(StringBuilder sb, int indent, UctNode<Move> parent, UctNode<Move> child) {
		if (9 < indent)
			return;

		while (child != null) {
			if (0 < child.nVisits) {
				for (var i = 0; i < indent; i++)
					sb.append('\t');

				var winRate = (float) child.nWins / child.nVisits;
				String uct;
				if (parent != null)
					uct = df3.format(uct(child,
							log1p(parent.nVisits),
							log1p(getMoveRave(nRaveVisits, parent.move))));
				else
					uct = "-";

				sb.append(child.move
						+ ", " + child.nWins + "/" + child.nVisits
						+ ", winRate = " + df3.format(winRate)
						+ ", UCT = " + uct
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
		var bestChild = node.bestChild;
		return (node.move != null ? node.move : "") + (bestChild != null ? " => " + getPrincipalVar(bestChild) : "");
	}

	public void dumpRave() {
		var n = 0;
		for (var move : visitor.getAllMoves()) {
			var nWins = getMoveRave(nRaveWins, move);
			var nTotals = getMoveRave(nRaveVisits, move);
			var s = 0 < nTotals ? df3.format(nWins / nTotals) : "  -  ";
			System.out.print(s + " ");

			if (++n % Weiqi.size == 0)
				System.out.println();
		}
	}

	private int getMoveRave(Map<Move, AtomicInteger> raveMap, Move move) {
		return move != null ? raveMap.get(move).get() : 0;
	}

	public float getWinningChance() {
		return (float) best.nWins / best.nVisits;
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
