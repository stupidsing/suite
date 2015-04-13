package suite.node.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.adt.Pair;
import suite.inspect.Inspect;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.CharsUtil;
import suite.text.Preprocess;
import suite.text.Preprocess.Reverser;
import suite.text.Segment;
import suite.util.ParseUtil;
import suite.util.To;
import suite.util.Util;

public class RecursiveFactorizer {

	private static Inspect inspect = new Inspect();

	private Operator operators[];
	private Chars in;
	private Reverser reverser;

	public enum FNodeType {
		ENCLOSE_, OPERATOR, TERMINAL,
	}

	public interface FNode {
	}

	public static class FNodeImpl implements FNode {
		public int hashCode() {
			return inspect.hashCode(this);
		}

		public boolean equals(Object object) {
			return inspect.equals(this, object);
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
		public final FNodeType type;
		public final String name;
		public final List<FPair> pairs;

		public FTree() {
			this(null, null, null);
		}

		public FTree(FNodeType type, String name, List<FPair> pairs) {
			this.type = type;
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

	public static class FR {
		public final Chars pre;
		public final FNode node;
		public final Chars post;

		public FR(Chars pre, FNode node, Chars post) {
			this.pre = pre;
			this.node = node;
			this.post = post;
		}
	}

	public RecursiveFactorizer(Operator operators[]) {
		this.operators = operators;
	}

	public FR parse(String s) {
		in = To.chars(s);
		Pair<String, Reverser> pair = Preprocess.transform(PreprocessorFactory.create(operators), s);
		String in1 = pair.t0;
		reverser = pair.t1;
		return parse0(To.chars(in1), 0);
	}

	public String unparse(FR fr) {
		CharsBuilder cb = new CharsBuilder();
		cb.append(fr.pre);
		unparse(cb, fr.node);
		cb.append(fr.post);
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

	private FR parse0(Chars chars, int fromOp) {
		Chars chars1 = CharsUtil.trim(chars);

		if (chars1.size() > 0) {
			char first = chars1.get(0);
			char last = chars1.get(-1);

			for (int i = fromOp; i < operators.length; i++) {
				Operator operator = operators[i];
				Chars range = operator != TermOp.TUPLE_ ? chars : chars1;
				Segment ops = ParseUtil.searchPosition(chars.cs, new Segment(range.start, range.end), operator);

				if (ops == null)
					continue;

				Chars left = Chars.of(chars.cs, chars.start, ops.start);
				Chars middle = Chars.of(chars.cs, ops.start, ops.end);
				Chars right = Chars.of(chars.cs, ops.end, chars.end);
				Chars post = null;
				int li, ri;

				if (operator == TermOp.BRACES) {
					if (ops.start > chars1.end || last != '}')
						continue;

					right = Chars.of(chars.cs, ops.end, chars1.end - 1);
					post = Chars.of(chars.cs, chars1.end - 1, chars.end);
					li = 0;
					ri = 0;
				} else {
					if (operator == TermOp.TUPLE_)
						if (CharsUtil.isWhitespaces(left) || CharsUtil.isWhitespaces(right))
							continue;

					boolean isLeftAssoc = operator.getAssoc() == Assoc.LEFT;
					li = fromOp + (isLeftAssoc ? 0 : 1);
					ri = fromOp + (isLeftAssoc ? 1 : 0);
				}

				List<FR> list = new ArrayList<>(4);
				list.add(parse0(left, li));
				list.add(term(middle));
				list.add(parse0(right, ri));
				if (post != null)
					list.add(term(post));

				return merge(FNodeType.OPERATOR, operator.toString(), list);
			}

			if (first == '(' && last == ')' //
					|| first == '[' && last == ']' //
					|| first == '`' && last == '`') {
				Chars left = Chars.of(chars.cs, chars.start, chars1.start + 1);
				Chars middle = Chars.of(chars.cs, chars1.start + 1, chars1.end - 1);
				Chars right = Chars.of(chars.cs, chars1.end - 1, chars.end);
				return merge(FNodeType.ENCLOSE_, "" + first, Arrays.asList(term(left), parse0(middle, 0), term(right)));
			}
		}

		return term(chars);
	}

	private FR term(Chars chars) {
		Chars chars1 = CharsUtil.trim(chars);
		int p0 = reverser.reverseEnd(chars.start);
		int p1 = reverser.reverseEnd(chars1.start);
		int p2 = reverser.reverseEnd(chars1.end);
		int px = reverser.reverseEnd(chars.end);
		return new FR(Chars.of(in.cs, p0, p1), new FTerminal(Chars.of(in.cs, p1, p2)), Chars.of(in.cs, p2, px));
	}

	private FR merge(FNodeType type, String name, List<FR> list) {
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

		FNode fn = new FTree(type, name, pairs);
		return new FR(pre, fn, post);
	}

}
