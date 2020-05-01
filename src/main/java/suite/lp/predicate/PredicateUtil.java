package suite.lp.predicate;

import primal.fp.Funs.Iterate;
import primal.fp.Funs.Sink;
import suite.lp.doer.Prover;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;

import java.util.function.Predicate;

public class PredicateUtil {

	public interface BuiltinPredicate {
		public boolean prove(Prover prover, Node ps);
	}

	public interface PredicateP1 {
		public boolean prove(Prover prover, Node p0);
	}

	public interface PredicateP2 {
		public boolean prove(Prover prover, Node p0, Node p1);
	}

	public interface PredicateP3 {
		public boolean prove(Prover prover, Node p0, Node p1, Node p2);
	}

	public interface PredicateP4 {
		public boolean prove(Prover prover, Node p0, Node p1, Node p2, Node p3);
	}

	public interface PredicatePs {
		public boolean prove(Prover prover, Node[] ps);
	}

	public static BuiltinPredicate run(Runnable runnable) {
		return (prover, ps) -> {
			runnable.run();
			return true;
		};
	}

	public static BuiltinPredicate sink(Sink<Node> fun) {
		return p1((prover, p0) -> {
			fun.f(p0);
			return true;
		});
	}

	public static BuiltinPredicate bool(Predicate<Node> fun) {
		return p1((prover, p0) -> fun.test(p0));
	}

	public static BuiltinPredicate fun(Iterate<Node> fun) {
		return p2((prover, p0, p1) -> prover.bind(p1, fun.apply(p0)));
	}

	public static BuiltinPredicate p1(PredicateP1 pred) {
		return (prover, t) -> {
			return pred.prove(prover, t);
		};
	}

	public static BuiltinPredicate p2(PredicateP2 pred) {
		return (prover, t) -> {
			var t0 = Tree.decompose(t, TermOp.TUPLE_);
			return pred.prove(prover, t0.getLeft(), t0.getRight());
		};
	}

	public static BuiltinPredicate p3(PredicateP3 pred) {
		return (prover, t) -> {
			var t0 = Tree.decompose(t, TermOp.TUPLE_);
			var t1 = Tree.decompose(t0.getRight(), TermOp.TUPLE_);
			return pred.prove(prover, t0.getLeft(), t1.getLeft(), t1.getRight());
		};
	}

	public static BuiltinPredicate p4(PredicateP4 pred) {
		return (prover, t) -> {
			var t0 = Tree.decompose(t, TermOp.TUPLE_);
			var t1 = Tree.decompose(t0.getRight(), TermOp.TUPLE_);
			var t2 = Tree.decompose(t1.getRight(), TermOp.TUPLE_);
			return pred.prove(prover, t0.getLeft(), t1.getLeft(), t2.getLeft(), t2.getRight());
		};
	}

	public static BuiltinPredicate ps(PredicatePs pred) {
		return (prover, t) -> pred.prove(prover, TreeUtil.elements(t, TreeUtil.nElements(t)));
	}

	public static boolean tryProve(Prover prover, Predicate<Prover> source) {
		var prover1 = new Prover(prover);
		var b = false;
		try {
			b = source.test(prover1);
		} finally {
			if (!b) // roll back bindings if overall goal is failed
				prover1.unwindAll();
		}
		return b;
	}

}
