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

import suite.node.Atom;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.Util;

/**
 * Parser for Backus-Naur form grammars.
 * 
 * @author ywsing
 */
public class Bnf {

	private String entity;
	private Map<String, List<List<String>>> grammars = new HashMap<>();

	private static final String inputCharExcept = "$except-";

	private class Parse {
		private String in;
		private int length;
		private int errorPosition = 0;
		private String errorEntity;

		private final Source<State> noResult = FunUtil.nullSource();

		private class State {
			private int end;

			public State(int end) {
				this.end = end;
			}
		}

		private Parse(String in) {
			this.in = in;
			length = in.length();
		}

		private void parse() {
			Source<State> source = parse(0, entity);
			State state;

			while ((state = source.source()) != null)
				if (state.end == length)
					return;

			throw new RuntimeException("Syntax error at " + errorPosition + " (" + errorEntity + ")");
		}

		private Source<State> parse(int end0, String entity) {
			while (end0 < length && Character.isWhitespace(in.charAt(end0)))
				end0++;

			final int end = end0;
			List<List<String>> grammar;
			Source<State> result;

			if (entity.length() > 1 && entity.endsWith("?"))
				result = FunUtil.cons(new State(end), parse(end, Util.substr(entity, 0, -1)));
			else if (entity.length() > 1 && entity.endsWith("*"))
				result = parseRepeatedly(end, Util.substr(entity, 0, -1));
			else if (entity.equals("<identifier>"))
				result = parseIdentifier(end);
			else if (entity.startsWith(inputCharExcept)) {
				String exceptChars = entity.substring(inputCharExcept.length());

				if (length > end && exceptChars.indexOf(in.charAt(end)) < 0)
					result = FunUtil.asSource(new State(end + 1));
				else
					result = noResult;
			} else if ((grammar = grammars.get(entity)) != null)
				result = parseGrammar(end, grammar);
			else if (in.startsWith(entity, end))
				result = FunUtil.asSource(new State(end + entity.length()));
			else
				result = noResult;

			if (result == noResult && end0 > errorPosition) {
				errorPosition = end0;
				errorEntity = entity;
			}

			return result;
		}

		private Source<State> parseRepeatedly(final int end, final String entity) {
			return new Source<State>() {
				private State state = new State(end);
				private Deque<Source<State>> sources = new ArrayDeque<>();

				public State source() {
					State state0 = state;

					if (state0 != null) {
						sources.push(parse(state0.end, entity));

						while (!sources.isEmpty() && (state = sources.peek().source()) == null)
							sources.pop();
					}

					return state0;
				}
			};
		}

		private Source<State> parseGrammar(final int end, List<List<String>> grammar) {
			return FunUtil.concat(FunUtil.map(new Fun<List<String>, Source<State>>() {
				public Source<State> apply(List<String> list) {
					Source<State> source = FunUtil.asSource(new State(end));

					for (final String item : list)
						source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
							public Source<State> apply(State state) {
								return parse(state.end, item);
							}
						}, source));

					return source;
				}
			}, FunUtil.asSource(grammar)));
		}

		private Source<State> parseIdentifier(int end) {
			Source<State> result;

			if (length > end && Character.isJavaIdentifierStart(in.charAt(end))) {
				end++;

				while (length > end && Character.isJavaIdentifierPart(in.charAt(end)))
					end++;

				result = FunUtil.asSource(new State(end));
			} else
				result = noResult;

			return result;
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
				list.add(Util.splitn(item, " "));

			grammars.put(lr.t0, list);

			if (entity == null)
				entity = lr.t0;
		}

		preprocess();
	}

	/**
	 * Transform head-recursion rule as follows:
	 * 
	 * A = B | A C0 | A C1 | ... | A Cn
	 * 
	 * become two rules
	 * 
	 * A = B temp*
	 * 
	 * temp = C0 | C1 | ... | Cn
	 */
	private void preprocess() {
		Map<String, List<List<String>>> newRules = new HashMap<>();

		for (Entry<String, List<List<String>>> entry : grammars.entrySet()) {
			String key = entry.getKey();
			List<List<String>> rule = entry.getValue();
			List<String> first = Util.first(rule);
			boolean isModify = rule.size() > 1;

			for (int i = 1; i < rule.size(); i++)
				isModify &= Util.equals(Util.first(rule.get(i)), key);

			if (isModify) {
				String temp = Atom.unique().getName();
				entry.setValue(Arrays.asList(Arrays.asList(first.get(0), temp + "*")));

				List<List<String>> tempRule = new ArrayList<>();

				for (int i = 1; i < rule.size(); i++)
					tempRule.add(Util.sublist(rule.get(i), 1, 0));

				newRules.put(temp, tempRule);
			}
		}

		grammars.putAll(newRules);
	}

	public void parse(String s) {
		new Parse(s).parse();
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
