package suite.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private String target;
	private Map<String, List<List<String>>> grammars = new HashMap<>();

	private static final String inputCharExcept = "$except-";
	private static final Source<State> noResult = FunUtil.nullSource();

	private class State {
		private int end;

		public State(int end) {
			this.end = end;
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

			if (target == null)
				target = lr.t0;
		}
	}

	public boolean recursiveDescent(String s) {
		Source<State> source = recursiveDescent(target, s, 0);
		State state;

		while ((state = source.source()) != null)
			if (state.end == s.length())
				return true;

		return false;
	}

	private Source<State> recursiveDescent(String target, final String s, int end0) {
		while (end0 < s.length() && Character.isWhitespace(s.charAt(end0)))
			end0++;

		final int end = end0;
		List<List<String>> grammar;
		Source<State> result;

		if (target.length() > 1 && target.endsWith("?"))
			result = FunUtil.cons(new State(end) //
					, recursiveDescent(Util.substr(target, 0, -1), s, end));
		else if (target.length() > 1 && target.endsWith("*")) {
			final String target1 = Util.substr(target, 0, -1);

			result = new Source<State>() {
				private State state = new State(end);
				private Deque<Source<State>> sources = new ArrayDeque<>();

				public State source() {
					State state0 = state;

					if (state0 != null) {
						sources.push(recursiveDescent(target1, s, state0.end));

						while (!sources.isEmpty() && (state = sources.peek().source()) == null)
							sources.pop();
					}

					return state0;

				}
			};
		} else if (target.startsWith(inputCharExcept)) {
			String exceptChars = target.substring(inputCharExcept.length());

			if (s.length() > end && exceptChars.indexOf(s.indexOf(end)) < 0)
				result = FunUtil.asSource(new State(end + 1));
			else
				result = noResult;
		} else if ((grammar = grammars.get(target)) != null)
			result = FunUtil.concat(FunUtil.map(new Fun<List<String>, Source<State>>() {
				public Source<State> apply(List<String> list) {
					Source<State> source = FunUtil.asSource(new State(end));

					for (final String item : list)
						source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
							public Source<State> apply(State state) {
								return recursiveDescent(item, s, state.end);
							}
						}, source));

					return source;
				}
			}, FunUtil.asSource(grammar)));
		else if (s.startsWith(target, end))
			result = FunUtil.asSource(new State(end + target.length()));
		else
			result = noResult;

		return result;
	}

}
