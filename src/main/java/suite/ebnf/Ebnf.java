package suite.ebnf;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.adt.Pair;
import suite.ebnf.EbnfExpect.Expect;
import suite.os.LogUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;
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
	private Map<String, Parser> parsersByEntity;

	private EbnfBreakdown breakdown = new EbnfBreakdown();
	private EbnfExpect expect = new EbnfExpect();
	private Streamlet<State> noResult = Read.empty();

	private static boolean trace = false;

	private interface Parser {
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

	private class State {
		private State previous;
		private int pos;

		private String entity;
		private int depthChange;

		private State(State previous, int pos) {
			this(previous, pos, previous.entity, 0);
		}

		private State(State previous, int pos, String entity, int depthChange) {
			this.previous = previous;
			this.pos = pos;
			this.entity = entity;
			this.depthChange = depthChange;
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

		private Node parse(int pos, Parser parser) {
			State initialState = new State(null, pos, null, 0);
			Streamlet<State> st = parse(initialState, parser);
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

					for (State state_ : states) {
						int d = state_.depthChange;
						while (d < 0) {
							d++;
							stack.pop().end = state_.pos;
						}
						while (d > 0) {
							d--;
							Node node = new Node(state_.entity, state_.pos);
							if (state_.entity != null)
								stack.peek().nodes.add(node);
							stack.push(node);
						}
					}

					return root.nodes.get(0);
				}

			return null;
		}

		private Streamlet<State> parse(State state, Parser parser) {
			if (trace)
				LogUtil.info("parse(" + parser + "): " + in.substring(state.pos));

			Streamlet<State> states = parser.p(this, state);

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

		Map<String, EbnfGrammar> grammarsByEntity = Read.from(pairs) //
				.map(lr -> Pair.of(lr.t0, breakdown.breakdown(lr.t0, lr.t1))) //
				.collect(As.map());

		EbnfHeadRecursion headRecursion = new EbnfHeadRecursion(grammarsByEntity);

		for (Entry<String, EbnfGrammar> entry : grammarsByEntity.entrySet())
			entry.setValue(headRecursion.reduceHeadRecursion(entry.getValue()));

		parsersByEntity = Read.from(grammarsByEntity) //
				.map(lr -> Pair.of(lr.t0, build(lr.t1))) //
				.collect(As.map());
	}

	private Parser parseGrammar(String s) {
		return build(breakdown.breakdown(s));
	}

	private Parser build(EbnfGrammar eg) {
		Parser parser;
		List<Parser> parsers;

		switch (eg.type) {
		case AND___:
			parsers = buildChildren(eg);
			parser = (parse, st) -> {
				Streamlet<State> streamlets = Read.from(st);
				for (Parser g : parsers)
					streamlets = streamlets.concatMap(st_ -> parse.parse(st_, g));
				return streamlets;
			};
			break;
		case ENTITY:
			parser = buildEntity(eg.content);
			break;
		case EXCEPT:
			Parser parser0 = build(eg.children.get(0));
			Parser parser1 = build(eg.children.get(1));
			parser = (parse, st) -> parser0.p(parse, st).filter(st1 -> {
				String in1 = parse.in.substring(st.pos, st1.pos);
				return parser1.p(new Parse(in1), new State(null, 0, null, 0)).count() == 0;
			});
			break;
		case NAMED_:
			parser = deepen(build(eg.children.get(0)), eg.content);
			break;
		case OPTION:
			Parser g = build(eg.children.get(0));
			parser = (parse, st) -> parse.parse(st, g).cons(st);
			break;
		case OR____:
			parsers = buildChildren(eg);
			parser = (parse, st) -> Read.from(parsers).concatMap(g_ -> parse.parse(st, g_));
			break;
		case REPT0_:
			parser = buildRepeat(eg, true);
			break;
		case REPT1_:
			parser = buildRepeat(eg, false);
			break;
		case STRING:
			Expect e = expect.expectString(eg.content);
			parser = skipWhitespaces((parse, st) -> parse.expect(st, e, st.pos));
			break;
		default:
			parser = null;
		}

		return parser;
	}

	private List<Parser> buildChildren(EbnfGrammar eg) {
		return Read.from(eg.children).map(this::build).toList();
	}

	private Parser buildRepeat(EbnfGrammar eg, boolean isAllowNone) {
		Parser g = build(eg.children.get(0));

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

	private Parser buildEntity(String entity) {
		Parser parser1;
		if ((parser1 = buildLiteral(entity)) == null)
			parser1 = (parse, st) -> {
				boolean isRecurse = false;
				State prevState = st;

				while (!isRecurse && prevState != null && prevState.pos == st.pos) {
					isRecurse |= Util.stringEquals(prevState.entity, entity);
					prevState = prevState.previous;
				}

				if (!isRecurse) {
					Parser parser = parsersByEntity.get(entity);
					if (parser != null)
						return parser.p(parse, st);
					else
						throw new RuntimeException("Entity " + entity + " not found");
				} else
					return noResult;
			};

		return parser1;
	}

	private Parser buildLiteral(String entity) {
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

	private Parser skipWhitespaces(Parser parser) {
		return (parse, st) -> {
			int pos1 = expect.expectWhitespaces(parse.in, parse.length, st.pos);
			return parser.p(parse, new State(st, pos1));
		};
	}

	private Parser deepen(Parser parser, String entity) {
		return (parse, st0) -> {
			State st1 = new State(st0, st0.pos, entity, 1);
			Streamlet<State> states = parser.p(parse, st1);
			return states.map(st2 -> new State(st2, st2.pos, null, -1));
		};
	}

}
