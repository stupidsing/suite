package suite.node.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
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
		ENCLOSE, OPER___, SPACE__, TERMINAL,
	}

	public static class FNode {
		public int hashCode() {
			return inspect.hashCode(this);
		}

		public boolean equals(Object object) {
			return inspect.equals(this, object);
		}
	}

	public static class FTerminal extends FNode {
		public final Chars chars;

		public FTerminal(Chars chars) {
			this.chars = chars;
		}
	}

	public static class FTree extends FNode {
		public final FNodeType type;
		public final String name;
		public final List<FNode> fns;

		public FTree(FNodeType type, String name, List<FNode> fns) {
			this.type = type;
			this.name = name;
			this.fns = fns;
		}
	}

	public RecursiveFactorizer(Operator operators[]) {
		this.operators = operators;
	}

	public FNode parse(String s) {
		this.in = To.chars(s);
		Pair<String, Reverser> pair = Preprocess.transform(PreprocessorFactory.create(operators), s);
		String in1 = pair.t0;
		reverser = pair.t1;

		return parse0(To.chars(in1), 0);
	}

	public String unparse(FNode fn) {
		CharsBuilder cb = new CharsBuilder();
		Deque<FNode> deque = new ArrayDeque<>();
		deque.push(fn);

		while (!deque.isEmpty()) {
			FNode fn_ = deque.pop();
			if (fn_ instanceof FTree)
				for (FNode child : Util.reverse(((FTree) fn_).fns))
					deque.push(child);
			else
				cb.append(((FTerminal) fn_).chars);
		}
		return cb.toChars().toString();
	}

	private FNode parse0(Chars chars, int fromOp) {
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

				List<FNode> list = new ArrayList<>(4);
				list.add(parse0(left, li));
				list.add(term(middle));
				list.add(parse0(right, ri));
				if (post != null)
					list.add(term(post));

				return new FTree(FNodeType.OPER___, operator.getName(), list);
			}

			if (first == '(' && last == ')' //
					|| first == '[' && last == ']' //
					|| first == '`' && last == '`') {
				Chars left = Chars.of(chars.cs, chars.start, chars1.start + 1);
				Chars middle = Chars.of(chars.cs, chars1.start + 1, chars1.end - 1);
				Chars right = Chars.of(chars.cs, chars1.end - 1, chars.end);
				return new FTree(FNodeType.ENCLOSE, "" + first, Arrays.asList(term(left), parse0(middle, 0), term(right)));
			}
		}

		return term(chars);
	}

	private FNode term(Chars chars) {
		Chars chars1 = CharsUtil.trim(chars);
		int p0 = reverser.reverseEnd(chars.start);
		int p1 = reverser.reverseEnd(chars1.start);
		int p2 = reverser.reverseEnd(chars1.end);
		int px = reverser.reverseEnd(chars.end);

		List<FNode> list = Arrays.asList(new FTerminal(new Chars(in.cs, p0, p1)) //
				, new FTerminal(new Chars(in.cs, p1, p2)) //
				, new FTerminal(new Chars(in.cs, p2, px)));

		return new FTree(FNodeType.TERMINAL, null, list);
	}

}
