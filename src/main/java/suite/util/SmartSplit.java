package suite.util;

import static primal.statics.Fail.fail;

import java.util.ArrayList;

import primal.MoreVerbs.Read;
import primal.adt.Pair;
import primal.primitive.ChrPrim.ChrPred;
import primal.streamlet.Streamlet;
import suite.node.io.Operator;
import suite.node.io.Operator.Assoc;
import suite.streamlet.ReadChars;
import suite.text.Segment;

public class SmartSplit {

	private ChrPred isOpen_ = c -> c == '(' || c == '[' || c == '{';
	private ChrPred isClose = c -> c == ')' || c == ']' || c == '}';
	private ChrPred isQuote = c -> c == '\'' || c == '"' || c == '`';

	public SmartSplit() {
		this(
				c -> c == '(' || c == '[' || c == '{',
				c -> c == ')' || c == ']' || c == '}',
				c -> c == '\'' || c == '"' || c == '`');
	}

	public SmartSplit(ChrPred isOpen_, ChrPred isClose, ChrPred isQuote) {
		this.isOpen_ = isOpen_;
		this.isClose = isClose;
		this.isQuote = isQuote;
	}

	public Streamlet<String> splitn(String s, String delim, Assoc assoc) {
		var pair = iter(s, delim, assoc);
		return pair.k.snoc(pair.v);
	}

	public Streamlet<String> splitnButLast(String s, String delim, Assoc assoc) {
		return iter(s, delim, assoc).k;
	}

	private Pair<Streamlet<String>, String> iter(String s, String delim, Assoc assoc) {
		var list = new ArrayList<String>();
		Pair<String, String> pair;

		while ((pair = split(s, delim, assoc)) != null) {
			list.add(pair.k);
			s = pair.v;
		}

		return Pair.of(Read.from(list), s);
	}

	public int searchPosition(String s, int start, String toMatch) {
		var nameLength = toMatch.length();
		var end = s.length() - nameLength;
		var quote = 0;

		for (var pos = start; pos <= end; pos++) {
			var c = s.charAt(pos);
			quote = getQuoteChange(quote, c);

			if (quote == 0 && s.startsWith(toMatch, pos))
				return pos;
		}

		return -1;
	}

	public Pair<String, String> split(String s, String delim, Assoc assoc) {
		return split(s, Segment.of(0, s.length()), delim, assoc, true);
	}

	private Pair<String, String> split(String s, Segment segment, String delim, Assoc assoc, boolean isCheckDepth) {
		var ops = searchSegment(s.toCharArray(), segment, delim, assoc, isCheckDepth);

		if (ops != null) {
			var left = s.substring(segment.start, ops.start);
			var right = s.substring(ops.end, segment.end);
			return Pair.of(left, right);
		} else
			return null;
	}

	public Segment searchSegment(char[] cs, Segment segment, Operator operator) {
		return searchSegment(cs, segment, operator.name_(), operator.assoc(), true);
	}

	public Segment searchSegment(char[] cs, Segment segment, String delim, Assoc assoc, boolean isCheckDepth) {
		var nameLength = delim.length();
		int start1 = segment.start, end1 = segment.end - 1;
		int quote = 0, depth = 0;
		int pos0, posx, step;

		if (start1 <= end1) {
			if (assoc == Assoc.RIGHT) {
				pos0 = start1;
				posx = end1;
				step = 1;
			} else {
				pos0 = end1;
				posx = start1;
				step = -1;
			}

			for (var pos = pos0; pos != posx + step; pos += step) {
				var c = cs[pos];
				quote = getQuoteChange(quote, c);

				if (quote == 0) {
					if (isCheckDepth)
						depth = checkDepth(depth, c);

					if (depth == 0 && pos + nameLength <= cs.length) {
						var b = true; // cs.startsWith(name, pos)
						for (var i = 0; b && i < nameLength; i++)
							b &= cs[pos + i] == delim.charAt(i);
						if (b)
							return Segment.of(pos, pos + nameLength);
					}
				}
			}
		}

		return null;
	}

	public boolean isParseable(String s) {
		return isParseable(s, false);
	}

	/**
	 * Judges if the input string has balanced quote characters and bracket
	 * characters.
	 *
	 * @param isThrow if this is set to true, and the string is deemed unparseable
	 *                even if more characters are added, throw exception.
	 */
	public boolean isParseable(String s, boolean isThrow) {
		int quote = 0, depth = 0;

		// shows warning if the atom has mismatched quotes or brackets
		for (var c : ReadChars.from(s)) {
			quote = getQuoteChange(quote, c);
			if (quote == 0)
				depth = checkDepth(depth, c);
		}

		return !isThrow || 0 <= depth ? quote == 0 && depth == 0 : fail("parse error");
	}

	private int checkDepth(int depth, char c) {
		if (isOpen_.test(c))
			depth++;
		if (isClose.test(c))
			depth--;
		return depth;
	}

	public int getQuoteChange(int quote, char c) {
		if (c == quote)
			quote = 0;
		else if (quote == 0 && isQuote.test(c))
			quote = c;
		return quote;
	}

}
