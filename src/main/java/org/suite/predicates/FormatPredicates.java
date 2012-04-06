package org.suite.predicates;

import java.util.ArrayList;
import java.util.List;

import org.suite.SuiteUtil;
import org.suite.doer.Formatter;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Str;
import org.suite.node.Tree;
import org.suite.predicates.SystemPredicates.SystemPredicate;

public class FormatPredicates {

	public static class Concat implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node node = ps;
			StringBuilder sb = new StringBuilder();
			Tree tree;

			while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
				sb.append(Formatter.display(tree.getLeft()));
				node = tree.getRight();
			}

			return prover.bind(new Str(sb.toString()), node);
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

	public static class Parse implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			return prover.bind(SuiteUtil.parse(Formatter.display(p0)), p1);
		}
	}

	public static class Rpn implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			if (p1 instanceof Str)
				return prover.bind(p0, fromRpn(((Str) p1).getValue()));
			else {
				StringBuilder sb = new StringBuilder();
				toRpn(p0, sb);
				return prover.bind(new Str(sb.toString()), p1);
			}
		}

		private static Node fromRpn(String rpn) {
			String elems[] = rpn.split("\n");
			List<Node> stack = new ArrayList<Node>();

			for (String elem : elems) {
				if (elem.isEmpty())
					continue;

				char type = elem.charAt(0);
				String s = elem.substring(1);
				Node n;

				if (type == '\\')
					n = Atom.create(s);
				else if (type == '^')
					n = SuiteUtil.parse(s);
				else if (type == 'i')
					n = Int.create(Integer.valueOf(s));
				else if (type == 't') {
					TermOp op = TermOp.valueOf(s);
					int l = stack.size();
					Node right = stack.remove(l - 1);
					Node left = stack.remove(l - 2);
					n = new Tree(op, left, right);
				} else
					throw new RuntimeException("RPN conversion error: " + elem);

				stack.add(n);
			}

			return stack.get(0);
		}

		private static void toRpn(Node node, StringBuilder sb) {
			String s;
			node = node.finalNode();

			if (node instanceof Atom)
				s = "\\" + ((Atom) node).getName();
			else if (node instanceof Int)
				s = "i" + ((Int) node).getNumber();
			else if (node instanceof Tree) {
				Tree tree = (Tree) node;
				toRpn(tree.getLeft(), sb);
				toRpn(tree.getRight(), sb);
				s = "t" + tree.getOperator();
			} else
				s = "^" + Formatter.dump(node);

			sb.append(s);
			sb.append('\n');
		}
	}

	public static class StartsWith implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();

			return p0 instanceof Atom && p1 instanceof Atom
					&& ((Atom) p0).getName().startsWith(((Atom) p1).getName());
		}
	}

	public static class ToAtom implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			return prover.bind(p1, Atom.create(Formatter.display(p0)));
		}
	}

	public static class ToDumpString implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			return prover.bind(p1, new Str(Formatter.dump(p0)));
		}
	}

	public static class ToInt implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			return prover.bind(p1, Int.create(Formatter.display(p0).charAt(0)));
		}
	}

	public static class ToString implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			return prover.bind(p1, new Str(Formatter.display(p0)));
		}
	}

	public static class Trim implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			return prover.bind(p1, new Str(Formatter.display(p0).trim()));
		}
	}

}
