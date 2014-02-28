package suite.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.node.io.Operator.Assoc;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;
import suite.util.Pair;
import suite.util.ParseUtil;
import suite.util.To;
import suite.util.Util;

/**
 * Parser for Backus-Naur form grammars.
 * 
 * @author ywsing
 */
public class Ebnf {

	private String rootGrammarName;
	private Map<String, Grammar> grammars = new HashMap<>();

	private static final boolean trace = false;

	private interface Grammar {
	}

	private abstract class WrappingGrammar implements Grammar {
		private Grammar grammar;

		private WrappingGrammar(Grammar grammar) {
			this.grammar = grammar;
		}
	}

	private abstract class CompositeGrammar implements Grammar {
		private List<Grammar> grammars;

		public CompositeGrammar(List<Grammar> grammars) {
			this.grammars = grammars;
		}
	}

	private class OrGrammar extends CompositeGrammar {
		public OrGrammar(List<Grammar> grammars) {
			super(grammars);
		}
	}

	private class JoinGrammar extends CompositeGrammar {
		public JoinGrammar(List<Grammar> grammars) {
			super(grammars);
		}
	}

	private class OptionalGrammar extends WrappingGrammar {
		private OptionalGrammar(Grammar grammar) {
			super(grammar);
		}
	}

	private class RepeatGrammar extends WrappingGrammar {
		private boolean isAllowNone;

		private RepeatGrammar(Grammar grammar, boolean isAllowNone) {
			super(grammar);
			this.isAllowNone = isAllowNone;
		}
	}

	private class NamedGrammar implements Grammar {
		private String name;

		private NamedGrammar(String name) {
			this.name = name;
		}
	}

	private class TokenGrammar implements Grammar {
		private String token;

		private TokenGrammar(String token) {
			this.token = token;
		}
	}

	private class CharRangeGrammar implements Grammar {
		private char start, end;

		public CharRangeGrammar(char start, char end) {
			this.start = start;
			this.end = end;
		}
	}

	public class Node {
		private String name;
		private int start, end;
		private List<Node> nodes = new ArrayList<>();

		public Node(String name, int start) {
			this(name, start, 0);
		}

		public Node(String name, int start, int end) {
			this.name = name;
			this.start = start;
			this.end = end;
		}

		public String toString() {
			return name + "@" + start + "-" + end + nodes;
		}

		public String toPrettyPrint() {
			StringBuilder sb = new StringBuilder();
			prettyPrint("", sb);
			return sb.toString();
		}

		private void prettyPrint(String indent, StringBuilder sb) {
			String indent1 = indent + "  ";
			sb.append(indent + name + "@" + start + "-" + end + "\n");
			for (Node node : nodes)
				node.prettyPrint(indent1, sb);
		}

		public String getEntity() {
			return name;
		}

		public int getEnd() {
			return start;
		}
	}

	public Ebnf(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		String line;
		Pair<String, String> lr;

		while ((line = br.readLine()) != null)
			if (!line.isEmpty() && line.charAt(0) != '#' && (lr = Util.split2(line, " ::= ")) != null) {
				grammars.put(lr.t0, parseGrammar(lr.t1));

				if (rootGrammarName == null)
					rootGrammarName = lr.t0;
			}

		verify();
		reduceHeadRecursion();
	}

	private Grammar parseGrammar(String s) {
		Grammar grammar;
		List<String> list;
		s = s.trim();

		if ((list = ParseUtil.searchn(s, " | ", Assoc.RIGHT)).size() > 1)
			grammar = new OrGrammar(parseGrammars(list));
		else if ((list = ParseUtil.searchn(s, " ", Assoc.RIGHT)).size() > 1)
			grammar = new JoinGrammar(parseGrammars(list));
		else if (s.endsWith("*"))
			grammar = new RepeatGrammar(parseGrammar(Util.substr(s, 0, -1)), true);
		else if (s.endsWith("+"))
			grammar = new RepeatGrammar(parseGrammar(Util.substr(s, 0, -1)), false);
		else if (s.endsWith("?"))
			grammar = new OptionalGrammar(parseGrammar(Util.substr(s, 0, -1)));
		else if (s.length() == 5 && s.charAt(0) == '[' && s.charAt(2) == '-' && s.charAt(4) == ']')
			grammar = new CharRangeGrammar(s.charAt(1), s.charAt(3));
		else if (s.startsWith("(") && s.endsWith(")"))
			grammar = parseGrammar(Util.substr(s, 1, -1));
		else if (s.startsWith("\"") && s.endsWith("\""))
			grammar = new TokenGrammar(Util.substr(s, 1, -1));
		else
			grammar = new NamedGrammar(s);

		return grammar;
	}

