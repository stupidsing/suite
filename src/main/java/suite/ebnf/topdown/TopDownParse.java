package suite.ebnf.topdown;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import suite.ebnf.Ebnf.Ast;
import suite.ebnf.Grammar;
import suite.ebnf.Grammar.GrammarType;
import suite.ebnf.topdown.Expect.ExpectFun;
import suite.os.LogUtil;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.String_;

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
public class TopDownParse {

	private static boolean trace = false;

	private Map<String, Parser> parserByEntity;
	private Expect expect = new Expect();
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

		private Ast parse(int pos, Parser parser) {
			var o = new State(null, pos, null, 0).pr(this, parser);
			State state;

			while ((state = o.next()) != null)
				if (expect.whitespaces(in, length, state.pos) == length) {
					var states = new ArrayDeque<State>();

					while (state != null) {
						if (state.sign < 0)
							state.frame.depth++;

						states.addFirst(state);
						state = state.previous;
					}

					var stack = new ArrayDeque<Ast>();
					var root = new Ast(null);
					stack.push(root);

					for (var state_ : states) {
						var d = state_.sign;
						if (d < 0)
							stack.pop().end = state_.pos;
						else if (0 < d)
							for (var i = 0; i < state_.frame.depth; i++) {
								var node = new Ast(state_.frame.entity, state_.pos);
								stack.peek().children.add(node);
								stack.push(node);
							}
					}

					return root.children.get(0);
				}

			return null;
		}

		private Outlet<State> parse(State state, Parser parser) {
			if (trace)
				LogUtil.info("parse(" + parser + "): " + in.substring(state.pos));

			var states = state.p(this, parser);
			if (states == noResult && 0 < state.sign && state.frame.entity != null && errorPosition <= state.pos) {
				errorPosition = state.pos;
				errorEntity = state.frame.entity;
			}
			return states;
		}

		private Outlet<State> expect(State state, ExpectFun expect, int pos) {
			var end = expect.expect(in, length, pos);
			return state.pos < end ? Outlet.of(state.pos(end)) : noResult;
		}

