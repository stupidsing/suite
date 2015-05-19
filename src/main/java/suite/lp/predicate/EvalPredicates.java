package suite.lp.predicate;

import java.util.Objects;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import suite.Suite;
import suite.adt.IdentityKey;
import suite.lp.doer.Cloner;
import suite.lp.doer.Prover;
import suite.lp.doer.Specializer;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.lp.sewing.SewingGeneralizer;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Grapher;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.Complexity;
import suite.node.util.Cyclic;
import suite.node.util.TreeRewriter;
import suite.os.LogUtil;
import suite.util.FunUtil.Fun;
import suite.util.Memoize;

public class EvalPredicates {

	private Atom AND = Atom.of("and");
	private Atom OR_ = Atom.of("or");
	private Atom SHL = Atom.of("shl");
	private Atom SHR = Atom.of("shr");
	private Comparer comparer = Comparer.comparer;
	private Fun<String, ScriptEngine> engines = Memoize.byInput(new ScriptEngineManager()::getEngineByExtension);

	private static Random random = new Random();

	public BuiltinPredicate bound = PredicateUtil.bool(n -> !(n instanceof Reference));

	public BuiltinPredicate clone = PredicateUtil.fun(n -> new Cloner().clone(n));

	public BuiltinPredicate complexity = PredicateUtil.fun(n -> Int.of(new Complexity().complexity(n)));

	public BuiltinPredicate contains = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		return new TreeRewriter().contains(params[0], params[1]);
	};

	public BuiltinPredicate compare = (prover, ps) -> {
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

	public BuiltinPredicate dictKeyValue = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 3);
		Node node = params[0].finalNode();
		Node key = params[1];
		Node value = params[2];

		Reference reference = new Reference();
		Dict dict = new Dict();
		dict.map.put(key, reference);
		return prover.bind(reference, value) && prover.bind(node, dict);
	};

	public BuiltinPredicate evalFun = PredicateUtil.fun(n -> Suite.evaluateFun(Suite.fcc(n, true)));

	public BuiltinPredicate evalJs = (prover, ps) -> {
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

	public BuiltinPredicate generalize = PredicateUtil.fun(SewingGeneralizer::generalize);

	public BuiltinPredicate graphGeneralize = PredicateUtil.fun(n -> {
		Grapher grapher = new Grapher();
		grapher.graph(n);
		grapher.generalize();
		return grapher.ungraph();
	});

	public BuiltinPredicate graphSpecialize = PredicateUtil.fun(n -> {
		Grapher grapher = new Grapher();
		grapher.graph(n);
		grapher.specialize();
		return grapher.ungraph();
	});

	public BuiltinPredicate hash = PredicateUtil.fun(n -> Int.of(n.hashCode()));

	public BuiltinPredicate hashId = PredicateUtil.fun(n -> Int.of(IdentityKey.of(n).hashCode()));

	public BuiltinPredicate isCyclic = PredicateUtil.bool(n -> new Cyclic().isCyclic(n));

	public BuiltinPredicate length = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node list = params[0];
		int size = 0;
		Tree tree;

		while ((tree = Tree.decompose(list)) != null) {
			size++;
			list = tree.getRight();
		}

		if (list.finalNode() == Atom.NIL)
			return prover.bind(params[1], Int.of(size));

		int size1 = ((Int) params[1].finalNode()).number;
		Node list1 = Atom.NIL;
		while (size1-- > 0)
			list1 = Tree.of(TermOp.AND___, new Reference(), list1);
		return prover.bind(list, list1);
	};

	public BuiltinPredicate let = new BuiltinPredicate() {
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
				result = ((Int) node).number;
			else
				throw new RuntimeException("Cannot evaluate expression");

			return result;
		}
	};

	public BuiltinPredicate notEquals = (prover, ps) -> {
		Tree tree = (Tree) ps;
		Prover prover1 = new Prover(prover);
		boolean result = prover1.bind(tree.getLeft(), tree.getRight());

		if (result) {
			prover1.undoAllBinds();
			return false;
		} else
			return true;
	};

	public BuiltinPredicate randomPredicate = PredicateUtil.fun(n -> Int.of(random.nextInt(((Int) n).number)));

	public BuiltinPredicate replace = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 4);
		return prover.bind(new TreeRewriter().replace(params[0], params[1], params[2]), params[3]);
	};

	public BuiltinPredicate rewrite = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 4);
		return prover.bind(new TreeRewriter().rewrite(params[0], params[1], params[2]), params[3]);
	};

	public BuiltinPredicate same = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		return params[0].finalNode() == params[1].finalNode();
	};

	public BuiltinPredicate specialize = PredicateUtil.fun(new Specializer()::specialize);

	public BuiltinPredicate temp = (prover, ps) -> prover.bind(ps, Atom.temp());

	public BuiltinPredicate tree = (prover, ps) -> {
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
			Operator operator = TermOp.find(((Atom) p2).name);
			return prover.bind(p, Tree.of(operator, p1, p3));
		} else
			return false;
	};

}
