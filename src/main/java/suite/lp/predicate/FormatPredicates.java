package suite.lp.predicate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import suite.Suite;
import suite.lp.predicate.PredicateUtil.SystemPredicate;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Persister.Loader;
import suite.node.io.Persister.Saver;
import suite.node.io.PrettyPrinter;
import suite.node.io.ReversePolish;
import suite.node.io.TermOp;
import suite.util.FileUtil;

public class FormatPredicates {

	private static ReversePolish rpn = new ReversePolish();

	public static SystemPredicate charAscii = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode();
		Node p1 = params[1].finalNode();
		return p0 instanceof Str && prover.bind(Int.of(((Str) p0).getValue().charAt(0)), p1) //
				|| p1 instanceof Int && prover.bind(new Str("" + (char) ((Int) p1).getNumber()), p0);
	};

	public static SystemPredicate concat = (prover, ps) -> {
		Node node = ps;
		StringBuilder sb = new StringBuilder();
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			sb.append(Formatter.display(tree.getLeft()));
			node = tree.getRight();
		}

		return prover.bind(new Str(sb.toString()), node);
	};

	public static SystemPredicate graphize = PredicateUtil.funPredicate(n -> new Str(Formatter.graphize(n)));

	public static SystemPredicate isAtom = PredicateUtil.boolPredicate(n -> n instanceof Atom);

	public static SystemPredicate isInt = PredicateUtil.boolPredicate(n -> n instanceof Int);

	public static SystemPredicate isString = PredicateUtil.boolPredicate(n -> n instanceof Str);

	public static SystemPredicate isTree = PredicateUtil.boolPredicate(n -> n instanceof Tree);

	public static SystemPredicate parse = PredicateUtil.funPredicate(n -> Suite.parse(Formatter.display(n)));

	public static SystemPredicate persistLoad = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		try (InputStream is = new FileInputStream(((Str) params[1].finalNode()).getValue())) {
			return prover.bind(params[0], new Loader().load(is));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

	public static SystemPredicate persistSave = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		try (OutputStream os = FileUtil.out(((Str) params[1].finalNode()).getValue())) {
			new Saver().save(os, params[0]);
			return true;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

	public static SystemPredicate prettyPrint = PredicateUtil.predicate(ps -> {
		System.out.println(new PrettyPrinter().prettyPrint(ps));
	});

	public static SystemPredicate rpnPredicate = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		if (p1 instanceof Str)
			return prover.bind(p0, rpn.fromRpn(((Str) p1).getValue()));
		else
			return prover.bind(new Str(rpn.toRpn(p0)), p1);
	};

	public static SystemPredicate startsWith = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();

		return p0 instanceof Atom && p1 instanceof Atom //
				&& ((Atom) p0).getName().startsWith(((Atom) p1).getName());
	};

	public static SystemPredicate stringLength = PredicateUtil.funPredicate(n -> Int.of(((Str) n).getValue().length()));

	public static SystemPredicate substring = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 4);
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

			return prover.bind(params[3], new Str(name.substring(m, n)));
		} else
			throw new RuntimeException("Invalid call pattern");
	};

	public static SystemPredicate toAtom = PredicateUtil.funPredicate(n -> Atom.of(Formatter.display(n)));

	public static SystemPredicate toDumpString = PredicateUtil.funPredicate(n -> new Str(Formatter.dump(n)));

	public static SystemPredicate toInt = PredicateUtil.funPredicate(n -> Int.of(Formatter.display(n).charAt(0)));

	public static SystemPredicate toString = PredicateUtil.funPredicate(n -> new Str(Formatter.display(n)));

	public static SystemPredicate treeize = PredicateUtil.funPredicate(n -> new Str(Formatter.treeize(n)));

	public static SystemPredicate trim = PredicateUtil.funPredicate(n -> new Str(Formatter.display(n).trim()));

}
