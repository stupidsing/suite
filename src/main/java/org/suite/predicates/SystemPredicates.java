package org.suite.predicates;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.parser.Operator;
import org.suite.doer.Formatter;
import org.suite.doer.Prover;
import org.suite.doer.Prover.Backtracks;
import org.suite.doer.TermParser.TermOp;
import org.suite.kb.Prototype;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSet.Rule;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Station;
import org.suite.node.Tree;

public class SystemPredicates {

	public interface SystemPredicate {
		public boolean prove(Prover prover, Node parameter);
	}

	private Map<Node, SystemPredicate> predicates = new HashMap<Node, SystemPredicate>();

	private Prover prover;

	public SystemPredicates(Prover prover) {
		this.prover = prover;

		addPredicate("find.all", new FindAll());
		addPredicate("list", new ListPredicates());
		addPredicate("not", new Not());
		addPredicate("once", new Once());
		addPredicate("get", new StoreGet());
		addPredicate("put", new StorePut());
		addPredicate("temporary", new Temporary());

		addPredicate("bound", new EvalPredicates.Bound());
		addPredicate("eval.js", new EvalPredicates.EvalJs());
		addPredicate(TermOp.LE____, new EvalPredicates.Compare());
		addPredicate(TermOp.LT____, new EvalPredicates.Compare());
		addPredicate(TermOp.GE____, new EvalPredicates.Compare());
		addPredicate(TermOp.GT____, new EvalPredicates.Compare());
		addPredicate("let", new EvalPredicates.Let());
		addPredicate("is.atom", new EvalPredicates.IsAtom());
		addPredicate("is.int", new EvalPredicates.IsInt());
		addPredicate("is.string", new EvalPredicates.IsString());
		addPredicate("is.tree", new EvalPredicates.IsTree());

		addPredicate("dump", new IoPredicates.Dump());
		addPredicate("exec", new IoPredicates.Exec());
		addPredicate("nl", new IoPredicates.Nl());
		addPredicate("write", new IoPredicates.Write());

		addPredicate("import", new ImportPredicates.Import());
		addPredicate("assert", new ImportPredicates.Assert());
		addPredicate("retract", new ImportPredicates.Retract());
	}

	public Boolean call(Node query) {
		SystemPredicate predicate;
		Node pass = query;

		if ((predicate = predicates.get(query)) != null)
			pass = Atom.nil;
		else {
			Tree tree = Tree.decompose(query);
			if (tree != null)
				if (tree.getOperator() != TermOp.SEP___)
					predicate = predicates.get(new Tree(tree.getOperator()));
				else {
					predicate = predicates.get(tree.getLeft());
					pass = tree.getRight();
				}
		}

		return (predicate != null) ? predicate.prove(prover, pass) : null;
	}

	private void addPredicate(Operator operator, SystemPredicate pred) {
		predicates.put(new Tree(operator), pred);
	}

	private void addPredicate(String name, SystemPredicate pred) {
		predicates.put(Atom.create(name), pred);
	}

	private class FindAll implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Stack<Node> stack = new Stack<Node>();
			final Node params[] = Predicate.getParameters(ps, 3);

			Tree subGoal = new Tree(TermOp.AND___, params[1], new Station() {
				public boolean run(Backtracks backtracks) {
					stack.push(params[0].finalNode());
					return false;
				}
			});
			Prover subProver = new Prover(prover);
			subProver.prove(subGoal);
			subProver.undoAllBinds();

			Node result = Atom.nil;
			while (!stack.isEmpty())
				result = new Tree(TermOp.AND___, stack.pop(), result);

			return prover.bind(params[2], result);
		}
	}

	private class ListPredicates implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Prototype proto = null;
			if (ps != Atom.nil)
				proto = Prototype.get(ps);

			for (Rule rule : prover.getRuleSet().getRules()) {
				Prototype p1 = Prototype.get(rule);
				if (proto == null || proto.equals(p1)) {
					String s = Formatter.dump(RuleSet.formClause(rule));
					System.out.println(s + " #");
				}
			}

			return true;
		}
	}

	private class Not implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return !prover.prove(ps);
		}
	}

	private class Once implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return prover.prove(ps);
		}
	}

	private static Map<Node, Node> store = new HashMap<Node, Node>();

	private class StoreGet implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node value = store.get(params[0]);
			return value != null ? prover.bind(value, params[1]) : false;
		}
	}

	private class StorePut implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			store.put(params[0], params[1]);
			return true;
		}
	}

	private static AtomicInteger count = new AtomicInteger();

	private class Temporary implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 1);
			int n = count.getAndIncrement();
			return prover.bind(params[0], Atom.create("TEMP" + n));
		}
	}

}
