package suite.node.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.CharsUtil;
import suite.text.Segment;
import suite.text.Transform;
import suite.text.Transform.Reverser;
import suite.util.Pair;
import suite.util.ParseUtil;
import suite.util.To;
import suite.util.Util;

public class RecursiveFactorizer {

	private Operator operators[];
	private Chars in;
	private Reverser reverser;

	public interface FNode {
	}

	public static class FTerminal implements FNode {
		public final Chars pre, chars, post;

		public FTerminal(Chars pre, Chars chars, Chars post) {
			this.pre = pre;
			this.chars = chars;
			this.post = post;
		}
	}

	public static class FTree implements FNode {
		public final String name;
		public final List<FNode> fns;

		public FTree(String name, List<FNode> fns) {
			this.name = name;
			this.fns = fns;
		}
	}

	public RecursiveFactorizer(Operator operators[]) {
		this.operators = operators;
	}

	public FNode parse(String s) {
		this.in = To.chars(s);
		Pair<String, Reverser> pair = Transform.transform(PreprocessorFactory.create(operators), s);
		String in1 = pair.t0;
		reverser = pair.t1;

		return parse0(To.chars(in1), 0);
	}

	public String unparse(FNode fn) {
		CharsBuilder cb = new CharsBuilder();
		Deque<FNode> deque = new ArrayDeque<>();
		deque.push(fn);

		while (!deque.isEmpty()) {
			FNode n = deque.pop();
			if (n instanceof FTree)
				for (FNode child : Util.reverse(((FTree) n).fns))
					deque.push(child);
			else {
				FTerminal ft = (FTerminal) n;
				cb.append(ft.pre);
				cb.append(ft.chars);
				cb.append(ft.post);
			}
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
				list.add(terminal(middle));
				list.add(parse0(right, ri));
				if (post != null)
					list.add(terminal(post));

				return new FTree(operator.getName(), list);
			}

			if (first == '(' && last == ')' //
					|| first == '[' && last == ']' //
					|| first == '`' && last == '`') {
				Chars left = Chars.of(chars.cs, chars.start, chars1.start + 1);
				Chars middle = Chars.of(chars.cs, chars1.start + 1, chars1.end - 1);
				Chars right = Chars.of(chars.cs, chars1.end - 1, chars.end);
				return new FTree("" + first, Arrays.asList(terminal(left), parse0(middle, 0), terminal(right)));
			}
		}

		return terminal(chars);
	}

	private FTerminal terminal(Chars chars) {
		Chars chars1 = CharsUtil.trim(chars);
		int p0 = reverser.reverseBegin(chars.start);
		int p1 = reverser.reverseBegin(chars1.start);
		int p2 = reverser.reverseBegin(chars1.end);
		int px = reverser.reverseBegin(chars.end);

		return new FTerminal(new Chars(in.cs, p0, p1) //
				, new Chars(in.cs, p1, p2) //
				, new Chars(in.cs, p2, px));
	}

}
