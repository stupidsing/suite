package suite.ebnf;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.ebnf.Ebnf.Node;
import suite.ebnf.EbnfExpect.Expect;
import suite.ebnf.EbnfGrammar.EbnfGrammarType;
import suite.os.LogUtil;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;
import suite.util.Util;

/**
 * Backtracking LL parser for Backus-Naur form grammars.
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
public class EbnfTopDownParse {

	private static boolean trace = false;

	private Map<String, Parser> parserByEntity;
	private EbnfExpect expect = new EbnfExpect();
	private Outlet<State> noResult = Outlet.empty();

	private interface Parser {
		public Outlet<State> p(Parse parse, State state);
	}

	private class State {
		private State previous;
		private int pos;
		private Frame frame;
		private int sign;

		private State pos(int pos) {
			return new State(this, pos, null, 0);
		}

		private State deepen(Frame frame, int sign) {
			return new State(this, pos, frame, sign);
		}

		private State(State previous, int pos, Frame frame, int sign) {
			this.previous = previous;
			this.pos = pos;
			this.sign = sign;
			this.frame = frame;
		}

		private Outlet<State> pr(Parse g, Parser p) {
			return g.parse(this, p);
		}

		private Outlet<State> p(Parse g, Parser p) {
			return p.p(g, this);
		}
	}

	private class Frame {
		private int depth;
		private String entity;

		private Frame(String entity) {
			this.entity = entity;
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
			Outlet<State> o = initialState.pr(this, parser);
			State state;

			while ((state = o.next()) != null)
				if (expect.expectWhitespaces(in, length, state.pos) == length) {
					Deque<State> states = new ArrayDeque<>();

					while (state != null) {
						if (state.sign < 0)
							state.frame.depth++;

						states.addFirst(state);
						state = state.previous;
					}

					Node root = new Node(null, 0);

					Deque<Node> stack = new ArrayDeque<>();
					stack.push(root);

					for (State state_ : states) {
						int d = state_.sign;
						if (d < 0)
							stack.pop().end = state_.pos;
						else if (d > 0)
							for (int i = 0; i < state_.frame.depth; i++) {
								Node node = new Node(state_.frame.entity, state_.pos);
								stack.peek().nodes.add(node);
								stack.push(node);
							}
					}

					return root.nodes.get(0);
				}

			return null;
		}

		private Outlet<State> parse(State state, Parser parser) {
			if (trace)
				LogUtil.info("parse(" + parser + "): " + in.substring(state.pos));

			Outlet<State> states = state.p(this, parser);
			if (states == noResult && state.sign > 0 && state.frame.entity != null && state.pos >= errorPosition) {
				errorPosition = state.pos;
				errorEntity = state.frame.entity;
			}
			return states;
		}

		private Outlet<State> expect(State state, Expect expect, int pos) {
			int end = expect.expect(in, length, pos);
			return state.pos < end ? Outlet.from(state.pos(end)) : noResult;
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

	public EbnfTopDownParse(Map<String, EbnfGrammar> grammarByEntity) {
		parserByEntity = Read.from2(grammarByEntity) //
				.mapValue(this::build) //
				.toMap();
	}

	public Node parse(String entity, String s) {
		Parse parse = new Parse(s);
		Node node = parse.parse(0, build(new EbnfGrammar(EbnfGrammarType.ENTITY, entity)));
		if (node != null)
			return node;
		else {
			Pair<Integer, Integer> pos = parse.findPosition(parse.errorPosition);
			throw new RuntimeException("Syntax error for entity " + parse.errorEntity + " at " + pos);
		}
	}

	public Node check(String entity, String s) {
		return new Parse(s).parse(0, build(new EbnfGrammar(EbnfGrammarType.ENTITY, entity)));
	}

	private Parser build(EbnfGrammar eg) {
		Parser parser;
		List<Parser> parsers;

		switch (eg.type) {
		case AND___:
			parsers = buildChildren(eg);
			parser = (parse, st) -> {
				Outlet<State> o = Outlet.from(st);
				for (Parser g : parsers)
					o = o.concatMap(st_ -> st_.pr(parse, g));
				return o;
			};
			break;
		case ENTITY:
			parser = buildEntity(eg.content);
			break;
		case EXCEPT:
			Parser parser0 = build(eg.children.get(0));
			Parser parser1 = build(eg.children.get(1));
			parser = (parse, st) -> st.p(parse, parser0).filter(st1 -> {
				String in1 = parse.in.substring(st.pos, st1.pos);
				return new State(null, 0, null, 0).p(new Parse(in1), parser1).count() == 0;
			});
			break;
		case NAMED_:
			parser = deepen(build(eg.children.get(0)), eg.content);
			break;
		case OPTION:
			Parser g = build(eg.children.get(0));
			parser = (parse, st) -> st.pr(parse, g).cons(st);
			break;
		case OR____:
			parsers = buildChildren(eg);
			parser = (parse, st) -> Outlet.from(parsers).concatMap(g_ -> st.pr(parse, g_));
			break;
		case REPT0_:
			parser = buildRepeat(eg, true);
			break;
		case REPT0H:
			parser = buildRepeatHeadRecursion(eg);
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

	private Parser buildRepeatHeadRecursion(EbnfGrammar eg) {
		Parser gb = build(eg.children.get(0));
		Parser gc = build(eg.children.get(1));

		return (parse, st0) -> {
			Frame frame = new Frame(eg.content);
			return st0.deepen(frame, 1) //
					.p(parse, gb) //
					.concatMap(st1 -> Outlet.from(new Source<State>() {
				private State state_ = st1;
				private Deque<Outlet<State>> outlets = new ArrayDeque<>();

				public State source() {
					if (state_ != null) {
						State state0 = state_.deepen(frame, -1);
						outlets.push(state0.pr(parse, gc));
						while (!outlets.isEmpty() && (state_ = outlets.peek().next()) == null)
							outlets.pop();
						return state0;
					} else
						return null;
				}
			}));
		};
	}

	private Parser buildRepeat(EbnfGrammar eg, boolean isAllowNone) {
		Parser g = build(eg.children.get(0));

		return (parse, st) -> {
			Outlet<State> states = Outlet.from(new Source<State>() {
				private State state_ = st;
				private Deque<Outlet<State>> outlets = new ArrayDeque<>();

				public State source() {
					State state0 = state_;
					if (state0 != null) {
						outlets.push(state0.pr(parse, g));
						while (!outlets.isEmpty() && (state_ = outlets.peek().next()) == null)
							outlets.pop();
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
				Parser parser = parserByEntity.get(entity);
				if (parser != null)
					return st.p(parse, parser);
				else
					throw new RuntimeException("Entity " + entity + " not found");
			};

		return parser1;
	}

	private Parser buildLiteral(String entity) {
		Expect e;

		if (Util.stringEquals(entity, "<CHARACTER>"))
			e = expect.expectChar;
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

	private Parser skipWhitespaces(Parser parser) {
		return (parse, st) -> {
			int pos1 = expect.expectWhitespaces(parse.in, parse.length, st.pos);
			return st.pos(pos1).p(parse, parser);
		};
	}

	private Parser deepen(Parser parser, String entity) {
		return (parse, st0) -> {
			Frame frame = new Frame(entity);
			return st0.deepen(frame, 1) //
					.p(parse, parser) //
					.map(st2 -> st2.deepen(frame, -1));
		};
	}

}
