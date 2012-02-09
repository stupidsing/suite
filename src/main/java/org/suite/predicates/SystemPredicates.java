package org.suite.predicates;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.parser.Operator;
import org.suite.Context;
import org.suite.Singleton;
import org.suite.doer.Cloner;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Station;
import org.suite.node.Tree;

public class SystemPredicates {

	public interface SystemPredicate {
		public boolean prove(Prover prover, Node parameter);
	}

	private Map<String, SystemPredicate> predicates = new HashMap<String, SystemPredicate>();

	private Prover prover;

	public SystemPredicates(Prover prover) {
		this.prover = prover;

		addPredicate("find.all", new FindAll());
		addPredicate("not", new Not());
		addPredicate("once", new Once());
		addPredicate("temporary", new Temporary());

		addPredicate("bound", new EvalPredicates.Bound());
		addPredicate("clone", new EvalPredicates.Clone());
		addPredicate("eval.js", new EvalPredicates.EvalJs());
		addPredicate("eval.fun", new EvalPredicates.EvalFunctional());
		addPredicate(TermOp.LE____, new EvalPredicates.Compare());
		addPredicate(TermOp.LT____, new EvalPredicates.Compare());
		addPredicate(TermOp.NOTEQ_, new EvalPredicates.NotEquals());
		addPredicate(TermOp.GE____, new EvalPredicates.Compare());
		addPredicate(TermOp.GT____, new EvalPredicates.Compare());
		addPredicate("concat", new EvalPredicates.Concat());
		addPredicate("generalize", new EvalPredicates.Generalize());
		addPredicate("generalize.prefix",
				new EvalPredicates.GeneralizeWithPrefix());
		addPredicate("hash", new EvalPredicates.Hash());
		addPredicate("let", new EvalPredicates.Let());
		addPredicate("is.atom", new EvalPredicates.IsAtom());
		addPredicate("is.int", new EvalPredicates.IsInt());
		addPredicate("is.string", new EvalPredicates.IsString());
		addPredicate("is.tree", new EvalPredicates.IsTree());
		addPredicate("map.retrieve", new EvalPredicates.MapRetrieve());
		addPredicate("map.erase", new EvalPredicates.MapErase());
		addPredicate("nth", new EvalPredicates.Nth());
		addPredicate("parse", new EvalPredicates.Parse());
		addPredicate("random", new EvalPredicates.RandomPredicate());
		addPredicate("same", new EvalPredicates.Same());
		addPredicate("starts.with", new EvalPredicates.StartsWith());
		addPredicate("temp", new EvalPredicates.Temp());
		addPredicate("to.atom", new EvalPredicates.ToAtom());
		addPredicate("to.string", new EvalPredicates.ToString());
		addPredicate("tree", new EvalPredicates.TreePredicate());
		addPredicate("trim", new EvalPredicates.Trim());

		addPredicate("dump", new IoPredicates.Dump());
		addPredicate("exec", new IoPredicates.Exec());
		addPredicate("nl", new IoPredicates.Nl());
		addPredicate("write", new IoPredicates.Write());

		addPredicate("assert", new RuleSetPredicates.Assertz());
		addPredicate("asserta", new RuleSetPredicates.Asserta());
		addPredicate("clear", new RuleSetPredicates.Clear());
		addPredicate("import", new RuleSetPredicates.Import());
		addPredicate("list", new RuleSetPredicates.ListPredicates());
		addPredicate("retract", new RuleSetPredicates.Retract());
		addPredicate("rules", new RuleSetPredicates.GetAllRules());
		addPredicate("with", new RuleSetPredicates.With());
	}

	public Boolean call(Node query) {
		SystemPredicate predicate = null;
		Tree tree;
		String name = null;
		Node pass = query;

		if (query instanceof Atom) {
			name = ((Atom) query).getName();
			pass = Atom.nil;
		} else if ((tree = Tree.decompose(query)) != null)
			if (tree.getOperator() != TermOp.SEP___)
				name = tree.getOperator().getName();
			else {
				Node left = tree.getLeft();

				if (left instanceof Atom) {
					name = ((Atom) left).getName();
					pass = tree.getRight();
				}
			}

		predicate = name != null ? predicates.get(name) : null;
		return predicate != null ? predicate.prove(prover, pass) : null;
	}

	private void addPredicate(Operator operator, SystemPredicate pred) {
		predicates.put(operator.getName(), pred);
	}

	private void addPredicate(String name, SystemPredicate pred) {
		predicates.put(name, pred);
	}

	private class FindAll implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Stack<Node> stack = new Stack<Node>();
			final Node params[] = Predicate.getParameters(ps, 3);

			Tree subGoal = new Tree(TermOp.AND___, params[1], new Station() {
				public boolean run() {
					stack.push(new Cloner().clone(params[0]));
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

	private class Not implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return !new Prover(prover).prove(ps);
		}
	}

	private class Once implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return new Prover(prover).prove(ps);
		}
	}

	private class Temporary implements SystemPredicate {
		private final AtomicInteger counter = new AtomicInteger();

		public boolean prove(Prover prover, Node ps) {
			Context hiddenContext = Singleton.get().getHiddenContext();
			String name = "TEMP" + counter.getAndIncrement();
			return prover.bind(ps, Atom.create(hiddenContext, name));
		}
	}

}