		private IntIntPair findPosition(int position) {
			int row = 1, col = 1;
			for (var i = 0; i < position; i++) {
				col++;
				if (in.charAt(i) == 10) {
					row++;
					col = 1;
				}
			}
			return IntIntPair.of(row, col);
		}
	}

	public TopDownParse(Map<String, Grammar> grammarByEntity) {
		parserByEntity = Read.from2(grammarByEntity).mapValue(this::build).toMap();
	}

	public Ast parse(String entity, String s) {
		var parse = new Parse(s);
		Ast node = parse.parse(0, build(new Grammar(GrammarType.ENTITY, entity)));
		if (node != null)
			return node;
		else {
			var pos = parse.findPosition(parse.errorPosition);
			return Fail.t("syntax error for entity " + parse.errorEntity + " at " + pos);
		}
	}

	public Ast check(String entity, String s) {
		return new Parse(s).parse(0, build(new Grammar(GrammarType.ENTITY, entity)));
	}

	private Parser build(Grammar eg) {
		Parser parser, g;
		List<Parser> parsers;

		switch (eg.type) {
		case AND___:
			parsers = buildChildren(eg);
			parser = (parse, st) -> {
				var o = Outlet.of(st);
				for (var g_ : parsers)
					o = o.concatMap(st_ -> st_.pr(parse, g_));
				return o;
			};
			break;
		case ENTITY:
			parser = buildEntity(eg.content);
			break;
		case EXCEPT:
			var parser0 = build(eg.children.get(0));
			var parser1 = build(eg.children.get(1));
			parser = (parse, st) -> st.p(parse, parser0).filter(st1 -> {
				String in1 = parse.in.substring(st.pos, st1.pos);
				return new State(null, 0, null, 0).p(new Parse(in1), parser1).count() == 0;
			});
			break;
		case NAMED_:
			parser = deepen(build(eg.children.get(0)), eg.content);
			break;
		case ONCE__:
			g = build(eg.children.get(0));
			parser = (parse, st) -> Outlet.of(st.pr(parse, g).take(1));
			break;
		case OPTION:
			g = build(eg.children.get(0));
			parser = (parse, st) -> st.pr(parse, g).cons(st);
			break;
		case OR____:
			parsers = buildChildren(eg);
			parser = (parse, st) -> Outlet.of(parsers).concatMap(g_ -> st.pr(parse, g_));
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
			var e = expect.string(eg.content);
			parser = skipWhitespaces((parse, st) -> parse.expect(st, e, st.pos));
			break;
		default:
			parser = null;
		}

		return parser;
	}

	private List<Parser> buildChildren(Grammar eg) {
		return Read.from(eg.children).map(this::build).toList();
	}

	private Parser buildRepeatHeadRecursion(Grammar eg) {
		var gb = build(eg.children.get(0));
		var gc = build(eg.children.get(1));

		return (parse, st0) -> {
			var frame = new Frame(eg.content);
			return st0.deepen(frame, 1) //
					.p(parse, gb) //
					.concatMap(st1 -> Outlet.of(new Source<State>() {
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

	private Parser buildRepeat(Grammar eg, boolean isAllowNone) {
		var g = build(eg.children.get(0));

		return (parse, st) -> {
			Outlet<State> states = Outlet.of(new Source<>() {
				private State state_ = st;
				private Deque<Outlet<State>> outlets = new ArrayDeque<>();

				public State source() {
					var state0 = state_;
					if (state0 != null) {
						outlets.push(state0.pr(parse, g));
						while (!outlets.isEmpty() && (state_ = outlets.peek().next()) == null)
							outlets.pop();
					}
					return state0;
				}
			});

			// skips first if it is a '+'
			return isAllowNone || states.next() != null ? states : noResult;
		};
	}

	private Parser buildEntity(String entity) {
		Parser parser1;
		if ((parser1 = buildLiteral(entity)) == null)
			parser1 = (parse, st) -> {
				var parser = parserByEntity.get(entity);
				if (parser != null)
					return st.p(parse, parser);
				else
					return Fail.t("entity " + entity + " not found");
			};

		return parser1;
	}

	private Parser buildLiteral(String entity) {
		ExpectFun e;

		if (String_.equals(entity, "<CHARACTER>"))
			e = expect.char_;
		else if (String_.equals(entity, "<CHARACTER_LITERAL>"))
			e = expect.charLiteral;
		else if (String_.equals(entity, "<FLOATING_POINT_LITERAL>"))
			e = expect.realLiteral;
		else if (String_.equals(entity, "<IDENTIFIER>"))
			e = expect.identifier;
		else if (entity.startsWith("<IGNORE:") && entity.endsWith(">"))
			e = expect.fail;
		else if (String_.equals(entity, "<INTEGER_LITERAL>"))
			e = expect.integerLiteral;
		else if (String_.equals(entity, "<STRING_LITERAL>"))
			e = expect.stringLiteral;
		else if (entity.startsWith("<UNICODE_CLASS:") && entity.endsWith(">"))
			e = expect.unicodeClass(entity.substring(4, entity.length() - 1));
		else if (entity.length() == 5 && entity.charAt(0) == '[' && entity.charAt(2) == '-' && entity.charAt(4) == ']')
			e = expect.charRange(entity.charAt(1), entity.charAt(3));
		else
			e = null;

		return e != null ? skipWhitespaces(deepen((parse, st) -> parse.expect(st, e, st.pos), entity)) : null;
	}

	private Parser skipWhitespaces(Parser parser) {
		return (parse, st) -> {
			var pos1 = expect.whitespaces(parse.in, parse.length, st.pos);
			return st.pos(pos1).p(parse, parser);
		};
	}

	private Parser deepen(Parser parser, String entity) {
		return (parse, st0) -> {
			var frame = new Frame(entity);
			return st0.deepen(frame, 1) //
					.p(parse, parser) //
					.map(st2 -> st2.deepen(frame, -1));
		};
	}

}
