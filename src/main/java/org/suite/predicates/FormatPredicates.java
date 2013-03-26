package org.suite.predicates;

import java.util.ArrayDeque;
import java.util.Deque;

import org.suite.SuiteUtil;
import org.suite.doer.Formatter;
import org.suite.doer.PrettyPrinter;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.suite.node.Reference;
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

	public static class PrettyPrint implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			PrettyPrinter printer = new PrettyPrinter();
			System.out.println(printer.prettyPrint(ps));
			return true;
		}
	}

	public static class Rpn implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			if (p1 instanceof Str)
				return prover.bind(p0, fromRpn(((Str) p1).getValue()));
			else
				return prover.bind(new Str(toRpn(p0)), p1);
		}

		private static Node fromRpn(String rpn) {
			String elems[] = rpn.split("\n");
			Deque<Node> deque = new ArrayDeque<>();
			int index = elems.length;

			while (index > 0) {
				String elem = elems[--index];
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
					Node left = deque.pop();
					Node right = deque.pop();
					n = Tree.create(op, left, right);
				} else
					throw new RuntimeException("RPN conversion error: " + elem);

				deque.push(n);
			}

			return deque.pop();
		}

		private static String toRpn(Node node) {
			StringBuilder sb = new StringBuilder();
			Deque<Node> deque = new ArrayDeque<>();

			deque.push(node);

			while (!deque.isEmpty()) {
				Node n = deque.pop().finalNode();
				String s;

				if (n instanceof Atom)
					s = "\\" + ((Atom) n).getName();
				else if (n instanceof Int)
					s = "i" + ((Int) n).getNumber();
				else if (n instanceof Reference)
					s = "\\." + ((Reference) n).getId();
				else if (n instanceof Tree) {
					Tree tree = (Tree) n;
					s = "t" + tree.getOperator();
					deque.push(tree.getRight());
					deque.push(tree.getLeft());
				} else
					s = "^" + Formatter.dump(n);

				sb.append(s);
				sb.append('\n');
			}

			return sb.toString();
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

	public static class StringLength implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			Node params[] = Predicate.getParameters(ps, 2);
			Str str = (Str) params[0].finalNode();
			int length = str.getValue().length();
			return prover.bind(params[1], Int.create(length));
		}
	}

	public static class Substring implements SystemPredicate {
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
