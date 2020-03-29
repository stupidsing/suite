package suite.ebnf.topdown;

import static primal.statics.Fail.fail;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.fp.Funs.Source;
import primal.os.Log_;
import primal.puller.Puller;
import suite.ebnf.Ebnf.Ast;
import suite.ebnf.Grammar;
import suite.ebnf.Grammar.GrammarType;
import suite.ebnf.topdown.Expect.ExpectFun;
import suite.primitive.Coord;

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
	private Puller<State> noResult = Puller.empty();

	private interface Parser {
		public Puller<State> p(Parse parse, State state);
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

		private Puller<State> pr(Parse g, Parser p) {
			return g.parse(this, p);
		}

		private Puller<State> p(Parse g, Parser p) {
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

			while ((state = o.pull()) != null)
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
								var ast = new Ast(state_.frame.entity, state_.pos);
								stack.peek().children.add(ast);
								stack.push(ast);
							}
					}

					return root.children.get(0);
				}

			return null;
		}

		private Puller<State> parse(State state, Parser parser) {
			if (trace)
				Log_.info("parse(" + parser + "): " + in.substring(state.pos));

			var states = state.p(this, parser);
			if (states == noResult && 0 < state.sign && state.frame.entity != null && errorPosition <= state.pos) {
				errorPosition = state.pos;
				errorEntity = state.frame.entity;
			}
			return states;
		}

		private Puller<State> expect(State state, ExpectFun expect, int pos) {
			var end = expect.expect(in, length, pos);
			return state.pos < end ? Puller.of(state.pos(end)) : noResult;
		}

		private Coord findPosition(int position) {
			int row = 1, col = 1;
			for (var i = 0; i < position; i++) {
				col++;
				if (in.charAt(i) == 10) {
					row++;
					col = 1;
				}
			}
			return Coord.of(row, col);
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
			return fail("syntax error for entity " + parse.errorEntity + " at " + pos);
		}
	}

	public Ast check(String entity, String s) {
		return new Parse(s).parse(0, build(new Grammar(GrammarType.ENTITY, entity)));
	}

	private Parser build(Grammar eg) {
		return switch (eg.type) {
		case AND___ -> {
			var parsers = buildChildren(eg);
			yield (parse, st) -> {
				var o = Puller.of(st);
				for (var g_ : parsers)
					o = o.concatMap(st_ -> st_.pr(parse, g_));
				return o;
			};
		}
		case ENTITY -> buildEntity(eg.content);
		case EXCEPT -> {
			var parser0 = build(eg.children.get(0));
			var parser1 = build(eg.children.get(1));
			yield (parse, st) -> st.p(parse, parser0).filter(st1 -> {
				String in1 = parse.in.substring(st.pos, st1.pos);
				return new State(null, 0, null, 0).p(new Parse(in1), parser1).count() == 0;
			});
		}
		case NAMED_ -> deepen(build(eg.children.get(0)), eg.content);
		case ONCE__ -> {
			var g = build(eg.children.get(0));
			yield (parse, st) -> Puller.of(st.pr(parse, g).take(1));
		}
		case OPTION -> {
			var g = build(eg.children.get(0));
			yield (parse, st) -> st.pr(parse, g).cons(st);
		}
		case OR____ -> {
			var parsers = buildChildren(eg);
			yield (parse, st) -> Puller.of(parsers).concatMap(g_ -> st.pr(parse, g_));
		}
		case REPT0_ -> buildRepeat(eg, true);
		case REPT0H -> buildRepeatHeadRecursion(eg);
		case REPT1_ -> buildRepeat(eg, false);
		case STRING -> {
			var e = expect.string(eg.content);
			yield skipWhitespaces((parse, st) -> parse.expect(st, e, st.pos));
		}
		default -> null;
		};
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
					.concatMap(st1 -> Puller.of(new Source<State>() {
						private State state_ = st1;
						private Deque<Puller<State>> pullers = new ArrayDeque<>();

						public State g() {
							if (state_ != null) {
								var state0 = state_.deepen(frame, -1);
								pullers.push(state0.pr(parse, gc));
								while (!pullers.isEmpty() && (state_ = pullers.peek().pull()) == null)
									pullers.pop();
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
			var states = Puller.<State> of(new Source<>() {
				private State state_ = st;
				private Deque<Puller<State>> pullers = new ArrayDeque<>();

				public State g() {
					var state0 = state_;
					if (state0 != null) {
						pullers.push(state0.pr(parse, g));
						while (!pullers.isEmpty() && (state_ = pullers.peek().pull()) == null)
							pullers.pop();
					}
					return state0;
				}
			});

			// skips first if it is a '+'
			return isAllowNone || states.pull() != null ? states : noResult;
		};
	}

	private Parser buildEntity(String entity) {
		Parser parser1;
		if ((parser1 = buildLiteral(entity)) == null)
			parser1 = (parse, st) -> {
				var parser = parserByEntity.get(entity);
				return parser != null ? st.p(parse, parser) : fail("entity " + entity + " not found");
			};
		return parser1;
	}

	private Parser buildLiteral(String entity) {
		ExpectFun e;

		if (Equals.string(entity, "<CHARACTER>"))
			e = expect.char_;
		else if (Equals.string(entity, "<CHARACTER_LITERAL>"))
			e = expect.charLiteral;
		else if (Equals.string(entity, "<FLOATING_POINT_LITERAL>"))
			e = expect.realLiteral;
		else if (Equals.string(entity, "<IDENTIFIER>"))
			e = expect.identifier;
		else if (entity.startsWith("<IGNORE:") && entity.endsWith(">"))
			e = expect.fail;
		else if (Equals.string(entity, "<INTEGER_LITERAL>"))
			e = expect.integerLiteral;
		else if (Equals.string(entity, "<STRING_LITERAL>"))
			e = expect.stringLiteral;
		else if (entity.startsWith("<UNICODE_CLASS:") && entity.endsWith(">"))
			e = expect.unicodeClass(entity.substring(4, entity.length() - 1));
		else if (entity.length() == 5 && entity.charAt(0) == '[' && entity.charAt(2) == '-' && entity.charAt(4) == ']')
			e = expect.charRange(entity.charAt(1), entity.charAt(3));
		else if (entity.length() == 11 //
				&& entity.charAt(0) == '[' && entity.charAt(5) == '-' && entity.charAt(10) == ']')
			e = expect.charRange((char) Integer.parseInt(entity.substring(1, 5), 16),
					(char) Integer.parseInt(entity.substring(6, 10), 16));
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
