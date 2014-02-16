package suite.text.wildcard;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import suite.immutable.ImmutableList;
import suite.util.FunUtil;
import suite.util.To;
import suite.util.Util;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Matcher {

	private final Source<State> noResult = FunUtil.nullSource();

	private class State {
		private String input;
		private int pos;
		private ImmutableList<String> matches;

		private State(String input) {
			this(input, 0, ImmutableList.<String> end());
		}

		private State(State state, int advance) {
			this(state.input, state.pos + advance, state.matches);
		}

		private State(String input, int position, ImmutableList<String> matches) {
			this.input = input;
			this.pos = position;
			this.matches = matches;
		}

		private boolean eof() {
			return pos == input.length();
		}
	}

	public List<String[]> matches(String pattern, String input) {
		Source<State> source = To.source(new State(input));

		for (final char ch : Util.chars(pattern))
			switch (ch) {
			case '*':
				source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
					public Source<State> apply(final State state) {
						return new Source<State>() {
							private int start = state.pos;
							private int end = state.pos;

							public State source() {
								if (end <= state.input.length()) {
									String m = state.input.substring(start, end);
									return new State(state.input, end++, ImmutableList.cons(m, state.matches));
								} else
									return null;
							}
						};
					}
				}, source));
				break;
			case '?':
				source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
					public Source<State> apply(State state) {
						return !state.eof() ? To.source(new State(state, 1)) : noResult;
					}
				}, source));
				break;
			default:
				source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
					public Source<State> apply(State state) {
						boolean isMatch = !state.eof() && state.input.charAt(state.pos) == ch;
						return isMatch ? To.source(new State(state, 1)) : noResult;
					}
				}, source));
			}

		source = FunUtil.filter(new Fun<State, Boolean>() {
			public Boolean apply(State state) {
				return state.eof();
			}
		}, source);

		List<String[]> results = new ArrayList<>();
		State state;

		while ((state = source.source()) != null) {
			Deque<String> deque = state.matches.reverse();
			results.add(deque.toArray(new String[deque.size()]));
		}

		return results;
	}

}
