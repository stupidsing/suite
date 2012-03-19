package org.suite.predicates;

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
