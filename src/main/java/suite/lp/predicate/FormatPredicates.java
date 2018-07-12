package suite.lp.predicate;

import static suite.util.Friends.fail;
import static suite.util.Friends.min;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import suite.Suite;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.Grapher;
import suite.node.io.ReversePolish;
import suite.node.pp.NewPrettyPrinter;
import suite.node.pp.PrettyPrinter;
import suite.os.FileUtil;

public class FormatPredicates {

	private ReversePolish rpn = new ReversePolish();

	public BuiltinPredicate charAscii = PredicateUtil.p2((prover, p0, p1) -> {
		return p0 instanceof Str && prover.bind(Int.of(Str.str(p0).charAt(0)), p1) //
				|| p1 instanceof Int && prover.bind(new Str("" + (char) Int.num(p1)), p0);
	});

	public BuiltinPredicate concat = PredicateUtil.ps((prover, nodes) -> {
		var sb = new StringBuilder();
		var n = nodes.length;
		for (var i = 0; i < n - 1; i++)
			sb.append(Formatter.display(nodes[i]));
		return prover.bind(new Str(sb.toString()), nodes[n - 1]);
	});

	public BuiltinPredicate graphize = PredicateUtil.fun(n -> new Str(Formatter.graphize(n)));

	public BuiltinPredicate isAtom = PredicateUtil.bool(n -> n instanceof Atom);

	public BuiltinPredicate isInt = PredicateUtil.bool(n -> n instanceof Int);

	public BuiltinPredicate isString = PredicateUtil.bool(n -> n instanceof Str);

	public BuiltinPredicate isTree = PredicateUtil.bool(n -> n instanceof Tree);

	public BuiltinPredicate parse = PredicateUtil.fun(n -> Suite.parse(Formatter.display(n)));

	public BuiltinPredicate persistLoad = PredicateUtil.p2((prover, node, filename) -> {
		return FileUtil.in(Str.str(filename)).doRead(is -> {
			try (var gis = new GZIPInputStream(is); var dis = new DataInputStream(gis)) {
				var grapher = new Grapher();
				grapher.load(dis);
				return prover.bind(node, grapher.ungraph());
			}
		});
	});

	public BuiltinPredicate persistSave = PredicateUtil.p2((prover, node, filename) -> {
		FileUtil.out(Str.str(filename)).doWrite(os -> {
			try (var gos = new GZIPOutputStream(os); var dos = new DataOutputStream(gos)) {
				var grapher = new Grapher();
				grapher.graph(node);
				grapher.save(dos);
			}
		});
		return true;
	});

	public BuiltinPredicate prettyPrint = PredicateUtil.sink(n -> System.out.println(new PrettyPrinter().prettyPrint(n)));

	public BuiltinPredicate prettyPrintNew = PredicateUtil.sink(n -> System.out.println(new NewPrettyPrinter().prettyPrint(n)));

	public BuiltinPredicate rpnPredicate = PredicateUtil.p2((prover, node, r) -> {
		if (r instanceof Str)
			return prover.bind(node, rpn.fromRpn(Str.str(r)));
		else
			return prover.bind(new Str(rpn.toRpn(node)), r);
	});

	public BuiltinPredicate startsWith = PredicateUtil.p2((prover, s, start) -> true //
			&& s instanceof Atom //
			&& start instanceof Atom //
			&& Atom.name(s).startsWith(Atom.name(start)));

	public BuiltinPredicate stringLength = PredicateUtil.fun(n -> Int.of(Str.str(n).length()));

	public BuiltinPredicate substring = PredicateUtil.p4((prover, s0, p0, px, sx) -> {
		var name = Str.str(s0);
		var length = name.length();

		if (p0 instanceof Int && px instanceof Int) {
			int m = Int.num(p0), n = Int.num(px);

			while (m < 0)
				m += length;
			while (n <= 0)
				n += length;

			n = min(n, length);

			return prover.bind(sx, new Str(name.substring(m, n)));
		} else
			return fail("invalid call pattern");
	});

	public BuiltinPredicate toAtom = PredicateUtil.fun(n -> Atom.of(Formatter.display(n)));

	public BuiltinPredicate toDumpString = PredicateUtil.fun(n -> new Str(Formatter.dump(n)));

	public BuiltinPredicate toInt = PredicateUtil.fun(n -> Int.of(Formatter.display(n).charAt(0)));

	public BuiltinPredicate toString = PredicateUtil.fun(n -> new Str(Formatter.display(n)));

	public BuiltinPredicate treeize = PredicateUtil.fun(n -> new Str(Formatter.treeize(n)));

	public BuiltinPredicate trim = PredicateUtil.fun(n -> new Str(Formatter.display(n).trim()));

}
