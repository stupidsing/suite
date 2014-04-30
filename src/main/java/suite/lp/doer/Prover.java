package suite.lp.doer;

import java.util.Date;
import java.util.List;
import java.util.Set;

import suite.lp.Journal;
import suite.lp.doer.Configuration.ProverConfig;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.predicate.SystemPredicates;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Suspend;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;
import suite.util.To;
import suite.util.Util;

public class Prover {

	private ProverConfig config;
	private ProveTracer tracer;
	private SystemPredicates systemPredicates = new SystemPredicates(this);

	private static Node OK = Atom.NIL;
	private static Node FAIL = Atom.create("fail");

	private Node rem, alt; // remaining, alternative

	private Journal journal = new Journal();
	private int initialPointInTime;

	public Prover(Prover prover) {
		this(prover.config, prover.tracer, prover.journal);
	}

	public Prover(RuleSet ruleSet) {
		this(new ProverConfig(ruleSet));
	}

	public Prover(ProverConfig proverConfig) {
		this(proverConfig, null, new Journal());
	}

	public Prover(ProverConfig proverConfig, ProveTracer tracer, Journal journal) {
		this.config = proverConfig;
		this.tracer = tracer;
		this.journal = journal;
		initialPointInTime = journal.getPointInTime();
	}

	public void elaborate(Node query) {
		try {
			prove(query);
		} finally {
			undoAllBinds();
		}
	}

	/**
	 * Try to prove a query clause. Perform bindings on the way.
	 * 
	 * @param query
	 *            Clause to be proved.
	 * @return true if success.
	 */
	public boolean prove(Node query) {
		Thread hook = new Thread() {
			public void run() {
				String d = To.string(new Date());
				LogUtil.info("-- Trace dump at " + d + " --\n" + tracer.getTrace());
				LogUtil.info("-- Fail dump at " + d + " --\n" + tracer.getFailTrace());
			}
		};

		if (config.isTrace())
			try {
				Runtime.getRuntime().addShutdownHook(hook);
				tracer = new ProveTracer(config);
				return prove0(query);
			} finally {
				hook.run();
				Runtime.getRuntime().removeShutdownHook(hook);
			}
		else
			return prove0(query);
	}

	public boolean prove0(Node query) {
		rem = OK;
		alt = FAIL;

		while (true) {
			// LogUtil.info(Formatter.dump(query));
			query = query.finalNode();

			if (query instanceof Tree) {
				Tree tree = (Tree) query;
				Node left = tree.getLeft(), right = tree.getRight();

				switch ((TermOp) tree.getOperator()) {
				case OR____:
					int pit = journal.getPointInTime();
					Node bt = new Data<>(new Source<Boolean>() {
						public Boolean source() {
							journal.undoBinds(pit);
							return Boolean.TRUE;
						}
					});

					alt = andTree(bt, orTree(andTree(right, rem), alt));
					query = left;
					continue;
				case AND___:
					rem = andTree(right, rem);
					query = left;
					continue;
				case EQUAL_:
					query = isSuccess(bind(left, right));
					break;
				default:
				}
			} else if (query instanceof Data) {
				Source<Boolean> source = Data.get(query);
				query = isSuccess(source.source());
				continue;
			}

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
				boolean isTrace = config.isTrace();

				if (isTrace) {
					Set<String> whites = config.getTracePredicates();
					Set<String> blacks = config.getNoTracePredicates();

					Prototype prototype = Prototype.get(query);
					Node head = prototype != null ? prototype.getHead() : null;
					Atom atom = head instanceof Atom ? (Atom) head : null;
					String name = atom != null ? atom.getName() : null;
					isTrace &= whites == null || whites.contains(name);
					isTrace &= blacks == null || !blacks.contains(name);
				}

				if (!isTrace)
					query = expand(query);
				else
					query = tracer.expandWithTrace(query, this, new Fun<Node, Node>() {
						public Node apply(Node node) {
							return expand(node);
						}
					});
			}
		}
	}

	/**
	 * Expands an user predicate (with many clauses) to a chain of logic.
	 * 
	 * @param query
	 *            The invocation pattern.
	 * @return The chained node.
	 */
	private Node expand(Node query) {
		Node alt0 = alt;

		Data<?> cut = new Data<>(new Source<Boolean>() {
			public Boolean source() {
				alt = alt0;
				return Boolean.TRUE;
			}
		});

		return expandClauses(query, cut, config.ruleSet().searchRule(query));
	}

	private Node expandClauses(Node query, Node cut, List<Rule> rules) {
		return new Suspend(new Source<Node>() {
			public Node source() {
				if (!rules.isEmpty()) {
					Rule rule = rules.get(0);

					// Delay generalizing for performance
					Generalizer generalizer = new Generalizer();
					generalizer.setCut(cut);

					Node head = generalizer.generalize(rule.getHead());
					Node tail = generalizer.generalize(rule.getTail());

					Node clause = Tree.create(TermOp.AND___ //
							, Tree.create(TermOp.EQUAL_ //
									, query //
									, head) //
							, tail);

					return Tree.create(TermOp.OR____, clause, expandClauses(query, cut, Util.right(rules, 1)));
				} else
					return FAIL;
			}
		});
	}

	/**
	 * Performs binding of two items.
	 * 
	 * @return true if success.
	 */
	public boolean bind(Node left, Node right) {
		return Binder.bind(left, right, journal);
	}

	/**
	 * Resets all bind done by this prover.
	 */
	public void undoAllBinds() {
		journal.undoBinds(initialPointInTime);
	}

	private Node andTree(Node n0, Node n1) {
		return formOp(n0, n1, OK, FAIL, TermOp.AND___);
	}

	private Node orTree(Node n0, Node n1) {
		return formOp(n0, n1, FAIL, OK, TermOp.OR____);
	}

	private Node formOp(Node n0, Node n1, Node bail, Node done, TermOp op) {
		if (n0 == bail)
			return n1;
		else if (n1 == bail)
			return n0;
		else if (n0 == done || n1 == done)
			return done;
		else
			return Tree.create(op, n0, n1);
	}

	private Node isSuccess(boolean b) {
		return b ? OK : FAIL;
	}

	public ProverConfig config() {
		return config;
	}

	public RuleSet ruleSet() {
		return config.ruleSet();
	}

	public ProveTracer getTracer() {
		return tracer;
	}

	/**
	 * Goals ahead.
	 */
	public Node getRemaining() {
		return rem;
	}

	public void setRemaining(Node rem) {
		this.rem = rem;
	}

	/**
	 * Alternative path to succeed.
	 */
	public Node getAlternative() {
		return alt;
	}

	public void setAlternative(Node alt) {
		this.alt = alt;
	}

	/**
	 * The roll-back log of variable binds.
	 */
	public Journal getJournal() {
		return journal;
	}

}
