package suite.lp.predicate;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import suite.Suite;
import suite.lp.doer.Cloner;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.doer.Specializer;
import suite.lp.predicate.SystemPredicates.SystemPredicate;
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
import suite.node.util.Rewriter;
import suite.util.LogUtil;

public class EvalPredicates {

	private static Atom AND = Atom.of("and");
	private static Atom OR_ = Atom.of("or");
	private static Atom SHL = Atom.of("shl");
	private static Atom SHR = Atom.of("shr");
	private static Comparer comparer = Comparer.comparer;

	public static class Bound implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return !(ps.finalNode() instanceof Reference);
		}
	}

	public static class Clone implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			return prover.bind(new Cloner().clone(params[0]), params[1]);
		}
	}

	public static class ComplexityPredicate implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			return prover.bind(Int.of(new Complexity().complexity(params[0])), params[1]);
		}
	}

	public static class Contains implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			return new Rewriter(params[0]).contains(params[1]);
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

	public static class EvalFun implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			return prover.bind(Suite.evaluateFun(Suite.fcc(params[0], true)), params[1]);
		}
	}

	public static class EvalJs implements SystemPredicate {
		private static ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");

		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			String js = Formatter.display(params[0]);
			Object result;

			try {
				result = engine.eval(js);
			} catch (ScriptException ex) {
				LogUtil.error(ex);
				return false;
			}

			String str = Objects.toString(result, "");
			return prover.bind(new Str(str), params[1]);
		}
	}

	public static class Generalize implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			Generalizer generalizer = new Generalizer();
			return prover.bind(generalizer.generalize(params[0]), params[1]);
		}
	}

	public static class Hash implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			return prover.bind(Int.of(params[0].hashCode()), params[1]);
		}
	}

	public static class IsCyclic implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			return new Cyclic().isCyclic(ps);
		}
	}

	public static class Let implements SystemPredicate {
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

	public static class RandomPredicate implements SystemPredicate {
		private static java.util.Random random = new Random();

		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			Int p0 = (Int) params[0].finalNode();
			int randomNumber = random.nextInt(p0.getNumber());
			return prover.bind(params[1], Int.of(randomNumber));
		}
	}

	public static class Replace implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 4);
			return prover.bind(new Rewriter(params[0], params[1]).replace(params[2]), params[3]);
		}
	}

	public static class Rewrite implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 4);
			return prover.bind(new Rewriter(params[0], params[1]).rewrite(params[2]), params[3]);
		}
	}

	public static class Same implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			return params[0].finalNode() == params[1].finalNode();
		}
	}

	public static class Specialize implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 2);
			return prover.bind(new Specializer().specialize(params[0]), params[1]);
		}
	}

	public static class Temp implements SystemPredicate {
		private static AtomicInteger counter = new AtomicInteger();

		public boolean prove(Prover prover, Node ps) {
			int n = counter.getAndIncrement();
			return prover.bind(ps, Atom.of("temp$$" + n));
		}
	}

	public static class TreePredicate implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
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
				throw new RuntimeException("Unknown input pattern");
		}
	}

	public static class TreeInternPredicate implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Tree.getParameters(ps, 4);
			Node p = params[0];
			Node p1 = params[1];
			Node p2 = params[2].finalNode();
			Node p3 = params[3];

			Operator operator = TermOp.find(((Atom) p2).getName());
			return prover.bind(p, TreeIntern.of(operator, p1, p3));
		}
	}

}
