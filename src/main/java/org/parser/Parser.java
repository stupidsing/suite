package org.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.util.Util.IoProcess;

public class Parser {

	public class InputBuffer {
		String buffer;
		int position;
	}

	public interface Node {
	}

	public class Token implements Node {
		public String preSpace;
		public String token;

		public Token(String preSpace, String token) {
			this.preSpace = preSpace;
			this.token = token;
		}
	}

	public enum Type {
		IF, GT, GE, LT, LE, Concat, Add, Sub, Mul, Div, Paren, Identifier
	}

	public class Construct implements Node {
		Type type;
		List<Node> nodes;

		public Construct() {
			this(null);
		}

		public Construct(Type type) {
			this(type, new ArrayList<Node>());
		}

		public Construct(Type type, List<Node> nodes) {
			this.type = type;
			this.nodes = nodes;
		}
	}

	public Construct parseIf(InputBuffer input) {
		Construct construct = new Construct(Type.IF);
		expect(input, "if", construct);
		construct.nodes.add(parseExpression(input));
		expect(input, "then", construct);
		construct.nodes.add(parseExpression(input));
		if (match(input, "else", construct) != null)
			construct.nodes.add(parseExpression(input));
		return construct;
	}

	private class Operator {
		Type type;
		String name;
		boolean isRightPrecedence;

		public Operator(Type type, String name, boolean isRightPrecedence) {
			this.type = type;
			this.name = name;
			this.isRightPrecedence = isRightPrecedence;
		}
	}

	private Operator operators[] = { //
	new Operator(Type.GT, ">", false), //
			new Operator(Type.GE, ">=", false), //
			new Operator(Type.LT, "<", false), //
			new Operator(Type.LE, "<=", false), //
			new Operator(Type.Concat, "||", false), //
			new Operator(Type.Add, "+", false), //
			new Operator(Type.Sub, "-", true), //
			new Operator(Type.Mul, "*", false), //
			new Operator(Type.Div, "/", true), //
	};

	public Node parseExpression(InputBuffer input) {
		return parseExpression(input, 0);
	}

	public Node parseExpression(InputBuffer input, int n) {
		Node node;
		Construct construct;

		if (n < operators.length) {
			Operator operator = operators[n];
			node = parseExpression(input, n + 1);
			Token token;

			if (!operator.isRightPrecedence) {
				if ((token = match(input, operator.name)) != null)
					node = new Construct(operator.type, Arrays.asList(node,
							token, parseExpression(input, n)));
			} else {
				Construct rightest = null;
				Node lastNode = node;

				while ((token = match(input, operator.name)) != null) {
					Construct subTree = new Construct(operator.type, Arrays
							.asList(lastNode, token, null));
					if (rightest != null)
						rightest.nodes.set(2, subTree);
					else
						node = subTree;

					rightest = subTree;
					lastNode = parseExpression(input, n + 1);
				}

				if (rightest != null)
					rightest.nodes.set(2, lastNode);
			}

			return node;
		} else if ((node = matchIfWord(input, new Construct(Type.Identifier))) != null)
			;
		else if ((node = construct = match(input, Type.Paren, "(")) != null) {
			construct.nodes.add(parseExpression(input));
			expect(input, ")", construct);
		} else
			node = getToken(input);

		return node;
	}

	private void expect(InputBuffer input, String toMatch, Construct construct) {
		Token token = match(input, toMatch, construct);
		if (token == null)
			throw new RuntimeException("EXPECT " + toMatch);
	}

	private Construct match(InputBuffer input, Type type, String toMatch) {
		Construct construct = new Construct(type);
		return match(input, toMatch, construct) != null ? construct : null;
	}

	private Token match(InputBuffer input, String toMatch) {
		return match(input, toMatch, new Construct());
	}

	private Token match(InputBuffer input, final String toMatch,
			Construct construct) {
		return match(input, new IoProcess<String, Boolean, RuntimeException>() {
			public Boolean perform(String s) throws RuntimeException {
				return s.equalsIgnoreCase(toMatch);
			}
		}, construct);
	}

	private Token matchIfWord(InputBuffer input, Construct construct) {
		return match(input, new IoProcess<String, Boolean, RuntimeException>() {
			public Boolean perform(String s) throws RuntimeException {
				return isWord(s);
			}
		}, construct);
	}

	private Token match(InputBuffer input,
			IoProcess<String, Boolean, RuntimeException> check,
			Construct construct) {
		int preIndex = input.position;
		Token token = getToken(input);
		if (check.perform(token.token)) {
			construct.nodes.add(token);
			return token;
		} else {
			input.position = preIndex; // Pretend as if nothing happened
			return null;
		}
	}

	private Token getToken(InputBuffer input) {
		String buffer = input.buffer;
		int begin = input.position, start = begin;
		int length = buffer.length();

		while (start < length) {
			if (Character.isWhitespace(buffer.charAt(start))) {
				start++;
				continue;
			}

			if (start + 1 < length) {
				char ch = buffer.charAt(start), ch1 = buffer.charAt(start + 1);

				String endTag = (ch == '-' && ch1 == '-') ? "\n"
						: (ch == '/' && ch1 == '*') ? "*/" //
								: null;

				if (endTag != null) {
					int pos = buffer.indexOf(endTag, start + 2);
					start = (pos != -1) ? pos + endTag.length() : length;
					continue;
				}
			}

			break;
		}

		int end = start;
		if (end < length) {
			char ch = buffer.charAt(end);

			if (Character.isJavaIdentifierStart(ch))
				while (end < length
						&& Character.isJavaIdentifierPart(buffer.charAt(end)))
					end++;
			else if (Character.isDigit(buffer.charAt(end)))
				while (end < length && Character.isDigit(buffer.charAt(end)))
					end++;
			else if (ch == '\'' || ch == '"')
				while (buffer.charAt(end) == ch)
					end = buffer.indexOf(ch, end + 1) + 1; // Check for '' or ""
			else if (++end < length) { // Suck one or two character(s)
				int ch1 = buffer.indexOf(end);
				if (ch == '>' && ch1 == '=' //
						|| ch == '<' && ch1 == '=' //
						|| ch == '<' && ch1 == '>' //
						|| ch == '>' && ch1 == '<' //
						|| ch == '!' && ch1 == '=' //
						|| ch == '|' && ch1 == '|' //
				)
					end++;
			}
		}

		input.position = end;
		return new Token(buffer.substring(begin, start), buffer.substring(
				start, end));
	}

	private boolean isWord(String s) {
		if (!Character.isJavaIdentifierStart(s.charAt(0)))
			return false;
		for (char c : s.toCharArray())
			if (!Character.isJavaIdentifierPart(c))
				return false;
		return true;
	}

}
