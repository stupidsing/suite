package suite.lp.doer;

import java.time.LocalDateTime;
import java.util.List;

import primal.fp.Funs.Source;
import primal.os.Log_;
import suite.Suite;
import suite.lp.Configuration.ProverCfg;
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
import suite.util.List_;

public class Prover {

	private ProverCfg config;
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
		this(new ProverCfg(ruleSet));
	}

	public Prover(ProverCfg proverCfg) {
		this(proverCfg, null, new Trail());
	}

	public Prover(ProverCfg proverCfg, ProveTracer tracer, Trail trail) {
		this.config = proverCfg;
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
		var hook = new Thread(() -> {
			var d = LocalDateTime.now().toString();
			Log_.info("-- trace dump at " + d + " --\n" + tracer.getTrace());
			Log_.info("-- fail dump at " + d + " --\n" + tracer.getFailTrace());
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
				var tree = (Tree) query;
				Node left = tree.getLeft(), right = tree.getRight();

				switch ((TermOp) tree.getOperator()) {
				case OR____:
					var pit = trail.getPointInTime();
					var bt = new Data<Source<Boolean>>(() -> {
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
				query = isSuccess(Data.<Source<Boolean>> get(query).g());
				continue;
			}

			var b = systemPredicates.call(query);
			if (b != null)
				query = isSuccess(b);

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
				var isTrace = config.isTrace();

				if (isTrace) {
					var whites = Suite.tracePredicates;
					var blacks = Suite.noTracePredicates;

					var prototype = Prototype.of(query);
					var head = prototype != null ? prototype.head : null;
					var atom = head instanceof Atom ? (Atom) head : null;
					var name = atom != null ? atom.name : null;
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
		var alt0 = alt;

		Data<Source<Boolean>> cut = new Data<>(() -> {
			alt = alt0;
			return Boolean.TRUE;
		});

		return expandClauses(query, cut, config.ruleSet().searchRule(query));
	}

	private Node expandClauses(Node query, Node cut, List<Rule> rules) {
		return new Suspend(() -> {
			if (!rules.isEmpty()) {
				var clause = rules.get(0).newClause(query, cut);
				return Tree.ofOr(clause, expandClauses(query, cut, List_.right(rules, 1)));
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

	public ProverCfg config() {
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
