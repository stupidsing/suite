package org.suite.doer;

import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import org.suite.Binder;
import org.suite.Journal;
import org.suite.doer.Parser.Operator;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.Rule;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Station;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates;
import org.util.Util.Pair;

public class Prover {

	private RuleSet ruleSet;
	private SystemPredicates systemPredicates = new SystemPredicates(this);

	public static class Env extends Pair<Node, Integer> {
		public Env() {
			super();
		}

		public Env(Node t1, Integer t2) {
			super(t1, t2);
		}
	}

	public class Backtracks extends Stack<Env> {
		private static final long serialVersionUID = 1L;
	}

	private static final Node OK = Atom.nil;
	private static final Node FAIL = Atom.create("fail");

	Journal journal = new Journal();
	Backtracks backtracks = new Backtracks();

	public Prover(Prover prover) {
		this(prover.ruleSet);
	}

	public Prover(RuleSet ruleSet) {
		this.ruleSet = ruleSet;
	}

	/**
	 * Try to prove a query clause. Perform bindings on the way.
	 * 
	 * @param query
	 *            Clause to be proved.
	 * @return true if success.
	 */
	public boolean prove(Node query) {
		Node remaining = OK;

		while (query != OK || remaining != OK) {
			// LogFactory.getLog(getClass()).info(Formatter.dump(query));

			Tree tree = Tree.decompose(query);
			if (tree != null) {
				Node left = tree.getLeft(), right = tree.getRight();

				switch (tree.getOperator()) {
				case OR____:
					Tree option = new Tree(Operator.AND___, right, remaining);
					backtracks.push(new Env(option, journal.getPointInTime()));
					query = left;
					continue;
				case AND___:
					if (right != OK)
						remaining = new Tree(Operator.AND___, right, remaining);
					query = left;
					continue;
				case EQUAL_:
					query = isSuccess(bind(left, right));
				}
			} else if (query instanceof Station)
				query = isSuccess(((Station) query).run(backtracks));

			Boolean result = systemPredicates.call(query);
			if (result != null)
				query = isSuccess(result);

			// Not handled above
			if (query == OK)
				query = remaining;
			else if (query == FAIL)
				if (!backtracks.empty()) {
					Env env = backtracks.pop();
					query = env.t1;
					journal.undoBinds(env.t2);
				} else
					return false;
			else
				query = expand(query, remaining);

			remaining = OK;
		}

		return true;
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
	private Node expand(Node query, Node remaining) {
		Node ret = FAIL;

		List<Rule> rules = ruleSet.getRules(query);
		ListIterator<Rule> iter = rules.listIterator(rules.size());

		while (iter.hasPrevious()) {
			Rule rule = iter.previous();

			Generalizer generalizer = new Generalizer();
			final int pit = backtracks.size();
			generalizer.setCut(new Station() {
				public boolean run(Backtracks backtracks) {
					backtracks.setSize(pit);
					return true;
				}
			});

			Node head = generalizer.generalize(rule.getHead());
			Node tail = generalizer.generalize(rule.getTail());

			ret = //
			new Tree(Operator.OR____, //
					new Tree(Operator.AND___, //
							new Tree(Operator.EQUAL_, //
									query, //
									head //
							), //
							new Tree(Operator.AND___, //
									tail, //
									remaining)), //
					ret);
		}

		return ret;
	}

	/**
	 * Allows access from predicates.
	 */
	public RuleSet getRuleSet() {
		return ruleSet;
	}

}
