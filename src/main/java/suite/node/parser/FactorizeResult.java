package suite.node.parser;

import java.util.ArrayList;
import java.util.List;

import primal.Verbs.First;
import primal.Verbs.Last;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Iterate;
import primal.primitive.adt.Chars;
import primal.primitive.adt.Chars.CharsBuilder;
import suite.Suite;
import suite.inspect.Inspect;
import suite.lp.doer.Generalizer;
import suite.lp.doer.ProverConstant;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Str;
import suite.node.util.Rewrite;
import suite.node.util.Singleton;
import suite.util.Nodify;
import suite.util.To;

public class FactorizeResult {

	private static Inspect inspect = Singleton.me.inspect;
	private static Nodify nodify = Singleton.me.nodify;
	private static Rewrite rw = new Rewrite();

	public final Chars pre;
	public final FNode node;
	public final Chars post;

	public interface FNode {
	}

	public static class FNodeImpl implements FNode {
		public boolean equals(Object object) {
			return inspect.equals(this, object);
		}

		public int hashCode() {
			return inspect.hashCode(this);
		}
	}

	public static class FTerminal extends FNodeImpl {
		public final Chars chars;

		public FTerminal() {
			this(null);
		}

		public FTerminal(Chars chars) {
			this.chars = chars;
		}
	}

	public static class FTree extends FNodeImpl {
		public final String name;
		public final List<FPair> pairs;

		public FTree() {
			this(null, null);
		}

		public FTree(String name, List<FPair> pairs) {
			this.name = name;
			this.pairs = pairs;
		}
	}

	public static class FPair {
		public final FNode node;
		public final Chars chars;

		public FPair() {
			this(null, null);
		}

		public FPair(FNode node, Chars chars) {
			this.node = node;
			this.chars = chars;
		}
	}

	public static FactorizeResult merge(String name, List<FactorizeResult> list) {
		var pre = First.of(list).pre;
		var post = Last.of(list).post;
		var pairs = new ArrayList<FPair>();

		for (var i = 0; i < list.size(); i++) {
			Chars space;
			if (i != list.size() - 1)
				space = Chars.of(pre.cs, list.get(i).post.start, list.get(i + 1).pre.end);
			else
				space = To.chars("");
			pairs.add(new FPair(list.get(i).node, space));
		}

		var fn = new FTree(name, pairs);
		return new FactorizeResult(pre, fn, post);
	}

	public FactorizeResult(Chars pre, FNode node, Chars post) {
		this.pre = pre;
		this.node = node;
		this.post = post;
	}

	public static FactorizeResult rewrite(FactorizeResult frfrom, FactorizeResult frto, FactorizeResult fr0) {
		var generalizer = new Generalizer();

		Iterate<Node> rewrite = n0 -> {
			var m = Suite.pattern(FTerminal.class.getName() + ":.0").match(n0);
			var n1 = m != null ? m[0] : null;
			var n2 = n1 instanceof Dict dict ? dict.getMap().get(Atom.of("chars")) : null;
			var n3 = n2 != null ? n2.finalNode() : null;
			var s = n3 instanceof Str str ? str.value : null;
			var b = s != null && s.startsWith(ProverConstant.variablePrefix) && s.substring(1).matches("[0-9]*");
			return b ? generalizer.generalize(Atom.of(s)) : n0;
		};

		Fun<FactorizeResult, Node> parse = fr -> rw.rewrite(rewrite, nodify.nodify(FNode.class, fr.node));

		var nodeFrom = parse.apply(frfrom);
		var nodeTo = parse.apply(frto);

		var fn0 = fr0.node;
		var node0 = nodify.nodify(FNode.class, fn0);
		var nodex = rw.rewrite(nodeFrom, nodeTo, node0);
		var fnx = nodify.unnodify(FNode.class, nodex);
		return new FactorizeResult(fr0.pre, fnx, fr0.post);
	}

	public String unparse() {
		return Chars.build(cb -> {
			cb.append(pre);
			unparse(cb, node);
			cb.append(post);
		}).toString();
	}

	private void unparse(CharsBuilder cb, FNode fn) {
		if (fn instanceof FTree ft) {
			var pairs = ft.pairs;
			for (var pair : pairs) {
				unparse(cb, pair.node);
				cb.append(pair.chars);
			}
		} else
			cb.append(((FTerminal) fn).chars);
	}

}
