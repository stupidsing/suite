package org.suite.doer;

import java.util.List;
import java.util.ListIterator;

import org.suite.Binder;
import org.suite.Journal;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.RuleSearcher;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.Rule;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Station;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates;

public class Prover {

	private RuleSearcher ruleSearcher;
	private RuleSet ruleSet;
	private SystemPredicates systemPredicates = new SystemPredicates(this);

	private static final Node OK = Atom.nil;
	private static final Node FAIL = Atom.create("fail");

	private Node remaining = OK, alternative = FAIL;

	private Journal journal = new Journal();

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

					Tree alt0 = new Tree(TermOp.AND___, right, remaining);
					alternative = alternative != FAIL ? new Tree(TermOp.OR____,
							alt0, alternative) : alt0;
					alternative = new Tree(TermOp.AND___, bt, alternative);
					query = left;
					continue;
				case AND___:
					if (right != OK)
						remaining = new Tree(TermOp.AND___, right, remaining);
					query = left;
					continue;
				case EQUAL_:
					query = isSuccess(bind(left, right));
				}
			} else if (query instanceof Station)
				query = isSuccess(((Station) query).run());

			Boolean result = systemPredicates.call(query);
			if (result != null)
				query = isSuccess(result);

			// Not handled above
			if (query == OK)
				if (remaining != OK) {
					query = remaining;
					remaining = OK;
				} else
					return true;
			else if (query == FAIL)
				if (alternative != FAIL) {
					query = alternative;
					alternative = FAIL;
					remaining = OK;
				} else
					return false;
			else
				query = expand(query);
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
		final Node alt0 = alternative;
		Node ret = FAIL;

		List<Rule> rules = ruleSearcher.getRules(query);
		ListIterator<Rule> iter = rules.listIterator(rules.size());

		while (iter.hasPrevious()) {
			Rule rule = iter.previous();

			Generalizer generalizer = new Generalizer();
			generalizer.setCut(new Station() {
				public boolean run() {
					Prover.this.alternative = alt0;
					return true;
				}
			});

			Node head = generalizer.generalize(rule.getHead());
			Node tail = generalizer.generalize(rule.getTail());

			ret = new Tree(TermOp.OR____ //
					, new Tree(TermOp.AND___ //
							, new Tree(TermOp.EQUAL_ //
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
	 * The roll-back log of variable binds.
	 */
	public Journal getJournal() {
		return journal;
	}

}
