package suite.ebnf;

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

import suite.ebnf.EbnfExpect.Expect;
import suite.node.io.Escaper;
import suite.node.io.Operator.Assoc;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;
import suite.util.Pair;
import suite.util.ParseUtil;
import suite.util.Util;

/**
 * Parser for Backus-Naur form grammars.
 *
 * TODO cyclic problem:
 * 
 * primary-no-array-creation-expression ::= simple-name | member-access
 * 
 * member-access ::= primary-expression "." identifier (type-argument-list)?
 * 
 * primary-expression ::= primary-no-array-creation-expression
 * 
 * @author ywsing
 */
public class Ebnf {

	private String rootGrammarEntity;
	private Map<String, Grammar> grammarsByEntity = new HashMap<>();

	private EbnfExpect expect = new EbnfExpect();
	private Streamlet<State> noResult = Read.empty();

	private static boolean trace = false;

	private interface Grammar {
		public Streamlet<State> p(Parse parse, State state);
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

		public Streamlet<State> p(Parse parse, State state) {
			return Read.from(grammars).concatMap(childGrammar -> parse.parse(state, childGrammar));
		}
	}

	private class JoinGrammar extends CompositeGrammar {
		public JoinGrammar(List<Grammar> grammars) {
			super(grammars);
		}

		public Streamlet<State> p(Parse parse, State state) {
			Streamlet<State> st = Read.from(state);
			for (Grammar childGrammar : grammars)
				st = st.concatMap(st_ -> parse.parse(st_, childGrammar));
			return st;
		}
	}

	private class RepeatGrammar extends WrappingGrammar {
		private boolean isAllowNone;

		private RepeatGrammar(Grammar grammar, boolean isAllowNone) {
			super(grammar);
			this.isAllowNone = isAllowNone;
		}

		public Streamlet<State> p(Parse parse, State state) {
			Streamlet<State> states = Read.from(new Source<State>() {
				private State state_ = state;
				private Deque<Streamlet<State>> streamlets = new ArrayDeque<>();

				public State source() {
					State state0 = state_;
					if (state0 != null) {
						streamlets.push(parse.parse(state0, grammar));
						while (!streamlets.isEmpty() && (state_ = streamlets.peek().next()) == null)
							streamlets.pop();
					}
					return state0;
				}
			});

			// Skips first if it is a '+'
			return isAllowNone || states.next() != null ? states : noResult;
		}
	}

	private class EntityGrammar implements Grammar {
		private String entity;

		private EntityGrammar(String entity) {
			this.entity = entity;
		}

		public Streamlet<State> p(Parse parse, State state) {
			boolean isRecurse = false;
			State prevState = state;

			while (!isRecurse && prevState != null && prevState.pos == state.pos) {
				isRecurse |= Util.stringEquals(prevState.entity, entity);
				prevState = prevState.previous;
			}

			if (!isRecurse) {
				Grammar grammar = grammarsByEntity.get(entity);
				if (grammar != null) {
					State state1 = deepen(state, entity);
					Streamlet<State> states = parse.parse(state1, grammar);
					return states.map(st -> undeepen(st, state.depth));
				} else
					throw new RuntimeException("Entity " + entity + " not found");
			} else
				return noResult;
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
			Streamlet<State> st = parse(initialState, grammar);
			State state;

			while ((state = st.next()) != null)
				if (state.pos == length) {
					Deque<State> states = new ArrayDeque<>();

					while (state != null) {
						states.addFirst(state);
						state = state.previous;
					}

					Node root = new Node(null, 0);

					Deque<Node> stack = new ArrayDeque<>();
					stack.push(root);

					for (State state_ : states)
						if (state_.depth < stack.size())
							stack.pop().end = state_.pos;
						else if (state_.depth > stack.size()) {
							Node node = new Node(state_.entity, state_.pos);
							if (state_.entity != null)
								stack.peek().nodes.add(node);
							stack.push(node);
						}

					return root.nodes.get(0);
				}

			return null;
		}

		private Streamlet<State> parse(State state, Grammar grammar) {
			int pos = expect.expectWhitespaces(in, length, state.pos);

			if (trace)
				LogUtil.info("parse(" + grammar + "): " + in.substring(pos));

			State state1 = new State(state, pos);
			Streamlet<State> states = grammar.p(this, state1);

			if (states == noResult && state1.entity != null && pos >= errorPosition) {
				errorPosition = pos;
				errorEntity = state1.entity;
			}

			return states;
		}

		private Streamlet<State> expect(State state, Expect expect, int pos) {
			int end = expect.expect(in, length, pos);
			return state.pos < end ? Read.from(new State(state, end)) : noResult;
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
		Read.lines(reader) //
				.filter(line -> !line.isEmpty() && !line.startsWith("#")) //
				.map(line -> line.replace('\t', ' ')) //
				.split(line -> !line.startsWith(" ")) //
				.map(st -> st.fold("", String::concat)) //
				.map(line -> Util.split2(line, " ::= ")) //
				.filter(lr -> lr != null) //
				.foreach(lr -> {
					grammarsByEntity.put(lr.t0, parseGrammar(lr.t1));
					if (rootGrammarEntity == null)
						rootGrammarEntity = lr.t0;
				});

		reduceHeadRecursion();
	}

