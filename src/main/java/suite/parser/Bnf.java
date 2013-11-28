package suite.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.fp.RbTreeMap;
import suite.node.Atom;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.LogUtil;
import suite.util.Pair;
import suite.util.To;
import suite.util.Util;

/**
 * Parser for Backus-Naur form grammars.
 * 
 * @author ywsing
 */
public class Bnf {

	private String entity;
	private Map<String, List<List<String>>> grammars = new HashMap<>();

	private static final String charExcept = "$char-except-";
	private static final boolean trace = false;

	public class Node {
		private int end;
		private String entity;
		private List<Node> nodes = new ArrayList<>();

		public Node(int end, String entity) {
			this.end = end;
			this.entity = entity;
		}

		public String toString() {
			return entity + "@" + end + nodes;
		}

		public int getEnd() {
			return end;
		}

		public String getEntity() {
			return entity;
		}
	}

	private class Parse {
		private String in;
		private int length;
		private int errorPosition = 0;
		private String errorEntity;

		private final Source<State> noResult = FunUtil.nullSource();

		private class State {
			private State previous;
			private int end;
			private RbTreeMap<String, Integer> entities; // Avoids re-entrance
			private int depth;
			private String entity;

			private State(State previous, int end, RbTreeMap<String, Integer> entities) {
				this(previous, end, entities, previous.depth, null);
			}

			private State(State previous, int end, RbTreeMap<String, Integer> entities, int depth, String entity) {
				this.previous = previous;
				this.end = end;
				this.entities = entities;
				this.depth = depth;
				this.entity = entity;
			}
		}

		private Parse(String in) {
			this.in = in;
			length = in.length();
		}

		private Node parse(int end, String entity) {
			State initialState = new State(null, end, new RbTreeMap<String, Integer>(), 0, null);
			Source<State> source = parseEntity(initialState, entity);
			State state;

			while ((state = source.source()) != null)
				if (state.end == length) {
					List<State> states = new LinkedList<>();
					State s = state;

					while (s != null) {
						states.add(0, s);
						s = s.previous;
					}

					Node root = new Node(0, null);

					Deque<Node> stack = new ArrayDeque<>();
					stack.push(root);

					for (State state_ : states)
						if (state_.entity != null) {
							while (state_.depth < stack.size())
								stack.pop();

							Node node = new Node(state_.end, state_.entity);
							stack.peek().nodes.add(node);
							stack.push(node);
						}

					return root.nodes.get(0);
				}

			throw new RuntimeException("Syntax error for entity " + errorEntity + " at " + findPosition(errorPosition));
		}

		private Source<State> parseEntity(State state, String entity) {
			int end = state.end;

			while (end < length && Character.isWhitespace(in.charAt(end)))
				end++;

			if (trace)
				LogUtil.info("parseEntity(" + entity + "): " + in.substring(end));

			State state1 = new State(state, end, state.entities);
			List<List<String>> grammar;
			Source<State> result;

			if (entity.length() > 1 && entity.endsWith("?"))
				result = FunUtil.cons(state1, parseEntity(state1, Util.substr(entity, 0, -1)));
			else if (entity.length() > 1 && entity.endsWith("*"))
				result = parseRepeat(state1, Util.substr(entity, 0, -1));
			else if (entity.equals("<identifier>"))
				result = parseExpect(state1, expectIdentifier(end));
			else if (entity.startsWith(charExcept))
				result = parseExpect(state1, expectCharExcept(end, entity.substring(charExcept.length())));
			else if ((grammar = grammars.get(entity)) != null)
				result = parseGrammar(state1, entity, grammar);
			else if (entity.length() > 1 && entity.startsWith("\"") && entity.endsWith("\""))
				result = parseExpect(state1, expectString(end, Util.substr(entity, 1, -1)));
			else if (in.startsWith(entity, end))
				result = parseExpect(state1, expectString(end, entity));
			else
				result = noResult;

			if (result == noResult && end > errorPosition) {
				errorPosition = end;
				errorEntity = entity;
			}

			return result;
		}

		private Source<State> parseRepeat(final State state, final String entity) {
			return new Source<State>() {
				private State state_ = state;
				private Deque<Source<State>> sources = new ArrayDeque<>();

				public State source() {
					State state0 = state_;

					if (state0 != null) {
						sources.push(parseEntity(state0, entity));

						while (!sources.isEmpty() && (state_ = sources.peek().source()) == null)
							sources.pop();
					}

					return state0;
				}
			};
		}