	private List<Grammar> parseGrammars(List<String> list) {
		return To.list(FunUtil.iter(FunUtil.map(new Fun<String, Grammar>() {
			public Grammar apply(String s) {
				return parseGrammar(s);
			}
		}, To.source(list))));
	}

	public Node parse(String s) {
		return parse(s, 0, rootGrammarName);
	}

	public Node parse(String s, int end, String name) {
		return new Parse(s).parse(end, parseGrammar(name));
	}

	private class Parse {
		private String in;
		private int length;
		private int errorPosition = 0;
		private String errorName;

		private class State {
			private State previous;
			private int pos;

			private String name;
			private int depth;

			private State(State previous, int pos) {
				this(previous, pos, previous.name, previous.depth);
			}

			private State(State previous, int pos, String name, int depth) {
				this.previous = previous;
				this.pos = pos;
				this.name = name;
				this.depth = depth;
			}
		}

		private final Source<State> noResult = FunUtil.nullSource();

		private Parse(String in) {
			this.in = in;
			length = in.length();
		}

		private Node parse(int pos, Grammar grammar) {
			State initialState = new State(null, pos, null, 1);
			Source<State> source = parse(initialState, grammar);
			State state;

			while ((state = source.source()) != null)
				if (state.pos == length) {
					Deque<State> states = new ArrayDeque<>();

					while (state != null) {
						states.addFirst(state);
						state = state.previous;
					}

					Node root = new Node(null, 0);

					Deque<Node> stack = new ArrayDeque<>();
					stack.push(root);

					for (State state_ : states) {
						if (state_.depth < stack.size())
							stack.pop().end = state_.pos;

						if (state_.depth > stack.size()) {
							Node node = new Node(state_.name, state_.pos);

							if (state_.name != null)
								stack.peek().nodes.add(node);

							stack.push(node);
						}
					}

					return root.nodes.get(0);
				}

			throw new RuntimeException("Syntax error for entity " + errorName + " at " + findPosition(errorPosition));
		}

		private Source<State> parse(State state, Grammar grammar) {
			int pos = expectWhitespaces(state.pos);

			if (trace)
				LogUtil.info("parse(" + grammar + "): " + in.substring(pos));

			State state1 = new State(state, pos);
			Source<State> states;

			if (grammar instanceof NamedGrammar)
				states = parseNamedGrammar(state1, (NamedGrammar) grammar);
			else if (grammar instanceof OrGrammar)
				states = parseOr(state1, (OrGrammar) grammar);
			else if (grammar instanceof JoinGrammar)
				states = parseJoin(state1, (JoinGrammar) grammar);
			else if (grammar instanceof OptionalGrammar)
				states = FunUtil.cons(state1, parse(state1, child((OptionalGrammar) grammar)));
			else if (grammar instanceof RepeatGrammar)
				states = parseRepeat(state1, (RepeatGrammar) grammar);
			else if (grammar instanceof TokenGrammar)
				states = parseToken(state1, (TokenGrammar) grammar);
			else if (grammar instanceof CharRangeGrammar)
				states = parseCharRange(pos, state1, (CharRangeGrammar) grammar);
			else
				states = noResult;

			if (states == noResult && state1.name != null && pos >= errorPosition) {
				errorPosition = pos;
				errorName = state1.name;
			}

			return states;
		}

		private Source<State> parseNamedGrammar(State state, NamedGrammar namedGrammar) {
			Source<State> states;
			String name = namedGrammar.name;
			final int depth = state.depth;
			int pos = state.pos;
			State state1 = deepen(state, name);

			if (Util.stringEquals(name, "<EOF>"))
				states = state1.pos == length ? To.source(state1) : noResult;
			else if (Util.stringEquals(name, "<CHARACTER_LITERAL>"))
				states = parseExpect(state1, expectCharLiteral(pos));
			else if (Util.stringEquals(name, "<FLOATING_POINT_LITERAL>"))
				states = parseExpect(state1, expectFloatLiteral(pos));
			else if (Util.stringEquals(name, "<IDENTIFIER>"))
				states = parseExpect(state1, expectIdentifier(pos));
			else if (Util.stringEquals(name, "<INTEGER_LITERAL>"))
				states = parseExpect(state1, expectIntegerLiteral(pos));
			else if (Util.stringEquals(name, "<STRING_LITERAL>"))
				states = parseExpect(state1, expectStringLiteral(pos));
			else
				states = parse(state1, grammars.get(name));

			return FunUtil.map(new Fun<State, State>() {
				public State apply(State state) {
					return undeepen(state, depth);
				}
			}, states);
		}

