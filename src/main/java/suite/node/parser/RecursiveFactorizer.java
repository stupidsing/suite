package suite.node.parser;

import java.util.ArrayList;
import java.util.List;

import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.node.io.TermOp;
import suite.node.parser.FactorizeResult.FTerminal;
import suite.primitive.Chars;
import suite.text.Preprocess;
import suite.text.Preprocess.Reverser;
import suite.text.Segment;
import suite.util.ParseUtil;
import suite.util.To;

public class RecursiveFactorizer {

	private Operator[] operators;
	private Chars in;
	private Reverser reverser;

	public RecursiveFactorizer(Operator[] operators) {
		this.operators = operators;
	}

	public String rewrite(String from, String to, String s0) {
		var frfrom = parse(from);
		var frto = parse(to);
		return FactorizeResult.rewrite(frfrom, frto, parse(s0)).unparse();
	}

	public FactorizeResult parse(String s) {
		in = To.chars(s);
		var pair = Preprocess.transform(PreprocessorFactory.create(operators), s);
		var in1 = pair.t0;
		reverser = pair.t1;

		FactorizeResult parsed = parse_(To.chars(in1), 0);

		// append possibly missed comments
		var p = reverser.reverse(0);
		return new FactorizeResult(Chars.concat(in.range(0, p), parsed.pre), parsed.node, parsed.post);
	}

	private FactorizeResult parse_(Chars chars, int fromOp) {
		var chars1 = chars.trim();

		if (0 < chars1.size()) {
			var first = chars1.get(0);
			var last = chars1.get(-1);

			for (var i = fromOp; i < operators.length; i++) {
				var operator = operators[i];
				var range = operator != TermOp.TUPLE_ ? chars : chars1;
				Segment ops = ParseUtil.searchPosition(chars.cs, Segment.of(range.start, range.end), operator);

				if (ops == null)
					continue;

				Chars left = Chars.of(chars.cs, chars.start, ops.start);
				Chars middle = Chars.of(chars.cs, ops.start, ops.end);
				Chars right = Chars.of(chars.cs, ops.end, chars.end);
				Chars post = null;
				int li, ri;

				if (operator == TermOp.BRACES) {
					if (chars1.end < ops.start || last != '}')
						continue;

					right = Chars.of(chars.cs, ops.end, chars1.end - 1);
					post = Chars.of(chars.cs, chars1.end - 1, chars.end);
					li = 0;
					ri = 0;
				} else {
					if (operator == TermOp.TUPLE_)
						if (left.isWhitespaces() || right.isWhitespaces())
							continue;

					var isLeftAssoc = operator.getAssoc() == Assoc.LEFT;
					li = fromOp + (isLeftAssoc ? 0 : 1);
					ri = fromOp + (isLeftAssoc ? 1 : 0);
				}

				List<FactorizeResult> list = new ArrayList<>(4);
				list.add(parse_(left, li));
				list.add(term(middle));
				list.add(parse_(right, ri));
				if (post != null)
					list.add(term(post));

				return FactorizeResult.merge(operator.toString(), list);
			}

			if (first == '(' && last == ')' //
					|| first == '[' && last == ']' //
					|| first == '`' && last == '`') {
				Chars left = Chars.of(chars.cs, chars.start, chars1.start + 1);
				Chars middle = Chars.of(chars.cs, chars1.start + 1, chars1.end - 1);
				Chars right = Chars.of(chars.cs, chars1.end - 1, chars.end);
				return FactorizeResult.merge("" + first, List.of(term(left), parse_(middle, 0), term(right)));
			}
		}

		return term(chars);
	}

	private FactorizeResult term(Chars chars) {
		var chars1 = chars.trim();
		var p0 = reverser.reverse(chars.start);
		var p1 = reverser.reverse(chars1.start);
		var p2 = reverser.reverse(chars1.end);
		var px = reverser.reverse(chars.end);
		return new FactorizeResult(Chars.of(in.cs, p0, p1), new FTerminal(Chars.of(in.cs, p1, p2)), Chars.of(in.cs, p2, px));
	}

}
