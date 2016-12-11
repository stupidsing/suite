package suite.lp.doer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import suite.Suite;
import suite.lp.Configuration.ProverConfig;
import suite.lp.Trail;
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
import suite.os.LogUtil;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class Prover {

	private ProverConfig config;
	private ProveTracer tracer;
	private SystemPredicates systemPredicates = new SystemPredicates(this);

	private static Node OK = Atom.NIL;
	private static Node FAIL = Atom.of("fail");

	private Node rem, alt; // remaining, alternative

	private Trail trail;
	private int initialPointInTime;

	public Prover(Prover prover) {
		this(prover.config, prover.tracer, prover.trail);
	}

	public Prover(RuleSet ruleSet) {
		this(new ProverConfig(ruleSet));
	}

	public Prover(ProverConfig proverConfig) {
		this(proverConfig, null, new Trail());
	}

	public Prover(ProverConfig proverConfig, ProveTracer tracer, Trail trail) {
		this.config = proverConfig;
		this.tracer = tracer;
		this.trail = trail;
		initialPointInTime = trail.getPointInTime();
	}

	public void elaborate(Node query) {
		try {
			prove(query);
		} finally {
			unwindAll();
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
		Thread hook = new Thread(() -> {
			String d = LocalDateTime.now().toString();
			LogUtil.info("-- trace dump at " + d + " --\n" + tracer.getTrace());
			LogUtil.info("-- fail dump at " + d + " --\n" + tracer.getFailTrace());
		});

		if (config.isTrace())
			try {
				Runtime.getRuntime().addShutdownHook(hook);
				tracer = new ProveTracer();
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
			// logUtil.info(Formatter.dump(query));
			query = query.finalNode();

			if (query instanceof Tree) {
				Tree tree = (Tree) query;
				Node left = tree.getLeft(), right = tree.getRight();

				switch ((TermOp) tree.getOperator()) {
				case OR____:
					int pit = trail.getPointInTime();
					Node bt = new Data<Source<Boolean>>(() -> {
						trail.unwind(pit);
						return Boolean.TRUE;
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
				query = isSuccess(Data.<Source<Boolean>> get(query).source());
				continue;
			}

			Boolean result = systemPredicates.call(query);
			if (result != null)
				query = isSuccess(result);

			// not handled above
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
					Set<String> whites = Suite.tracePredicates;
					Set<String> blacks = Suite.noTracePredicates;

					Prototype prototype = Prototype.of(query);
					Node head = prototype != null ? prototype.head : null;
					Atom atom = head instanceof Atom ? (Atom) head : null;
					String name = atom != null ? atom.name : null;
					isTrace &= whites == null || whites.contains(name);
					isTrace &= blacks == null || !blacks.contains(name);
				}

				if (!isTrace)
					query = expand(query);
				else
					query = tracer.expandWithTrace(query, this, this::expand);
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

		Data<Source<Boolean>> cut = new Data<>(() -> {
			alt = alt0;
			return Boolean.TRUE;
		});

		return expandClauses(query, cut, config.ruleSet().searchRule(query));
	}

	private Node expandClauses(Node query, Node cut, List<Rule> rules) {
		return new Suspend(() -> {
			if (!rules.isEmpty()) {
				Node clause = rules.get(0).createClause(query, cut);
				return Tree.of(TermOp.OR____, clause, expandClauses(query, cut, Util.right(rules, 1)));
			} else
				return FAIL;
		});
	}

	/**
	 * Performs binding of two items.
	 *
	 * @return true if success.
	 */
	public boolean bind(Node left, Node right) {
		return Binder.bind(left, right, trail);
	}

	/**
	 * Resets all bind done by this prover.
	 */
	public void unwindAll() {
		trail.unwind(initialPointInTime);
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
			return Tree.of(op, n0, n1);
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
	public Trail getTrail() {
		return trail;
	}

}
