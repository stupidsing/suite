package suite.lp.predicate;

import static primal.statics.Fail.fail;

import java.util.HashMap;
import java.util.Objects;
import java.util.Random;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import primal.adt.IdentityKey;
import primal.fp.Funs.Fun;
import primal.os.Log_;
import suite.Suite;
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
import suite.node.io.TermOp;
import suite.node.util.Comparer;
import suite.node.util.Complexity;
import suite.node.util.Cyclic;
import suite.node.util.Rewrite;
import suite.node.util.TreeUtil;
import suite.util.Memoize;

public class EvalPredicates {

	private static Random random = new Random();
	private static Specializer specializer = new Specializer();
	private static Rewrite rw = new Rewrite();

	private Comparer comparer = Comparer.comparer;
	private Fun<String, ScriptEngine> engines = Memoize.fun(new ScriptEngineManager()::getEngineByExtension);

	public BuiltinPredicate bound = PredicateUtil.bool(n -> !(n instanceof Reference));

	public BuiltinPredicate clone = PredicateUtil.fun(n -> new Cloner().clone(n));

	public BuiltinPredicate complexity = PredicateUtil.fun(n -> Int.of(new Complexity().complexity(n)));

	public BuiltinPredicate contains = PredicateUtil.p2((prover, p0, p1) -> rw.contains(p0, p1));

	public BuiltinPredicate compare = (prover, ps) -> {
		var tree = (Tree) ps;
		switch ((TermOp) tree.getOperator()) {
		case LE____:
			return comparer.compare(tree.getLeft(), tree.getRight()) <= 0;
		case LT____:
			return comparer.compare(tree.getLeft(), tree.getRight()) < 0;
		default:
			return fail("unknown comparison");
		}
	};

	public BuiltinPredicate dictKeyValue = PredicateUtil.p3((prover, node, key, value) -> {
		var reference = new Reference();

		var map = new HashMap<Node, Reference>();
		map.put(key, reference);

		var dict = Dict.of(map);
		return prover.bind(reference, value) && prover.bind(node, dict);
	});

	public BuiltinPredicate evalFun = PredicateUtil.fun(n -> Suite.evaluateFun(Suite.fcc(n, true)));

	public BuiltinPredicate evalJs = PredicateUtil.p2((prover, p0, p1) -> {
		var js = Formatter.display(p0);
		Object result;

		try {
			result = engines.apply("js").eval(js);
		} catch (ScriptException ex) {
			Log_.error(ex);
			return false;
		}

		return prover.bind(new Str(Objects.toString(result, "")), p1);
	});

	public BuiltinPredicate generalize = PredicateUtil.fun(SewingGeneralizerImpl::generalize);

	public BuiltinPredicate graphBind = PredicateUtil.p2((prover, left, right) -> Grapher.bind(left, right, prover.getTrail()));

	public BuiltinPredicate graphGeneralize = PredicateUtil.fun(n -> {
		var grapher = new Grapher();
		grapher.graph(n);
		grapher.generalize();
		return grapher.ungraph();
	});

	public BuiltinPredicate graphReplace = PredicateUtil.p4((prover, from, to, n0, nx) -> {
		Node n = Grapher.replace(from, to, n0);
		return Grapher.bind(n, nx, prover.getTrail());
	});

	public BuiltinPredicate graphSpecialize = PredicateUtil.fun(n -> {
		var grapher = new Grapher();
		grapher.graph(n);
		grapher.specialize();
		return grapher.ungraph();
	});

	public BuiltinPredicate hash = PredicateUtil.fun(n -> Int.of(n.hashCode()));

	public BuiltinPredicate hashId = PredicateUtil.fun(n -> Int.of(IdentityKey.of(n).hashCode()));

	public BuiltinPredicate isCyclic = PredicateUtil.bool(n -> new Cyclic().isCyclic(n));

	public BuiltinPredicate length = PredicateUtil.p2((prover, list, length) -> {
		var size = 0;
		Tree tree;

		while ((tree = Tree.decompose(list)) != null) {
			size++;
			list = tree.getRight();
		}

		if (list == Atom.NIL)
			return prover.bind(length, Int.of(size));
		else {
			var size1 = Int.num(length);
			Node list1 = Atom.NIL;
			while (0 < size1--)
				list1 = Tree.ofAnd(new Reference(), list1);
			return prover.bind(list, list1);
		}
	});

	public BuiltinPredicate let = PredicateUtil.p2((prover, var, expr) -> prover.bind(Int.of(TreeUtil.evaluate(expr)), var));

	public BuiltinPredicate notEquals = (prover, ps) -> {
		var tree = (Tree) ps;
		return PredicateUtil.tryProve(prover, prover1 -> !prover1.bind(tree.getLeft(), tree.getRight()));
	};

	public BuiltinPredicate randomPredicate = PredicateUtil.fun(n -> Int.of(random.nextInt(Int.num(n))));

	public BuiltinPredicate replace = PredicateUtil.p4((prover, from, to, n0, nx) -> prover.bind(rw.replace(from, to, n0), nx));

	public BuiltinPredicate rewrite = PredicateUtil.p4((prover, from, to, n0, nx) -> prover.bind(rw.rewrite(from, to, n0), nx));

	public BuiltinPredicate same = PredicateUtil.p2((prover, p0, p1) -> p0 == p1);

	public BuiltinPredicate specialize = PredicateUtil.fun(specializer::specialize);

	public BuiltinPredicate temp = PredicateUtil.p1((prover, p0) -> prover.bind(p0, Atom.temp()));

	public BuiltinPredicate tree = PredicateUtil.p4((prover, t, l, op, r) -> {
		Tree tree;
		if ((tree = Tree.decompose(t)) != null) {
			var oper = Atom.of(tree.getOperator().name_());
			return prover.bind(tree.getLeft(), l) //
					&& prover.bind(oper, op) //
					&& prover.bind(tree.getRight(), r);
		} else if (op instanceof Atom) {
			var operator = TermOp.find(Atom.name(op));
			return prover.bind(t, Tree.of(operator, l, r));
		} else
			return false;
	});

}
