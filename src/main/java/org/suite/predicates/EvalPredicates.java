package org.suite.predicates;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.parser.Operator;
import org.suite.doer.Cloner;
import org.suite.doer.Comparer;
import org.suite.doer.Formatter;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
import org.suite.node.Str;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates.SystemPredicate;
import org.util.Util;

public class EvalPredicates {

	private static Comparer comparer = Comparer.comparer;

	public static class Bound implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return !(ps.finalNode() instanceof Reference);
		}
	}

	public static class Clone implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			return prover.bind(new Cloner().clone(params[0]), params[1]);
		}
	}

	public static class Compare implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
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
		}
	}

	public static class EvalJs implements SystemPredicate {
		private final ScriptEngine engine = new ScriptEngineManager()
				.getEngineByExtension("js");

		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			String js = Formatter.display(params[0]);
			Object result;

			try {
				result = engine.eval(js);
			} catch (ScriptException ex) {
				log.error(js, ex);
				return false;
			}

			String str = result != null ? result.toString() : "";
			return prover.bind(new Str(str), params[1]);
		}
	}

	public static class Generalize implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Generalizer generalizer = new Generalizer();
			return prover.bind(generalizer.generalize(params[0]), params[1]);
		}
	}

	public static class GeneralizeWithPrefix implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 3);
			Generalizer generalizer = new Generalizer();
			generalizer.setVariablePrefix(Formatter.display(params[1]));
			return prover.bind(generalizer.generalize(params[0]), params[2]);
		}
	}

	public static class Hash implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			return prover.bind(Int.create(params[0].hashCode()), params[1]);
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
			node = node.finalNode();
			Tree tree = Tree.decompose(node);

			if (tree != null) {
				int a = evaluate(tree.getLeft()), b = evaluate(tree.getRight());

				switch ((TermOp) tree.getOperator()) {
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
				}
			} else if (node instanceof Int)
				result = ((Int) node).getNumber();
			else
				throw new RuntimeException("Unable to evaluate expression");

			return result;
		}
	}

	private static final Map<Node, Node> store = new TreeMap<Node, Node>();

	public static class MapRetrieve implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node value = store.get(params[0]);
			if (value == null)
				store.put(params[0], value = new Reference());
			return prover.bind(value, params[1]);
		}
	}

	public static class MapErase implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return store.remove(ps) != null;
		}
	}

	public static class NotEquals implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Tree tree = (Tree) ps;
			Prover prover1 = new Prover(prover);
			boolean result = prover1.bind(tree.getLeft(), tree.getRight());

			if (result) {
				prover1.undoAllBinds();
				return false;
			} else
				return true;
		}
	}

	public static class Nth implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 4);
			String name = ((Str) params[0].finalNode()).getValue();
			int length = name.length();
			Node p1 = params[1].finalNode(), p2 = params[2].finalNode();

			if (p1 instanceof Int && p2 instanceof Int) {
				int m = ((Int) p1).getNumber(), n = ((Int) p2).getNumber();

				while (m < 0)
					m += length;
				while (n <= 0)
					n += length;

				n = Math.min(n, length);

				return prover.bind(params[3] //
						, new Str(name.substring(m, n)));
			} else
				throw new RuntimeException("Invalid call pattern");
		}
	}

	public static class RandomPredicate implements SystemPredicate {
		private static final java.util.Random random = new Random();

		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Int p0 = (Int) params[0].finalNode();
			int randomNumber = random.nextInt(p0.getNumber());
			return prover.bind(params[1], Int.create(randomNumber));
		}
	}

	public static class Same implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			return params[0].finalNode() == params[1].finalNode();
		}
	}

	public static class Specialize implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			return prover.bind(specialize(params[0]), params[1]);
		}

		private static Node specialize(Node node) {
			node = node.finalNode();

			if (node instanceof Reference) {
				Reference ref = (Reference) node;
				node = Atom.create(Generalizer.DEFAULTPREFIX + ref.getId());
			} else if (node instanceof Tree) {
				Tree tree = (Tree) node;
				Node left = tree.getLeft(), right = tree.getRight();
				Node left1 = specialize(left), right1 = specialize(right);
				if (left != left1 || right != right1)
					node = new Tree(tree.getOperator(), left1, right1);
			}

			return node;
		}
	}

	public static class Temp implements SystemPredicate {
		private static AtomicInteger counter = new AtomicInteger();

		public boolean prove(Prover prover, Node ps) {
			int n = counter.getAndIncrement();
			return prover.bind(ps, Atom.create("$$T" + n));
		}
	}

	public static class TreePredicate implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 4);
			Node p = params[0].finalNode();
			Node p2 = params[2].finalNode();

			if (p instanceof Tree) {
				Tree tree = (Tree) p;
				Atom oper = Atom.create(tree.getOperator().getName());
				return prover.bind(tree.getLeft(), params[1])
						&& prover.bind(oper, p2)
						&& prover.bind(tree.getRight(), params[3]);
			} else if (p2 instanceof Atom) {
				Operator operator = TermOp.find(((Atom) p2).getName());
				return prover.bind(p, new Tree(operator, params[1], params[3]));
			} else
				return false;
		}
	}

	private static Log log = LogFactory.getLog(Util.currentClass());

}
