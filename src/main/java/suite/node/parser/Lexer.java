package suite.node.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.streamlet.Read;
import suite.util.CommandUtil;

/**
 * Non-recursive, performance-improved parser for operator-based languages.
 *
 * @author ywsing
 */
public class Lexer {

	private CommandUtil<Operator> commandUtil;
	private String in;
	private int pos = 0;
	private Token token0;

	public enum LexType {
		CHAR__, HEX__, ID___, OPER_, SPACE, STR__, SYM__,
	}

	public class Token {
		public final LexType type;
		public final Operator operator;
		private String data;

		private Token(LexType type, Operator operator) {
			this.type = type;
			this.operator = operator;
		}

		public String getData() {
			return data;
		}
	}

	public Lexer(Operator[] operators, String in) {
		this.in = in;

		Map<String, Operator> operatorByName = Read.from(operators) //
				.filter(operator -> operator != TermOp.TUPLE_) //
				.toMap(Operator::getName);

		commandUtil = new CommandUtil<>(operatorByName);
	}

	public Token lex() {
		return token0 = lex_();
	}

	private Token lex_() {
		if (pos < in.length()) {
			int start = pos;
			Token token = detect();
			LexType type = token.type;

			if (type == LexType.ID___ || type == LexType.SPACE)
				while (pos < in.length() && detect().type == type)
					pos++;
			else if (type == LexType.CHAR__)
				pos += 4;
			else if (type == LexType.HEX__) {
				pos += 2;
				while (pos < in.length() && 0 <= "0123456789ABCDEF".indexOf(in.charAt(pos)))
					pos++;
			} else if (type == LexType.OPER_)
				pos += token.operator.getName().length();
			else if (type == LexType.STR__) {
				char quote = in.charAt(pos);
				while (pos < in.length() && in.charAt(pos) == quote) {
					pos++;
					while (pos < in.length() && in.charAt(pos) != quote)
						pos++;
					pos++;
				}
			} else if (type == LexType.SYM__)
				pos++;

			String data = in.substring(start, pos);

			if (type == LexType.SPACE) {
				List<Integer> precs = new ArrayList<>();

				for (Token t : List.of(token0, detect()))
					if (t != null && t.operator != null)
						precs.add(t.operator.getPrecedence());

				if (!precs.isEmpty() && TermOp.TUPLE_.getPrecedence() < Collections.min(precs)) {
					token = new Token(LexType.OPER_, TermOp.TUPLE_);
					token.data = data;
				} else
					token = lex_();
			} else
				token.data = data;

			return token;
		} else
			return null;
	}

	private Token detect() {
		LexType type;
		Operator operator = Pair.first_(commandUtil.recognize(in, pos));

		if (pos < in.length()) {
			char ch = in.charAt(pos);

			if (operator != null)
				type = LexType.OPER_;
			else if (ch == '+' && pos + 4 <= in.length() && in.charAt(pos + 1) == '\'')
				type = LexType.CHAR__;
			else if (ch == '+' && pos + 2 <= in.length() && in.charAt(pos + 1) == 'x')
				type = LexType.HEX__;
			else if (ch == ' ')
				type = LexType.SPACE;
			else if (ch == '\'' || ch == '"')
				type = LexType.STR__;
			else if (ch == '(' || ch == '[' || ch == '{' //
					|| ch == ')' || ch == ']' || ch == '}' //
					|| ch == '`')
				type = LexType.SYM__;
			else
				type = LexType.ID___;
		} else
			type = null;

		return new Token(type, operator);
	}

}