	private Grammar parseGrammar(String s) {
		Grammar grammar;
		List<String> list;
		Pair<String, String> pair;
		s = s.trim();

		if ((list = ParseUtil.searchn(s, " | ", Assoc.RIGHT)).size() > 1)
			grammar = new OrGrammar(parseGrammars(list));
		else if ((pair = ParseUtil.search(s, " /except/ ", Assoc.RIGHT)) != null) {
			Grammar grammar0 = parseGrammar(pair.t0);
			Grammar grammar1 = parseGrammar(pair.t1);
			return (parse, st) -> grammar0.p(parse, st).filter(st1 -> {
				String in1 = parse.in.substring(st.pos, st1.pos);
				return grammar1.p(new Parse(in1), new State(null, 0, null, 1)).count() == 0;
			});
		} else if ((list = ParseUtil.searchn(s, " ", Assoc.RIGHT)).size() > 1)
			grammar = new JoinGrammar(parseGrammars(list));
		else if (s.endsWith("*"))
			grammar = new RepeatGrammar(parseGrammar(Util.substr(s, 0, -1)), true);
		else if (s.endsWith("+"))
			grammar = new RepeatGrammar(parseGrammar(Util.substr(s, 0, -1)), false);
		else if (s.endsWith("?")) {
			Grammar grammar1 = parseGrammar(Util.substr(s, 0, -1));
			grammar = (parse, st) -> parse.parse(st, grammar1).cons(st);
		} else if (s.length() == 5 && s.charAt(0) == '[' && s.charAt(2) == '-' && s.charAt(4) == ']') {
			Expect e = expect.expectCharRange(s.charAt(1), s.charAt(3));
			grammar = (parse, st) -> parse.expect(st, e, st.pos);
		} else if (s.startsWith("(") && s.endsWith(")"))
			grammar = parseGrammar(Util.substr(s, 1, -1));
		else if (s.startsWith("\"") && s.endsWith("\"")) {
			String token = Escaper.unescape(Util.substr(s, 1, -1), "\"");
			Expect e = expect.expectString(token);
			grammar = (parse, st) -> parse.expect(st, e, st.pos);
		} else
			grammar = parseGrammarEntity(s);

		return grammar;
	}

	private List<Grammar> parseGrammars(List<String> list) {
		return Read.from(list).map(this::parseGrammar).toList();
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
			grammar = (parse, st) -> st.pos == parse.length ? Read.from(st) : noResult;
		else if (Util.stringEquals(entity, "<CHARACTER>"))
			grammar = (parse, st) -> parse.expect(st, (in, length, start) -> Math.min(start + 1, length), st.pos);
		else if (Util.stringEquals(entity, "<CHARACTER_LITERAL>"))
			grammar = (parse, st) -> parse.expect(st, expect.expectCharLiteral, st.pos);
		else if (Util.stringEquals(entity, "<FLOATING_POINT_LITERAL>"))
			grammar = (parse, st) -> parse.expect(st, expect.expectRealLiteral, st.pos);
		else if (Util.stringEquals(entity, "<IDENTIFIER>"))
			grammar = (parse, st) -> parse.expect(st, expect.expectIdentifier, st.pos);
		else if (entity.startsWith("<IGNORE:") && entity.endsWith(">"))
			grammar = (parse, st) -> noResult;
		else if (Util.stringEquals(entity, "<INTEGER_LITERAL>"))
			grammar = (parse, st) -> parse.expect(st, expect.expectIntegerLiteral, st.pos);
		else if (Util.stringEquals(entity, "<STRING_LITERAL>"))
			grammar = (parse, st) -> parse.expect(st, expect.expectStringLiteral, st.pos);
		else if (entity.startsWith("<UNICODE_CLASS:") && entity.endsWith(">")) {
			Expect e = expect.expectUnicodeClass(entity.substring(4, entity.length() - 1));
			grammar = (parse, st) -> parse.expect(st, e, st.pos);
		} else
			grammar = null;

		if (grammar != null)
			return (parse, st) -> {
				State st1 = deepen(st, entity);
				return grammar.p(parse, st1).map(st_ -> undeepen(st_, st.depth));
			};
		else
			return null;
	}

	public Node parse(String s) {
		return parse(s, rootGrammarEntity);
	}

	public Node parse(String s, String entity) {
		Parse parse = new Parse(s);
		Node node = parse.parse(0, parseGrammar(entity));
		if (node != null)
			return node;
		else {
			Pair<Integer, Integer> pos = parse.findPosition(parse.errorPosition);
			throw new RuntimeException("Syntax error for entity " + parse.errorEntity + " at " + pos);
		}

	}

	public Node check(String s, String entity) {
		return new Parse(s).parse(0, parseGrammar(entity));
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
