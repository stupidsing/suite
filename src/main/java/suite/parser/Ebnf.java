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

import suite.node.io.Escaper;
import suite.node.io.Operator.Assoc;
import suite.util.FunUtil;
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

	private String rootGrammarEntity;
	private Map<String, Grammar> grammarsByEntity = new HashMap<>();

	private Source<State> noResult = FunUtil.nullSource();

	private static boolean trace = false;

	private interface Grammar {
		public Source<State> p(Parse parse, State state);
	}

	private abstract class WrappingGrammar implements Grammar {
		public Grammar grammar;

		private WrappingGrammar(Grammar grammar) {
			this.grammar = grammar;
		}
	}

	private abstract class CompositeGrammar implements Grammar {
		public List<Grammar> grammars;

		public CompositeGrammar(List<Grammar> grammars) {
			this.grammars = grammars;
		}
	}

	private class OrGrammar extends CompositeGrammar {
		public OrGrammar(List<Grammar> grammars) {
			super(grammars);
		}

		public Source<State> p(Parse parse, State state) {
			return FunUtil.concat(FunUtil.map(childGrammar -> parse.parse(state, childGrammar), To.source(grammars)));
		}
	}

	private class JoinGrammar extends CompositeGrammar {
		public JoinGrammar(List<Grammar> grammars) {
			super(grammars);
		}

		public Source<State> p(Parse parse, State state) {
			Source<State> source = To.source(state);
			for (Grammar childGrammar : grammars)
				source = FunUtil.concat(FunUtil.map(st -> parse.parse(st, childGrammar), source));
			return source;
		}
	}

	private class RepeatGrammar extends WrappingGrammar {
		private boolean isAllowNone;

		private RepeatGrammar(Grammar grammar, boolean isAllowNone) {
			super(grammar);
			this.isAllowNone = isAllowNone;
		}

		public Source<State> p(Parse parse, State state) {
			Source<State> states = new Source<State>() {
				private State state_ = state;
				private Deque<Source<State>> sources = new ArrayDeque<>();

				public State source() {
					State state0 = state_;
					if (state0 != null) {
						sources.push(parse.parse(state0, grammar));
						while (!sources.isEmpty() && (state_ = sources.peek().source()) == null)
							sources.pop();
					}
					return state0;
				}
			};

			// Skips first if it is a '+'
			return isAllowNone || states.source() != null ? states : noResult;
		}
	}

	private class EntityGrammar implements Grammar {
		private String entity;

		private EntityGrammar(String entity) {
			this.entity = entity;
		}

		public Source<State> p(Parse parse, State state) {
			State state1 = deepen(state, entity);
			Source<State> states = parse.parse(state1, grammarsByEntity.get(entity));
			return FunUtil.map(st -> undeepen(st, state.depth), states);
		}
	}

	public class Node {
		private int start, end;
		public final String entity;
		public final List<Node> nodes = new ArrayList<>();

		public Node(String entity, int start) {
			this(entity, start, 0);
		}

		public Node(String entity, int start, int end) {
			this.entity = entity;
			this.start = start;
			this.end = end;
		}

		public String toString() {
			return entity + "@" + start + "-" + end + nodes;
		}

		public int getStart() {
			return start;
		}

		public int getEnd() {
			return end;
		}
	}

	private class Parse {
		private String in;
		private int length;
		private int errorPosition = 0;
		private String errorEntity;

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
							Node node = new Node(state_.entity, state_.pos);
							if (state_.entity != null)
								stack.peek().nodes.add(node);
							stack.push(node);
						}
					}

					return root.nodes.get(0);
				}

			throw new RuntimeException("Syntax error for entity " + errorEntity + " at " + findPosition(errorPosition));
		}

		private Source<State> parse(State state, Grammar grammar) {
			int pos = expectWhitespaces(state.pos);

			if (trace)
				LogUtil.info("parse(" + grammar + "): " + in.substring(pos));

			State state1 = new State(state, pos);
			Source<State> states = grammar.p(this, state1);

			if (states == noResult && state1.entity != null && pos >= errorPosition) {
				errorPosition = pos;
				errorEntity = state1.entity;
			}

			return states;
		}

		private Source<State> expect(State state, int end) {
			return state.pos < end ? To.source(new State(state, end)) : noResult;
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
			return Pair.of(row, col);
		}
	}

	private class State {
		private State previous;
		private int pos;

		private String entity;
		private int depth;

		private State(State previous, int pos) {
			this(previous, pos, previous.entity, previous.depth);
		}

		private State(State previous, int pos, String entity, int depth) {
			this.previous = previous;
			this.pos = pos;
			this.entity = entity;
			this.depth = depth;
		}
	}

	public Ebnf(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		String line;
		Pair<String, String> lr;

		while ((line = br.readLine()) != null)
			if (!line.isEmpty() && line.charAt(0) != '#' && (lr = Util.split2(line, " ::= ")) != null) {
				grammarsByEntity.put(lr.t0, parseGrammar(lr.t1));

				if (rootGrammarEntity == null)
					rootGrammarEntity = lr.t0;
			}

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
		else if (s.endsWith("?")) {
			Grammar grammar1 = parseGrammar(Util.substr(s, 0, -1));
			grammar = (parse, st) -> FunUtil.cons(st, parse.parse(st, grammar1));
		} else if (s.length() == 5 && s.charAt(0) == '[' && s.charAt(2) == '-' && s.charAt(4) == ']') {
			char start = s.charAt(1);
			char end = s.charAt(3);
			grammar = (parse, st) -> parse.expect(st, parse.expectCharRange(st.pos, start, end));
		} else if (s.startsWith("(") && s.endsWith(")"))
			grammar = parseGrammar(Util.substr(s, 1, -1));
		else if (s.startsWith("\"") && s.endsWith("\"")) {
			String token = Escaper.unescape(Util.substr(s, 1, -1), "\"");
			grammar = (parse, st) -> parse.expect(st, parse.expectString(st.pos, token));
		} else
			grammar = parseGrammarEntity(s);

		return grammar;
	}

	private List<Grammar> parseGrammars(List<String> list) {
		return To.list(FunUtil.iter(FunUtil.map(this::parseGrammar, To.source(list))));
	}

	private Grammar parseGrammarEntity(String entity) {
		Grammar grammar;
		if ((grammar = parseGrammarLiterals(entity)) == null)
			grammar = new EntityGrammar(entity);
		return grammar;
	}

	private Grammar parseGrammarLiterals(String entity) {
		Grammar grammar;

		if (Util.stringEquals(entity, "<EOF>"))
			grammar = (parse, st) -> st.pos == parse.length ? To.source(st) : noResult;
		else if (Util.stringEquals(entity, "<CHARACTER_LITERAL>"))
			grammar = (parse, st) -> parse.expect(st, parse.expectCharLiteral(st.pos));
		else if (Util.stringEquals(entity, "<FLOATING_POINT_LITERAL>"))
			grammar = (parse, st) -> parse.expect(st, parse.expectFloatLiteral(st.pos));
		else if (Util.stringEquals(entity, "<IDENTIFIER>"))
			grammar = (parse, st) -> parse.expect(st, parse.expectIdentifier(st.pos));
		else if (Util.stringEquals(entity, "<INTEGER_LITERAL>"))
			grammar = (parse, st) -> parse.expect(st, parse.expectIntegerLiteral(st.pos));
		else if (Util.stringEquals(entity, "<STRING_LITERAL>"))
			grammar = (parse, st) -> parse.expect(st, parse.expectStringLiteral(st.pos));
		else if (Util.stringEquals(entity, "<FAIL>"))
			grammar = (parse, st) -> noResult;
		else
			grammar = null;

		if (grammar != null)
			return (parse, st) -> {
				State st1 = deepen(st, entity);
				return FunUtil.map(st_ -> undeepen(st_, st.depth), grammar.p(parse, st1));
			};
		else
			return null;
	}

	public Node parse(String s) {
		return parse(s, 0, rootGrammarEntity);
	}

	public Node parse(String s, int end, String entity) {
		return new Parse(s).parse(end, parseGrammar(entity));
	}

	private void reduceHeadRecursion() {
		for (Entry<String, Grammar> entry : new ArrayList<>(grammarsByEntity.entrySet())) {
			String entity = entry.getKey();
			Grammar grammar = entry.getValue();
			grammarsByEntity.put(entity, reduceHeadRecursion(entity, grammar));
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
	private Grammar reduceHeadRecursion(String entity, Grammar grammar0) {
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
				String tempb = entity + "-Head";
				String tempc = entity + "-Tail";
				grammarsByEntity.put(tempb, new OrGrammar(listb));
				grammarsByEntity.put(tempc, new OrGrammar(listc));
				Grammar tempbGrammar = parseGrammarEntity(tempb);
				Grammar tempcGrammar = parseGrammarEntity(tempc);
				grammar1 = new JoinGrammar(Arrays.asList(tempbGrammar, new RepeatGrammar(tempcGrammar, true)));
			} else
				grammar1 = grammar;
		} else
			grammar1 = grammar;

		return grammar1;
	}

	private Grammar lookup(Grammar grammar) {
		String entity = grammar instanceof EntityGrammar ? ((EntityGrammar) grammar).entity : null;
		Grammar grammar1 = entity != null ? grammarsByEntity.get(entity) : null;
		return grammar1 != null ? grammar1 : grammar;
	}

	private State deepen(State state, String entity) {
		return new State(state, state.pos, entity, state.depth + 1);
	}

	private State undeepen(State state, int depth) {
		return new State(state, state.pos, null, depth);
	}

	private List<Grammar> children(CompositeGrammar grammar) {
		return grammar.grammars;
	}

}
