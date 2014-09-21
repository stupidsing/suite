package suite.lp.predicate;

import java.util.Objects;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import suite.Suite;
import suite.lp.doer.Cloner;
import suite.lp.doer.Prover;
import suite.lp.doer.Specializer;
import suite.lp.predicate.PredicateUtil.SystemPredicate;
import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.TreeIntern;
import suite.node.io.Formatter;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.Complexity;
import suite.node.util.Cyclic;
import suite.node.util.IdentityKey;
import suite.node.util.Rewriter;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;
import suite.util.Memoize;

public class EvalPredicates {

	private Atom AND = Atom.of("and");
	private Atom OR_ = Atom.of("or");
	private Atom SHL = Atom.of("shl");
	private Atom SHR = Atom.of("shr");
	private Comparer comparer = Comparer.comparer;
	private Fun<String, ScriptEngine> engines = Memoize.byInput(new ScriptEngineManager()::getEngineByExtension);

	private static Random random = new Random();

	public SystemPredicate bound = PredicateUtil.boolPredicate(n -> !(n instanceof Reference));

	public SystemPredicate clone = PredicateUtil.funPredicate(n -> new Cloner().clone(n));

	public SystemPredicate complexity = PredicateUtil.funPredicate(n -> Int.of(new Complexity().complexity(n)));

	public SystemPredicate contains = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		return new Rewriter(params[0]).contains(params[1]);
	};

	public SystemPredicate compare = (prover, ps) -> {
		Tree tree = (Tree) ps.finalNode();
		switch ((TermOp) tree.getOperator()) {
		case LE____:
			return comparer.compare(tree.getLeft(), tree.getRight()) <= 0;
		case LT____:
			return comparer.compare(tree.getLeft(), tree.getRight()) < 0;
		case GE____:
			return comparer.compare(tree.getLeft(), tree.getRight()) >= 0;
		case GT____:
			return comparer.compare(tree.getLeft(), tree.getRight()) > 0;
		default:
			throw new RuntimeException("Unknown comparison");
		}
	};

	public SystemPredicate evalFun = PredicateUtil.funPredicate(n -> Suite.evaluateFun(Suite.fcc(n, true)));

	public SystemPredicate evalJs = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		String js = Formatter.display(params[0]);
		Object result;

		try {
			result = engines.apply("js").eval(js);
		} catch (ScriptException ex) {
			LogUtil.error(ex);
			return false;
		}

		String str = Objects.toString(result, "");
		return prover.bind(new Str(str), params[1]);
	};

	public SystemPredicate generalize = PredicateUtil.funPredicate(SewingGeneralizer::generalize);

	public SystemPredicate hash = PredicateUtil.funPredicate(n -> Int.of(n.hashCode()));

	public SystemPredicate hashId = PredicateUtil.funPredicate(n -> Int.of(new IdentityKey(n).hashCode()));

	public SystemPredicate isCyclic = PredicateUtil.boolPredicate(n -> new Cyclic().isCyclic(n));

	public SystemPredicate let = new SystemPredicate() {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			int result = evaluate(params[1]);
			return prover.bind(Int.of(result), params[0]);
		}

		public int evaluate(Node node) {
			int result;
			node = node.finalNode();
			Tree tree = Tree.decompose(node);

			if (tree != null) {
				TermOp op = (TermOp) tree.getOperator();

				if (op == TermOp.TUPLE_) {
					Tree rightTree = Tree.decompose(tree.getRight());
					Node op1 = rightTree.getLeft();
					int a = evaluate(tree.getLeft()), b = evaluate(rightTree.getRight());
					if (op1 == AND)
						result = a & b;
					else if (op1 == OR_)
						result = a | b;
					else if (op1 == SHL)
						result = a << b;
					else if (op1 == SHR)
						result = a >> b;
					else
						throw new RuntimeException("Cannot evaluate expression");
				} else {
					int a = evaluate(tree.getLeft()), b = evaluate(tree.getRight());
					switch (op) {
					case PLUS__:
						result = a + b;
						break;
					case MINUS_:
						result = a - b;
						break;
					case MULT__:
						result = a * b;
						break;
					case DIVIDE:
						result = a / b;
						break;
					case MODULO:
						result = a % b;
						break;
					case POWER_:
						result = (int) Math.pow(a, b);
						break;
					default:
						throw new RuntimeException("Cannot evaluate expression");
					}
				}
			} else if (node instanceof Int)
				result = ((Int) node).getNumber();
			else
				throw new RuntimeException("Cannot evaluate expression");

			return result;
		}
	};

	public SystemPredicate notEquals = (prover, ps) -> {
		Tree tree = (Tree) ps;
		Prover prover1 = new Prover(prover);
		boolean result = prover1.bind(tree.getLeft(), tree.getRight());

		if (result) {
			prover1.undoAllBinds();
			return false;
		} else
			return true;
	};

	public SystemPredicate randomPredicate = PredicateUtil.funPredicate(n -> Int.of(random.nextInt(((Int) n).getNumber())));

	public SystemPredicate replace = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 4);
		return prover.bind(new Rewriter(params[0], params[1]).replace(params[2]), params[3]);
	};

	public SystemPredicate rewrite = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 4);
		return prover.bind(new Rewriter(params[0], params[1]).rewrite(params[2]), params[3]);
	};

	public SystemPredicate same = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		return params[0].finalNode() == params[1].finalNode();
	};

	public SystemPredicate specialize = PredicateUtil.funPredicate(n -> new Specializer().specialize(n));

	public SystemPredicate temp = (prover, ps) -> prover.bind(ps, Atom.temp());

	public SystemPredicate tree = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 4);
		Node p = params[0].finalNode();
		Node p1 = params[1];
		Node p2 = params[2].finalNode();
		Node p3 = params[3];
		Tree tree;

		if ((tree = Tree.decompose(p)) != null) {
			Atom oper = Atom.of(tree.getOperator().getName());
			return prover.bind(tree.getLeft(), p1) //
					&& prover.bind(oper, p2) //
					&& prover.bind(tree.getRight(), p3);
		} else if (p2 instanceof Atom) {
			Operator operator = TermOp.find(((Atom) p2).getName());
			return prover.bind(p, Tree.of(operator, p1, p3));
		} else
			return false;
	};

	public SystemPredicate treeIntern = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 4);
		Node p = params[0];
		Node p1 = params[1];
		Node p2 = params[2].finalNode();
		Node p3 = params[3];

		Operator operator = TermOp.find(((Atom) p2).getName());
		return prover.bind(p, TreeIntern.of(operator, p1, p3));
	};

}
