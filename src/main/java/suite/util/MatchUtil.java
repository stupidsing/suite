package suite.util;

import java.util.Deque;

import suite.immutable.ImmutableList;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class MatchUtil {

	private final Source<State> noResult = FunUtil.nullSource();

	private class State {
		private String input;
		private int pos;
		private ImmutableList<String> matches;

		private State(String input) {
			this(input, 0, ImmutableList.<String> end());
		}

		private State(State state, int position) {
			this(state.input, position, state.matches);
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

	public String[] match(String pattern, String input) {
		Source<State> source = To.source(new State(input));

		for (final char ch : Util.chars(pattern)) {
			final Source<State> source0 = source;

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
				}, source0));
				break;
			case '?':
				source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
					public Source<State> apply(State state) {
						return !state.eof() ? To.source(new State(state, state.pos + 1)) : noResult;
					}
				}, source0));
				break;
			default:
				source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
					public Source<State> apply(State state) {
						boolean isMatch = !state.eof() && state.input.charAt(state.pos) == ch;
						return isMatch ? To.source(new State(state, state.pos + 1)) : noResult;
					}
				}, source0));
			}
		}

		source = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
			public Source<State> apply(State state) {
				return state.eof() ? To.source(state) : noResult;
			}
		}, source));

		State state = source.source();

		if (state != null) {
			Deque<String> deque = state.matches.reverse();
			return deque.toArray(new String[deque.size()]);
		} else
			return null;
	}

}
