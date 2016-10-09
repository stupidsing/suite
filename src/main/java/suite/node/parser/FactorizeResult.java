package suite.node.parser;

import java.util.ArrayList;
import java.util.List;

import suite.Suite;
import suite.inspect.Inspect;
import suite.lp.doer.Generalizer;
import suite.lp.doer.ProverConstant;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Str;
import suite.node.util.Singleton;
import suite.node.util.TreeRewriter;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.util.FunUtil.Fun;
import suite.util.Nodify;
import suite.util.Util;

public class FactorizeResult {

	private static Inspect inspect = Singleton.get().getInspect();
	private static Nodify nodify = Singleton.get().getNodify();

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
		Chars pre = Util.first(list).pre;
		Chars post = Util.last(list).post;
		List<FPair> pairs = new ArrayList<>();

		for (int i = 0; i < list.size(); i++) {
			Chars space;
			if (i != list.size() - 1)
				space = Chars.of(pre.cs, list.get(i).post.start, list.get(i + 1).pre.end);
			else
				space = Chars.of("");
			pairs.add(new FPair(list.get(i).node, space));
		}

		FNode fn = new FTree(name, pairs);
		return new FactorizeResult(pre, fn, post);
	}

	public FactorizeResult(Chars pre, FNode node, Chars post) {
		this.pre = pre;
		this.node = node;
		this.post = post;
	}

	public static FactorizeResult rewrite(FactorizeResult frfrom, FactorizeResult frto, FactorizeResult fr0) {
		Generalizer generalizer = new Generalizer();
		TreeRewriter tr = new TreeRewriter();

		Fun<Node, Node> rewrite = n0 -> {
			Node m[] = Suite.matcher(FTerminal.class.getName() + ":.0").apply(n0);
			Node n1 = m != null ? m[0] : null;
			Node n2 = n1 instanceof Dict ? ((Dict) n1).map.get(Atom.of("chars")) : null;
			Node n3 = n2 != null ? n2.finalNode() : null;
			String s = n3 instanceof Str ? ((Str) n3).value : null;
			boolean b = s != null && s.startsWith(ProverConstant.variablePrefix) && s.substring(1).matches("[0-9]*");
			return b ? generalizer.generalize(Atom.of(s)) : n0;
		};

		Fun<FactorizeResult, Node> parse = fr -> tr.rewrite(rewrite, nodify.nodify(FNode.class, fr.node));

		Node nodeFrom = parse.apply(frfrom);
		Node nodeTo = parse.apply(frto);

		FNode fn0 = fr0.node;
		Node node0 = nodify.nodify(FNode.class, fn0);
		Node nodex = tr.rewrite(nodeFrom, nodeTo, node0);
		FNode fnx = nodify.unnodify(FNode.class, nodex);
		return new FactorizeResult(fr0.pre, fnx, fr0.post);
	}

	public String unparse() {
		CharsBuilder cb = new CharsBuilder();
		cb.append(pre);
		unparse(cb, node);
		cb.append(post);
		return cb.toChars().toString();
	}

	private void unparse(CharsBuilder cb, FNode fn) {
		if (fn instanceof FTree) {
			FTree ft = (FTree) fn;
			List<FPair> pairs = ft.pairs;
			for (FPair pair : pairs) {
				unparse(cb, pair.node);
				cb.append(pair.chars);
			}
		} else
			cb.append(((FTerminal) fn).chars);
	}

}
