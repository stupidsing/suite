package suite.lp.predicate;

import static java.lang.Math.min;
import static primal.statics.Fail.fail;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import primal.Verbs.Build;
import primal.Verbs.ReadFile;
import primal.Verbs.WriteFile;
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

public class FormatPredicates {

	private ReversePolish rpn = new ReversePolish();

	public BuiltinPredicate charAscii = PredicateUtil.p2((prover, p0, p1) -> {
		return p0 instanceof Str s && prover.bind(Int.of(s.value.charAt(0)), p1) //
				|| p1 instanceof Int i && prover.bind(new Str("" + (char) i.number), p0);
	});

	public BuiltinPredicate concat = PredicateUtil.ps((prover, nodes) -> {
		var n = nodes.length;
		var s = Build.string(sb -> {
			for (var i = 0; i < n - 1; i++)
				sb.append(Formatter.display(nodes[i]));
		});
		return prover.bind(new Str(s), nodes[n - 1]);
	});

	public BuiltinPredicate graphize = PredicateUtil.fun(n -> new Str(Formatter.graphize(n)));

	public BuiltinPredicate isAtom = PredicateUtil.bool(n -> n instanceof Atom);

	public BuiltinPredicate isInt = PredicateUtil.bool(n -> n instanceof Int);

	public BuiltinPredicate isString = PredicateUtil.bool(n -> n instanceof Str);

	public BuiltinPredicate isTree = PredicateUtil.bool(n -> n instanceof Tree);

	public BuiltinPredicate parse = PredicateUtil.fun(n -> Suite.parse(Formatter.display(n)));

	public BuiltinPredicate persistLoad = PredicateUtil.p2((prover, node, filename) -> {
		return ReadFile.from(Str.str(filename)).doRead(is -> {
			try (var gis = new GZIPInputStream(is); var dis = new DataInputStream(gis)) {
				var grapher = new Grapher();
				grapher.load(dis);
				return prover.bind(node, grapher.ungraph());
			}
		});
	});

	public BuiltinPredicate persistSave = PredicateUtil.p2((prover, node, filename) -> {
		WriteFile.to(Str.str(filename)).doWrite(os -> {
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
		if (r instanceof Str str)
			return prover.bind(node, rpn.fromRpn(str.value));
		else
			return prover.bind(new Str(rpn.toRpn(node)), r);
	});

	public BuiltinPredicate startsWith = PredicateUtil.p2((prover, s, start) -> true //
			&& s instanceof Atom s_ //
			&& start instanceof Atom start_ //
			&& s_.name.startsWith(start_.name));

	public BuiltinPredicate stringLength = PredicateUtil.fun(n -> Int.of(Str.str(n).length()));

	public BuiltinPredicate substring = PredicateUtil.p4((prover, s0, p0, px, sx) -> {
		var name = Str.str(s0);
		var length = name.length();

		if (p0 instanceof Int p0_ && px instanceof Int px_) {
			int m = p0_.number, n = px_.number;

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