		private Source<State> parseOr(final State state, final OrGrammar grammar) {
			return FunUtil.concat(FunUtil.map(new Fun<Grammar, Source<State>>() {
				public Source<State> apply(Grammar childGrammar) {
					return parse(state, childGrammar);
				}
			}, To.source(children(grammar))));
		}

		private Source<State> parseJoin(State state, final JoinGrammar grammar) {
			Source<State> source = To.source(state);

			for (final Grammar childGrammar : children(grammar))
				source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
					public Source<State> apply(State state) {
						return parse(state, childGrammar);
					}
				}, source));

			return source;
		}

		private Source<State> parseRepeat(State state, RepeatGrammar grammar) {
			Source<State> states = parseRepeat(state, child(grammar));

			// Skips first if it is a '+'
			return grammar.isAllowNone || states.source() != null ? states : noResult;
		}

		private Source<State> parseRepeat(final State state, final Grammar grammar) {
			return new Source<State>() {
				private State state_ = state;
				private Deque<Source<State>> sources = new ArrayDeque<>();

				public State source() {
					State state0 = state_;
					if (state0 != null) {
						sources.push(parse(state0, grammar));

						while (!sources.isEmpty() && (state_ = sources.peek().source()) == null)
							sources.pop();
					}
					return state0;
				}
			};
		}

		private Source<State> parseToken(State state, TokenGrammar grammar) {
			return parseExpect(state, expectString(state.pos, grammar.token));
		}

		private Source<State> parseCharRange(int pos, State state, CharRangeGrammar grammar) {
			return parseExpect(state, expectCharRange(state.pos, grammar.start, grammar.end));
		}

		private Source<State> parseExpect(State state, int end) {
			return state.pos < end ? To.source(new State(state, end)) : noResult;
		}

		private State deepen(State state, String name) {
			return new State(state, state.pos, name, state.depth + 1);
		}

		private State undeepen(State state, int depth) {
			return new State(state, state.pos, null, depth);
		}

		private int expectCharLiteral(int start) {
			int pos = start, end;
			if (pos < length && in.charAt(pos) == '\'') {
				pos++;
				if (pos < length && in.charAt(pos) == '\\')
					pos++;
				if (pos < length)
					pos++;
				if (pos < length && in.charAt(pos) == '\'') {
					pos++;
					end = pos;
				} else
					end = start;
			} else
				end = start;
			return end;
		}

		private int expectFloatLiteral(int start) {
			int pos = start;
			pos = expectIntegerLiteral(pos);
			if (pos < length && in.charAt(pos) == '.') {
				pos++;
				pos = expectIntegerLiteral(pos);
			}
			if (pos < length && "fd".indexOf(in.charAt(pos)) >= 0)
				pos++;
			return pos;
		}

		private int expectIdentifier(int start) {
			int pos = start;
			if (pos < length && Character.isJavaIdentifierStart(in.charAt(pos))) {
				pos++;
				while (pos < length && Character.isJavaIdentifierPart(in.charAt(pos)))
					pos++;
			}
			return pos;
		}

		private int expectIntegerLiteral(int start) {
			int pos = start;
			while (pos < length && Character.isDigit(in.charAt(pos)))
				pos++;
			if (pos < length && in.charAt(pos) == 'l')
				pos++;
			return pos;
		}

		private int expectStringLiteral(int start) {
			int pos = start, end;
			if (pos < length && in.charAt(pos) == '"') {
				pos++;
				char c;

				while (pos < length && (c = in.charAt(pos)) != '"') {
					pos++;
					if (pos < length && c == '\\')
						pos++;
				}

				if (pos < length && in.charAt(pos) == '"') {
					pos++;
					end = pos;
				} else
					end = start;
			} else
				end = start;
			return end;
		}

		private int expectString(int start, String s) {
			return start + (in.startsWith(s, start) ? s.length() : 0);
		}

		private int expectCharRange(int start, char s, char e) {
			int end;
			if (start < length) {
				char ch = in.charAt(start);
				end = start + (s <= ch && ch <= e ? 1 : 0);
			} else
				end = start;
			return end;
		}

		private int expectWhitespaces(int start) {
			int pos = start, pos1;
			while ((pos1 = expectWhitespace(pos)) > pos)
				pos = pos1;
			return pos;
		}

		private int expectWhitespace(int pos) {
			while (pos < length && Character.isWhitespace(in.charAt(pos)))
				pos++;
			pos = expectComment(pos, "/*", "*/");
			pos = expectComment(pos, "//", "\n");
			return pos;
		}

		private int expectComment(int start, String sm, String em) {
			int sl = sm.length(), el = em.length();
			int pos = start, end;
			if (pos < length && Util.stringEquals(Util.substr(in, pos, pos + sl), sm)) {
				pos += 2;
				while (pos < length && !Util.stringEquals(Util.substr(in, pos, pos + el), em))
					pos++;
				if (pos < length && Util.stringEquals(Util.substr(in, pos, pos + el), em)) {
					pos += 2;
					end = pos;
				} else
					end = start;
			} else
				end = start;
			return end;
		}

		private Pair<Integer, Integer> findPosition(int position) {
			int row = 1, col = 1;

			for (int i = 0; i < position; i++) {
				col++;

				if (in.charAt(i) == 10) {
					row++;
					col = 1;
				}
			}

			Pair<Integer, Integer> pos = Pair.create(row, col);
			return pos;
		}
	}

	private void verify() {
		for (Grammar grammar : grammars.values())
			verify(grammar);
	}

	private void verify(Grammar grammar) {
		if (grammar instanceof NamedGrammar) {
			String name = ((NamedGrammar) grammar).name;
			boolean isNameExists = name.startsWith("<") && name.endsWith(">") || grammars.containsKey(name);

			if (!isNameExists)
				throw new RuntimeException("Grammar " + name + " not exist");
		} else if (grammar instanceof WrappingGrammar)
			verify(child((WrappingGrammar) grammar));
		else if (grammar instanceof CompositeGrammar)
			for (Grammar child : children((CompositeGrammar) grammar))
				verify(child);
	}

	private void reduceHeadRecursion() {
		for (Entry<String, Grammar> entry : new ArrayList<>(grammars.entrySet())) {
			String name = entry.getKey();
			Grammar grammar = entry.getValue();
			grammars.put(name, reduceHeadRecursion(name, grammar));
		}
	}

	/**
	 * Transform head-recursion rule as follows:
	 * 
	 * A = B0 | B1 | ... | Bm | A C0 | A C1 | ... | A Cn
	 * 
	 * become two rules
	 * 
	 * A = tempB tempC*
	 * 
	 * tempB = B0 | B1 | ... | Bm
	 * 
	 * tempC = C0 | C1 | ... | Cn
	 */
	private Grammar reduceHeadRecursion(String name, Grammar grammar0) {
		Grammar grammar = lookup(grammar0);
		OrGrammar orGrammar = grammar instanceof OrGrammar ? (OrGrammar) grammar : null;
		Grammar grammar1;

		if (orGrammar != null) {
			List<Grammar> listb = new ArrayList<>();
			List<Grammar> listc = new ArrayList<>();

			for (Grammar childGrammar : children(orGrammar)) {
				if (childGrammar instanceof JoinGrammar) {
					List<Grammar> grammars = children((JoinGrammar) childGrammar);

					if (lookup(grammars.get(0)) == grammar) {
						listc.add(new JoinGrammar(Util.right(grammars, 1)));
						continue;
					}
				}

				listb.add(childGrammar);
			}

			if (!listc.isEmpty()) {
				String tempb = name + "-Head";
				String tempc = name + "-Tail";
				grammars.put(tempb, new OrGrammar(listb));
				grammars.put(tempc, new OrGrammar(listc));
				NamedGrammar tempbGrammar = new NamedGrammar(tempb);
				NamedGrammar tempcGrammar = new NamedGrammar(tempc);
				grammar1 = new JoinGrammar(Arrays.asList(tempbGrammar, new RepeatGrammar(tempcGrammar, true)));
			} else
				grammar1 = grammar;
		} else
			grammar1 = grammar;

		return grammar1;
	}

	private Grammar lookup(Grammar grammar) {
		String name = grammar instanceof NamedGrammar ? ((NamedGrammar) grammar).name : null;
		Grammar grammar1 = name != null ? grammars.get(name) : null;
		return grammar1 != null ? grammar1 : grammar;
	}

	private Grammar child(WrappingGrammar grammar) {
		return grammar.grammar;
	}

	private List<Grammar> children(CompositeGrammar grammar) {
		return grammar.grammars;
	}

}
