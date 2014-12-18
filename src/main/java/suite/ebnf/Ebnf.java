package suite.ebnf;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.ebnf.EbnfExpect.Expect;
import suite.ebnf.EbnfNode.EbnfType;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;
import suite.util.Pair;
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

	private String rootEntity;
	private Map<String, Grammar> grammarsByEntity;

	private EbnfBreakdown breakdown = new EbnfBreakdown();
	private EbnfExpect expect = new EbnfExpect();
	private Streamlet<State> noResult = Read.empty();

	private static boolean trace = false;

	private interface Grammar {
		public Streamlet<State> p(Parse parse, State state);
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
				if (expect.expectWhitespaces(in, length, state.pos) == length) {
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
			if (trace)
				LogUtil.info("parse(" + grammar + "): " + in.substring(state.pos));

			Streamlet<State> states = grammar.p(this, state);

			if (states == noResult && state.entity != null && state.pos >= errorPosition) {
				errorPosition = state.pos;
				errorEntity = state.entity;
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
		List<Pair<String, String>> pairs = Read.lines(reader) //
				.filter(line -> !line.isEmpty() && !line.startsWith("#")) //
				.map(line -> line.replace('\t', ' ')) //
				.split(line -> !line.startsWith(" ")) //
				.map(st -> st.fold("", String::concat)) //
				.map(line -> Util.split2(line, " ::= ")) //
				.filter(lr -> lr != null) //
				.toList();

		if (!pairs.isEmpty())
			rootEntity = pairs.get(0).t0;

		Map<String, EbnfNode> nodesByEntity = Read.from(pairs) //
				.map(lr -> Pair.of(lr.t0, new EbnfNode(EbnfType.NAMED_, lr.t0, breakdown.breakdown(lr.t1)))) //
				.collect(As.map());

		EbnfHeadRecursion headRecursion = new EbnfHeadRecursion(nodesByEntity);

		for (Entry<String, EbnfNode> entry : nodesByEntity.entrySet())
			entry.setValue(headRecursion.reduceHeadRecursion(entry.getValue()));

		grammarsByEntity = Read.from(nodesByEntity) //
				.map(lr -> Pair.of(lr.t0, build(lr.t1))) //
				.collect(As.map());
	}

	private Grammar parseGrammar(String s) {
		return build(breakdown.breakdown(s));
	}

	private Grammar build(EbnfNode en) {
		Grammar grammar;
		List<Grammar> grammars;

		switch (en.type) {
		case AND___:
			grammars = buildChildren(en);
			grammar = (parse, st) -> {
				Streamlet<State> streamlets = Read.from(st);
				for (Grammar g : grammars)
					streamlets = streamlets.concatMap(st_ -> parse.parse(st_, g));
				return streamlets;
			};
			break;
		case ENTITY:
			grammar = buildEntity(en.content);
			break;
		case EXCEPT:
			Grammar grammar0 = build(en.children.get(0));
			Grammar grammar1 = build(en.children.get(1));
			grammar = (parse, st) -> grammar0.p(parse, st).filter(st1 -> {
				String in1 = parse.in.substring(st.pos, st1.pos);
				return grammar1.p(new Parse(in1), new State(null, 0, null, 1)).count() == 0;
			});
			break;
		case NAMED_:
			grammar = deepen(build(en.children.get(0)), en.content);
			break;
		case OPTION:
			Grammar g = build(en.children.get(0));
			grammar = (parse, st) -> parse.parse(st, g).cons(st);
			break;
		case OR____:
			grammars = buildChildren(en);
			grammar = (parse, st) -> Read.from(grammars).concatMap(g_ -> parse.parse(st, g_));
			break;
		case REPT0_:
			grammar = buildRepeat(en, true);
			break;
		case REPT1_:
			grammar = buildRepeat(en, false);
			break;
		case STRING:
			Expect e = expect.expectString(en.content);
			grammar = skipWhitespaces((parse, st) -> parse.expect(st, e, st.pos));
			break;
		default:
			grammar = null;
		}

		return grammar;
	}

	private List<Grammar> buildChildren(EbnfNode en) {
		return Read.from(en.children).map(this::build).toList();
	}

	private Grammar buildRepeat(EbnfNode en, boolean isAllowNone) {
		Grammar g = build(en.children.get(0));

		return (parse, st) -> {
			Streamlet<State> states = Read.from(new Source<State>() {
				private State state_ = st;
				private Deque<Streamlet<State>> streamlets = new ArrayDeque<>();

				public State source() {
					State state0 = state_;
					if (state0 != null) {
						streamlets.push(parse.parse(state0, g));
						while (!streamlets.isEmpty() && (state_ = streamlets.peek().next()) == null)
							streamlets.pop();
					}
					return state0;
				}
			});

			// Skips first if it is a '+'
			return isAllowNone || states.next() != null ? states : noResult;
		};
	}

	private Grammar buildEntity(String entity) {
		Grammar grammar1;
		if ((grammar1 = buildLiteral(entity)) == null)
			grammar1 = (parse, st) -> {
				boolean isRecurse = false;
				State prevState = st;

				while (!isRecurse && prevState != null && prevState.pos == st.pos) {
					isRecurse |= Util.stringEquals(prevState.entity, entity);
					prevState = prevState.previous;
				}

				if (!isRecurse) {
					Grammar grammar = grammarsByEntity.get(entity);
					if (grammar != null)
						return grammar.p(parse, st);
					else
						throw new RuntimeException("Entity " + entity + " not found");
				} else
					return noResult;
			};

		return grammar1;
	}

	private Grammar buildLiteral(String entity) {
		Expect e;

		if (Util.stringEquals(entity, "<CHARACTER>"))
			e = (in, length, start) -> Math.min(start + 1, length);
		else if (Util.stringEquals(entity, "<CHARACTER_LITERAL>"))
			e = expect.expectCharLiteral;
		else if (Util.stringEquals(entity, "<FLOATING_POINT_LITERAL>"))
			e = expect.expectRealLiteral;
		else if (Util.stringEquals(entity, "<IDENTIFIER>"))
			e = expect.expectIdentifier;
		else if (entity.startsWith("<IGNORE:") && entity.endsWith(">"))
			e = expect.expectFail;
		else if (Util.stringEquals(entity, "<INTEGER_LITERAL>"))
			e = expect.expectIntegerLiteral;
		else if (Util.stringEquals(entity, "<STRING_LITERAL>"))
			e = expect.expectStringLiteral;
		else if (entity.startsWith("<UNICODE_CLASS:") && entity.endsWith(">"))
			e = expect.expectUnicodeClass(entity.substring(4, entity.length() - 1));
		else if (entity.length() == 5 && entity.charAt(0) == '[' && entity.charAt(2) == '-' && entity.charAt(4) == ']')
			e = expect.expectCharRange(entity.charAt(1), entity.charAt(3));
		else
			e = null;

		return e != null ? skipWhitespaces(deepen((parse, st) -> parse.expect(st, e, st.pos), entity)) : null;
	}

	public Node parse(String s) {
		return parse(s, rootEntity);
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

	private Grammar skipWhitespaces(Grammar grammar) {
		return (parse, st) -> {
			int pos1 = expect.expectWhitespaces(parse.in, parse.length, st.pos);
			return grammar.p(parse, new State(st, pos1));
		};
	}

	private Grammar deepen(Grammar grammar, String entity) {
		return (parse, st) -> {
			State st1 = deepen(st, entity);
			return grammar.p(parse, st1).map(st_ -> undeepen(st_, st.depth));
		};
	}

	private State deepen(State state, String entity) {
		return new State(state, state.pos, entity, state.depth + 1);
	}

	private State undeepen(State state, int depth) {
		return new State(state, state.pos, null, depth);
	}

}
