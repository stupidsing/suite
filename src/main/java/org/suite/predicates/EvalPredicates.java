package org.suite.predicates;

import org.suite.doer.Comparer;
import org.suite.doer.Prover;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Str;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates.SystemPredicate;

public class EvalPredicates {

	private static Comparer comparer = new Comparer();

	public static class Bound implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return !(ps.finalNode() instanceof Reference);
		}
	}

	public static class Compare implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Tree tree = (Tree) ps;
			switch (tree.getOperator()) {
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
		}
	}

	public static class Let implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			int result = evaluate(params[1]);
			return prover.bind(Int.create(result), params[0]);
		}

		public int evaluate(Node node) {
			int result = 0;

			Tree tree = Tree.decompose(node);
			if (tree != null) {
				int a = evaluate(tree.getLeft()), b = evaluate(tree.getRight());

				switch (tree.getOperator()) {
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
				}
			} else if (node instanceof Int)
				result = ((Int) node).getNumber();

			return result;
		}
	}

	public static class IsAtom implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return ps.finalNode() instanceof Atom;
		}
	}

	public static class IsInt implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return ps.finalNode() instanceof Int;
		}
	}

	public static class IsString implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return ps.finalNode() instanceof Str;
		}
	}

	public static class IsTree implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return ps.finalNode() instanceof Tree;
		}
	}

}