		private Source<State> parseGrammar(State state, final String entity, List<List<String>> grammar) {
			final RbTreeMap<String, Integer> entities = state.entities;
			Integer lastEnd = entities.get(entity);
			boolean isReentrance = lastEnd != null && lastEnd.intValue() == state.end;
			Source<State> result;

			if (!isReentrance) {
				final int depth = state.depth;
				RbTreeMap<String, Integer> entities1 = entities.replace(entity, state.end);
				final State state1 = new State(state, state.end, entities1, depth + 1, entity);

				result = FunUtil.concat(FunUtil.map(new Fun<List<String>, Source<State>>() {
					public Source<State> apply(List<String> list) {
						Source<State> source = FunUtil.asSource(state1);

						for (final String item : list)
							source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
								public Source<State> apply(State state) {
									return parseEntity(state, item);
								}
							}, source));

						return source;
					}
				}, FunUtil.asSource(grammar)));

				result = FunUtil.map(new Fun<State, State>() {
					public State apply(State state) {
						return new State(state, state.end, entities, depth, null);
					}
				}, result);
			} else
				result = noResult;

			return result;
		}

		private Source<State> parseExpect(State state, int end) {
			return state.end < end ? FunUtil.asSource(new State(state, end, state.entities)) : noResult;
		}

		private int expectIdentifier(int end) {
			if (end < length && Character.isJavaIdentifierStart(in.charAt(end))) {
				end++;
				while (end < length && Character.isJavaIdentifierPart(in.charAt(end)))
					end++;
			}
			return end;
		}

		private int expectCharExcept(int end, String excepts) {
			return end + (end < length && excepts.indexOf(in.charAt(end)) < 0 ? 1 : 0);
		}

		private int expectString(int end, String s) {
			return end + (in.startsWith(s, end) ? s.length() : 0);
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

	public Bnf(Reader reader) throws IOException {
		BufferedReader br = new BufferedReader(reader);
		String line;

		while ((line = br.readLine()) != null) {
			if (line.isEmpty() || line.charAt(0) == '#')
				continue;

			Pair<String, String> lr = Util.split2(line, " ::= ");
			if (lr == null)
				continue;

			List<List<String>> list = new ArrayList<>();

			for (String item : Util.splitn(" " + lr.t1 + " ", " | "))
				list.add(To.list(Util.splitn(item, " ")));

			grammars.put(lr.t0, list);

			if (entity == null)
				entity = lr.t0;
		}

		preprocess();
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
	private void preprocess() {
		Map<String, List<List<String>>> newRules = new HashMap<>();

		for (Entry<String, List<List<String>>> entry : grammars.entrySet()) {
			String entity = entry.getKey();
			List<List<String>> rule = entry.getValue();
			boolean isAnyHeadRecursion = false;
			List<Boolean> isHeadRecursions = new ArrayList<>();

			for (int i = 0; i < rule.size(); i++) {
				boolean isHeadRecursion = Util.equals(Util.first(rule.get(i)), entity);
				isHeadRecursions.add(isHeadRecursion);
				isAnyHeadRecursion |= isHeadRecursion;
			}

			if (isAnyHeadRecursion) {
				String tempb = Atom.unique().getName();
				String tempc = Atom.unique().getName();
				entry.setValue(Arrays.asList(Arrays.asList(tempb, tempc + "*")));

				List<List<String>> tempbRule = new ArrayList<>();
				List<List<String>> tempcRule = new ArrayList<>();

				for (int i = 0; i < rule.size(); i++)
					if (isHeadRecursions.get(i))
						tempcRule.add(Util.sublist(rule.get(i), 1, 0));
					else
						tempbRule.add(rule.get(i));

				newRules.put(tempb, tempbRule);
				newRules.put(tempc, tempcRule);
			}
		}

		grammars.putAll(newRules);

		verify();
	}

	private void verify() {
		for (Entry<String, List<List<String>>> entry : grammars.entrySet()) {
			String entity = entry.getKey();

			for (List<String> rule : entry.getValue())
				if (Util.equals(Util.first(rule), entity))
					throw new RuntimeException("Head recursion for " + entity);
		}
	}

	public Node parse(String s) {
		return parse(s, 0, entity);
	}

	public Node parse(String s, int end, String entity) {
		return new Parse(s).parse(end, entity);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Entry<String, List<List<String>>> entry : grammars.entrySet()) {
			sb.append(entry.getKey() + " ::= ");
			boolean first0 = true;

			for (List<String> list : entry.getValue()) {
				sb.append(!first0 ? " | " : "");
				first0 = false;
				boolean first1 = true;

				for (String item : list) {
					sb.append(!first1 ? " " : "");
					first1 = false;
					sb.append(item);
				}
			}

			sb.append("\n");
		}

		return sb.toString();
	}

}
