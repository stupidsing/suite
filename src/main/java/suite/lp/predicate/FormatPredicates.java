package suite.lp.predicate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import suite.Suite;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Persister.Loader;
import suite.node.io.Persister.Saver;
import suite.node.io.ReversePolish;
import suite.node.io.TermOp;
import suite.node.pp.NewPrettyPrinter;
import suite.node.pp.PrettyPrinter;
import suite.os.FileUtil;

public class FormatPredicates {

	private ReversePolish rpn = new ReversePolish();

	public BuiltinPredicate charAscii = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode();
		Node p1 = params[1].finalNode();
		return p0 instanceof Str && prover.bind(Int.of(((Str) p0).value.charAt(0)), p1) //
				|| p1 instanceof Int && prover.bind(new Str("" + (char) ((Int) p1).number), p0);
	};

	public BuiltinPredicate concat = (prover, ps) -> {
		Node node = ps;
		StringBuilder sb = new StringBuilder();
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.TUPLE_)) != null) {
			sb.append(Formatter.display(tree.getLeft()));
			node = tree.getRight();
		}

		return prover.bind(new Str(sb.toString()), node);
	};

	public BuiltinPredicate graphize = PredicateUtil.fun(n -> new Str(Formatter.graphize(n)));

	public BuiltinPredicate isAtom = PredicateUtil.bool(n -> n instanceof Atom);

	public BuiltinPredicate isInt = PredicateUtil.bool(n -> n instanceof Int);

	public BuiltinPredicate isString = PredicateUtil.bool(n -> n instanceof Str);

	public BuiltinPredicate isTree = PredicateUtil.bool(n -> n instanceof Tree);

	public BuiltinPredicate parse = PredicateUtil.fun(n -> Suite.parse(Formatter.display(n)));

	public BuiltinPredicate persistLoad = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		try (InputStream is = new FileInputStream(((Str) params[1].finalNode()).value)) {
			return prover.bind(params[0], new Loader().load(is));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

	public BuiltinPredicate persistSave = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		try (OutputStream os = FileUtil.out(((Str) params[1].finalNode()).value)) {
			new Saver().save(os, params[0]);
			return true;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	};

	public BuiltinPredicate prettyPrint = PredicateUtil.run(n -> System.out.println(new PrettyPrinter().prettyPrint(n)));

	public BuiltinPredicate prettyPrintNew = PredicateUtil.run(n -> System.out.println(new NewPrettyPrinter().prettyPrint(n)));

	public BuiltinPredicate rpnPredicate = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
		if (p1 instanceof Str)
			return prover.bind(p0, rpn.fromRpn(((Str) p1).value));
		else
			return prover.bind(new Str(rpn.toRpn(p0)), p1);
	};

	public BuiltinPredicate startsWith = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 2);
		Node p0 = params[0].finalNode(), p1 = params[1].finalNode();

		return p0 instanceof Atom && p1 instanceof Atom //
				&& ((Atom) p0).name.startsWith(((Atom) p1).name);
	};

	public BuiltinPredicate stringLength = PredicateUtil.fun(n -> Int.of(((Str) n).value.length()));

	public BuiltinPredicate substring = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 4);
		String name = ((Str) params[0].finalNode()).value;
		int length = name.length();
		Node p1 = params[1].finalNode(), p2 = params[2].finalNode();

		if (p1 instanceof Int && p2 instanceof Int) {
			int m = ((Int) p1).number, n = ((Int) p2).number;

			while (m < 0)
				m += length;
			while (n <= 0)
				n += length;

			n = Math.min(n, length);

			return prover.bind(params[3], new Str(name.substring(m, n)));
		} else
			throw new RuntimeException("Invalid call pattern");
	};

	public BuiltinPredicate toAtom = PredicateUtil.fun(n -> Atom.of(Formatter.display(n)));

	public BuiltinPredicate toDumpString = PredicateUtil.fun(n -> new Str(Formatter.dump(n)));

	public BuiltinPredicate toInt = PredicateUtil.fun(n -> Int.of(Formatter.display(n).charAt(0)));

	public BuiltinPredicate toString = PredicateUtil.fun(n -> new Str(Formatter.display(n)));

	public BuiltinPredicate treeize = PredicateUtil.fun(n -> new Str(Formatter.treeize(n)));

	public BuiltinPredicate trim = PredicateUtil.fun(n -> new Str(Formatter.display(n).trim()));

}
