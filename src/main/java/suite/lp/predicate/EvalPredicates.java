package suite.lp.predicate;

import java.util.Objects;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import suite.Suite;
import suite.adt.IdentityKey;
import suite.lp.doer.Cloner;
import suite.lp.doer.Specializer;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.lp.sewing.impl.SewingGeneralizerImpl;
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

	public BuiltinPredicate contains = PredicateUtil.p2((prover, p0, p1) -> new TreeRewriter().contains(p0, p1));

	public BuiltinPredicate compare = (prover, ps) -> {
		Tree tree = (Tree) ps;
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

	public BuiltinPredicate dictKeyValue = PredicateUtil.p3((prover, node, key, value) -> {
		Reference reference = new Reference();
		Dict dict = new Dict();
		dict.map.put(key, reference);
		return prover.bind(reference, value) && prover.bind(node, dict);
	});

	public BuiltinPredicate evalFun = PredicateUtil.fun(n -> Suite.evaluateFun(Suite.fcc(n, true)));

	public BuiltinPredicate evalJs = PredicateUtil.p2((prover, p0, p1) -> {
		String js = Formatter.display(p0);
		Object result;

		try {
			result = engines.apply("js").eval(js);
		} catch (ScriptException ex)

		{
			LogUtil.error(ex);
			return false;
		}

		return prover.bind(new Str(Objects.toString(result, "")), p1);
	});

	public BuiltinPredicate generalize = PredicateUtil.fun(SewingGeneralizerImpl::generalize);

	public BuiltinPredicate graphBind = PredicateUtil.p2((prover, left, right) -> Grapher.bind(left, right, prover.getTrail()));

	public BuiltinPredicate graphGeneralize = PredicateUtil.fun(n -> {
		Grapher grapher = new Grapher();
		grapher.graph(n);
		grapher.generalize();
		return grapher.ungraph();
	});

	public BuiltinPredicate graphReplace = PredicateUtil.p4((prover, from, to, n0, nx) -> {
		Node n = Grapher.replace(from, to, n0);
		return Grapher.bind(n, nx, prover.getTrail());
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

	public BuiltinPredicate length = PredicateUtil.p2((prover, list, length) -> {
		int size = 0;
		Tree tree;

		while ((tree = Tree.decompose(list)) != null) {
			size++;
			list = tree.getRight();
		}

		if (list == Atom.NIL)
			return prover.bind(length, Int.of(size));
		else {
			int size1 = ((Int) length).number;
			Node list1 = Atom.NIL;
			while (0 < size1--)
				list1 = Tree.of(TermOp.AND___, new Reference(), list1);
			return prover.bind(list, list1);
		}
	});

	public BuiltinPredicate let = PredicateUtil.p2((prover, var, expr) -> prover.bind(Int.of(evaluate(expr)), var));

	public BuiltinPredicate notEquals = (prover, ps) -> {
		Tree tree = (Tree) ps;
		return PredicateUtil.tryProve(prover, prover1 -> !prover1.bind(tree.getLeft(), tree.getRight()));
	};

	public BuiltinPredicate randomPredicate = PredicateUtil.fun(n -> Int.of(random.nextInt(((Int) n).number)));

	public BuiltinPredicate replace = PredicateUtil
			.p4((prover, from, to, n0, nx) -> prover.bind(new TreeRewriter().replace(from, to, n0), nx));

	public BuiltinPredicate rewrite = PredicateUtil
			.p4((prover, from, to, n0, nx) -> prover.bind(new TreeRewriter().rewrite(from, to, n0), nx));

	public BuiltinPredicate same = PredicateUtil.p2((prover, p0, p1) -> p0 == p1);

	public BuiltinPredicate specialize = PredicateUtil.fun(new Specializer()::specialize);

	public BuiltinPredicate temp = PredicateUtil.p1((prover, p0) -> prover.bind(p0, Atom.temp()));

	public BuiltinPredicate tree = PredicateUtil.p4((prover, t, l, op, r) -> {
		Tree tree;
		if ((tree = Tree.decompose(t)) != null) {
			Atom oper = Atom.of(tree.getOperator().getName());
			return prover.bind(tree.getLeft(), l) //
					&& prover.bind(oper, op) //
					&& prover.bind(tree.getRight(), r);
		} else if (op instanceof Atom) {
			Operator operator = TermOp.find(((Atom) op).name);
			return prover.bind(t, Tree.of(operator, l, r));
		} else
			return false;
	});

	public int evaluate(Node node) {
		int result;
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
					throw new RuntimeException("Cannot evaluate expression: " + node);
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
					throw new RuntimeException("Cannot evaluate expression: " + node);
				}
			}
		} else if (node instanceof Int)
			result = ((Int) node).number;
		else
			throw new RuntimeException("Cannot evaluate expression: " + node);

		return result;
	}

}
