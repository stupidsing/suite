package suite.lp.predicates;

import suite.lp.Suite;
import suite.lp.doer.Formatter;
import suite.lp.doer.PrettyPrinter;
import suite.lp.doer.Prover;
import suite.lp.doer.ReversePolish;
import suite.lp.doer.TermParser.TermOp;
import suite.lp.node.Atom;
import suite.lp.node.Int;
import suite.lp.node.Node;
import suite.lp.node.Str;
import suite.lp.node.Tree;
import suite.lp.predicates.SystemPredicates.SystemPredicate;

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
			return prover.bind(Suite.parse(Formatter.display(p0)), p1);
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
		private ReversePolish rpn = new ReversePolish();

		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			if (p1 instanceof Str)
				return prover.bind(p0, rpn.fromRpn(((Str) p1).getValue()));
			else
				return prover.bind(new Str(rpn.toRpn(p0)), p1);
		}
	}

	public static class StartsWith implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();

			return p0 instanceof Atom && p1 instanceof Atom //
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

	public static class Treeize implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			final Node params[] = Predicate.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			return prover.bind(p1, new Str(Formatter.treeize(p0)));
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
