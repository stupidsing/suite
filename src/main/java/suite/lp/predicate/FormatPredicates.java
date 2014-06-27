package suite.lp.predicate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import suite.Suite;
import suite.lp.predicate.SystemPredicates.SystemPredicate;
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

	public static SystemPredicate graphize = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		return prover.bind(p1, new Str(Formatter.graphize(p0)));
	};

	public static SystemPredicate isAtom = (prover, ps) -> {
		return ps.finalNode() instanceof Atom;
	};

	public static SystemPredicate isInt = (prover, ps) -> {
		return ps.finalNode() instanceof Int;
	};

	public static SystemPredicate isString = (prover, ps) -> {
		return ps.finalNode() instanceof Str;
	};

	public static SystemPredicate isTree = (prover, ps) -> {
		return ps.finalNode() instanceof Tree;
	};

	public static SystemPredicate parse = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		return prover.bind(Suite.parse(Formatter.display(p0)), p1);
	};

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

	public static SystemPredicate prettyPrint = (prover, ps) -> {
		PrettyPrinter printer = new PrettyPrinter();
		System.out.println(printer.prettyPrint(ps));
		return true;
	};

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

	public static SystemPredicate stringLength = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Str str = (Str) params[0].finalNode();
		int length = str.getValue().length();
		return prover.bind(params[1], Int.of(length));
	};

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

			return prover.bind(params[3] //
					, new Str(name.substring(m, n)));
		} else
			throw new RuntimeException("Invalid call pattern");
	};

	public static SystemPredicate toAtom = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		return prover.bind(p1, Atom.of(Formatter.display(p0)));
	};

	public static SystemPredicate toDumpString = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		return prover.bind(p1, new Str(Formatter.dump(p0)));
	};

	public static SystemPredicate toInt = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		return prover.bind(p1, Int.of(Formatter.display(p0).charAt(0)));
	};

	public static SystemPredicate toString = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		return prover.bind(p1, new Str(Formatter.display(p0)));
	};

	public static SystemPredicate treeize = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		return prover.bind(p1, new Str(Formatter.treeize(p0)));
	};

	public static SystemPredicate trim = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		return prover.bind(p1, new Str(Formatter.display(p0).trim()));
	};

}
