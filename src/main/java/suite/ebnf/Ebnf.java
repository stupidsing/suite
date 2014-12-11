package suite.ebnf;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.ebnf.EbnfExpect.Expect;
import suite.ebnf.EbnfNode.EbnfType;
import suite.node.io.Escaper;
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

	private String rootGrammarEntity;
	private Map<String, EbnfNode> nodesByEntity;
	private Map<String, Grammar> grammarsByEntity;

	private EbnfBreakdown breakdown = new EbnfBreakdown();
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
		List<Pair<String, String>> pairs = Read.lines(reader) //
				.filter(line -> !line.isEmpty() && !line.startsWith("#")) //
				.map(line -> line.replace('\t', ' ')) //
				.split(line -> !line.startsWith(" ")) //
				.map(st -> st.fold("", String::concat)) //
				.map(line -> Util.split2(line, " ::= ")) //
				.filter(lr -> lr != null) //
				.toList();

		if (!pairs.isEmpty())
			rootGrammarEntity = pairs.get(0).t0;

		nodesByEntity = Read.from(pairs).map(lr -> Pair.of(lr.t0, breakdown.breakdown(lr.t1))).collect(As.map());
		reduceHeadRecursion();
		grammarsByEntity = Read.from(nodesByEntity).map(lr -> Pair.of(lr.t0, build(lr.t1))).collect(As.map());
	}

	private Grammar parseGrammar(String s) {
		return build(breakdown.breakdown(s));
	}

	private Grammar build(EbnfNode en) {
		Grammar grammar;

		switch (en.type) {
		case AND___:
			grammar = new JoinGrammar(buildChildren(en));
			break;
		case ENTITY:
			if (Util.stringEquals(en.content, "<EOF>"))
				grammar = (parse, st) -> st.pos == parse.length ? Read.from(st) : noResult;
			else
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
		case OPTION:
			Grammar childGrammar = build(en.children.get(0));
			grammar = (parse, st) -> parse.parse(st, childGrammar).cons(st);
			break;
		case OR____:
			grammar = new OrGrammar(buildChildren(en));
			break;
		case REPT0_:
			grammar = new RepeatGrammar(build(en.children.get(0)), true);
			break;
		case REPT1_:
			grammar = new RepeatGrammar(build(en.children.get(0)), false);
			break;
		case STRING:
			Expect e = expect.expectString(en.content);
			grammar = (parse, st) -> parse.expect(st, e, st.pos);
			break;
		default:
			grammar = null;
		}

		return grammar;
	}

	private List<Grammar> buildChildren(EbnfNode en) {
		return Read.from(en.children).map(this::build).toList();
	}

	private Grammar buildEntity(String entity) {
		Grammar grammar;
		if ((grammar = buildLiteral(entity)) == null)
			grammar = new EntityGrammar(entity);
		return grammar;
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
		else if (entity.startsWith("\"") && entity.endsWith("\""))
			e = expect.expectString(Escaper.unescape(Util.substr(entity, 1, -1), "\""));
		else
			e = null;

		Grammar grammar = e != null ? grammar = (parse, st) -> parse.expect(st, e, st.pos) : null;

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

	private State deepen(State state, String entity) {
		return new State(state, state.pos, entity, state.depth + 1);
	}

	private State undeepen(State state, int depth) {
		return new State(state, state.pos, null, depth);
	}

	private void reduceHeadRecursion() {
		for (Entry<String, EbnfNode> entry : nodesByEntity.entrySet())
			entry.setValue(reduceHeadRecursion(entry.getValue()));
	}

	/**
	 * Transform head-recursion rule as follows:
	 *
	 * A = B0 | B1 | ... | Bm | A C0 | A C1 | ... | A Cn
	 *
	 * becomes
	 *
	 * A = (B0 | B1 | ... | Bm) (C0 | C1 | ... | Cn)*
	 */
	private EbnfNode reduceHeadRecursion(EbnfNode en0) {
		EbnfNode en = lookup(en0);
		EbnfNode en1;

		if (en.type == EbnfType.OR____) {
			List<EbnfNode> listb = new ArrayList<>();
			List<EbnfNode> listc = new ArrayList<>();

			for (EbnfNode childEn : en.children) {
				if (childEn.type == EbnfType.AND___) {
					List<EbnfNode> ens = childEn.children;

					if (lookup(ens.get(0)) == en) {
						listc.add(new EbnfNode(EbnfType.AND___, Util.right(ens, 1)));
						continue;
					}
				}

				listb.add(childEn);
			}

			if (!listc.isEmpty()) {
				EbnfNode enb = new EbnfNode(EbnfType.OR____, listb);
				EbnfNode enc = new EbnfNode(EbnfType.OR____, listc);
				en1 = new EbnfNode(EbnfType.AND___, Arrays.asList(enb, new EbnfNode(EbnfType.REPT0_, enc)));
			} else
				en1 = en;
		} else
			en1 = en;

		return en1;
	}

	private EbnfNode lookup(EbnfNode en) {
		String entity = en.type == EbnfType.ENTITY ? en.content : null;
		EbnfNode en1 = entity != null ? nodesByEntity.get(entity) : null;
		return en1 != null ? en1 : en;
	}

}
