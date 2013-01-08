package org.suite.doer;

import java.util.List;
import java.util.ListIterator;

import org.suite.Binder;
import org.suite.Journal;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.Prototype;
import org.suite.kb.RuleSearcher;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.Rule;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates;

public class Prover {

	private RuleSearcher ruleSearcher;
	private RuleSet ruleSet;
	private SystemPredicates systemPredicates = new SystemPredicates(this);

	private boolean isEnableTrace = false;

	private static final Node OK = Atom.nil;
	private static final Node FAIL = Atom.create("fail");

	private Node rem, alt; // remaining, alternative

	private Journal journal = new Journal();
	private Node trace = Atom.nil;
	private int depth;

	public Prover(Prover prover) {
		this(prover.ruleSet);
	}

	public Prover(RuleSet ruleSet) {
		this.ruleSearcher = ruleSet;
		this.ruleSet = ruleSet;
	}

	public Prover(RuleSearcher ruleSearcher, Prover prover) {
		this(prover.ruleSet);
		this.ruleSearcher = ruleSearcher;
	}

	/**
	 * Try to prove a query clause. Perform bindings on the way.
	 * 
	 * @param query
	 *            Clause to be proved.
	 * @return true if success.
	 */
	public boolean prove(Node query) {
		rem = OK;
		alt = FAIL;

		while (true) {
			// LogUtil.info("PROVE", Formatter.dump(query));

			Tree tree = Tree.decompose(query);
			if (tree != null) {
				final Node left = tree.getLeft(), right = tree.getRight();

				switch ((TermOp) tree.getOperator()) {
				case OR____:
					final int pit = journal.getPointInTime();
					Node bt = new Station() {
						public boolean run() {
							journal.undoBinds(pit);
							return true;
						}
					};

					Tree alt0 = Tree.create(TermOp.AND___, right, rem);
					alt = alt != FAIL ? Tree.create(TermOp.OR____, alt0, alt)
							: alt0;
					alt = Tree.create(TermOp.AND___, bt, alt);
					query = left;
					continue;
				case AND___:
					if (right != OK)
						rem = Tree.create(TermOp.AND___, right, rem);
					query = left;
					continue;
				case EQUAL_:
					query = isSuccess(bind(left, right));
					break;
				default:
				}
			} else if (query instanceof Station)
				query = isSuccess(((Station) query).run());

			Boolean result = systemPredicates.call(query);
			if (result != null)
				query = isSuccess(result);

			// Not handled above
			if (query == OK)
				if (rem != OK) {
					query = rem;
					rem = OK;
				} else
					return true;
			else if (query == FAIL)
				if (alt != FAIL) {
					query = alt;
					alt = FAIL;
					rem = OK;
				} else
					return false;
			else {
				boolean isTrace = isEnableTrace;
				Prototype prototype = isTrace ? Prototype.get(query) : null;
				Node head = prototype != null ? prototype.getHead() : null;
				Atom atom = head instanceof Atom ? (Atom) head : null;
				String name = atom != null ? atom.getName() : null;
				isTrace &= !"member".equals(name) && !"replace".equals(name);

				if (!isTrace)
					query = expand(query);
				else
					query = expandWithTrace(query);
			}
		}
	}

	/**
	 * Performs binding of two items.
	 * 
	 * @Return true if success.
	 */
	public boolean bind(Node left, Node right) {
		return Binder.bind(left, right, journal);
	}

	/**
	 * Resets all bind done by this prover.
	 */
	public void undoAllBinds() {
		journal.undoBinds(0);
	}

	private Node isSuccess(boolean b) {
		return b ? OK : FAIL;
	}

	private Node expandWithTrace(Node query) {
		final Node query1 = new Cloner().clone(query);

		final Node trace0 = trace;
		final Node trace1 = Tree.create(TermOp.AND___, query1, trace0);
		final int depth0 = depth;
		final int depth1 = depth + 1;

		final Station enter = new Station() {
			public boolean run() {
				Prover.this.trace = trace1;
				Prover.this.depth = depth1;
				showLog("ENTER", query1, depth1);
				return true;
			}
		};

		final Station leaveOk = new Station() {
			public boolean run() {
				Prover.this.trace = trace0;
				Prover.this.depth = depth0;
				return true;
			}
		};

		final Station leaveFail = new Station() {
			public boolean run() {
				Prover.this.trace = trace0;
				Prover.this.depth = depth0;
				showLog("LEAVE", query1, depth1);
				return false;
			}
		};

		alt = Tree.create(TermOp.OR____, leaveFail, alt);
		rem = Tree.create(TermOp.AND___, leaveOk, rem);
		query = expand(query);
		query = Tree.create(TermOp.AND___, enter, query);
		return query;
	}

	private void showLog(String message, Node query, int depth) {
		if (message != null)
			System.err.println("[" + message //
					+ ":" + depth //
					+ "] " + Formatter.dump(query));
	}

	/**
	 * Expands an user predicate (with many clauses) to a chain of logic.
	 * 
	 * @param query
	 *            The invocation pattern.
	 * @param remaining
	 *            The final goal to be appended.
	 * @return The chained node.
	 */
	private Node expand(Node query) {
		final Node alt0 = alt;
		Node ret = FAIL;

		List<Rule> rules = ruleSearcher.getRules(query);
		ListIterator<Rule> iter = rules.listIterator(rules.size());

		while (iter.hasPrevious()) {
			Rule rule = iter.previous();

			Generalizer generalizer = new Generalizer();
			generalizer.setCut(new Station() {
				public boolean run() {
					Prover.this.alt = alt0;
					return true;
				}
			});

			Node head = generalizer.generalize(rule.getHead());
			Node tail = generalizer.generalize(rule.getTail());

			ret = Tree.create(TermOp.OR____ //
					, Tree.create(TermOp.AND___ //
							, Tree.create(TermOp.EQUAL_ //
									, query //
									, head //
							) //
							, tail //
					) //
					, ret //
					);
		}

		return ret;
	}

	/**
	 * The set of rules which is read-only.
	 */
	public RuleSearcher getRuleSearcher() {
		return ruleSearcher;
	}

	/**
	 * The set of rules which is mutable (may assert/retract).
	 * 
	 * Allows access from predicates.
	 */
	public RuleSet getRuleSet() {
		return ruleSet;
	}

	/**
	 * Allows taking stack dump, with performance hit.
	 */
	public void setEnableTrace(boolean isEnableTrace) {
		this.isEnableTrace = isEnableTrace;
	}

	/**
	 * The roll-back log of variable binds.
	 */
	public Journal getJournal() {
		return journal;
	}

	/**
	 * Gets stack dump when trace is enabled.
	 */
	public Node getTrace() {
		return trace;
	}

	/**
	 * Depth of predicate call trace.
	 */
	public int getDepth() {
		return depth;
	}

}
