package suite.wildcard;

import java.util.List;

import suite.adt.Pair;
import suite.immutable.IList;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class Matcher {

	private Streamlet<State> noResult = Read.empty();

	private class State {
		private String input;
		private int pos;
		private IList<String> matches;

		private State(String input) {
			this(input, 0, IList.<String> end());
		}

		private State(State state, int advance) {
			this(state.input, state.pos + advance, state.matches);
		}

		private State(String input, int position, IList<String> matches) {
			this.input = input;
			pos = position;
			this.matches = matches;
		}

		private boolean eof() {
			return pos == input.length();
		}
	}

	public List<String[]> matches(String pattern, String input) {
		return applyPattern(pattern, input) //
				.filter(State::eof) //
				.map(state -> state.matches.reverse().toArray(new String[0])) //
				.toList();
	}

	public Pair<String[], String> matchStart(String pattern, String input) {
		State state = applyPattern(pattern, input).first();
		return Pair.of(state.matches.reverse().toArray(new String[0]), input.substring(state.pos));
	}

	private Streamlet<State> applyPattern(String pattern, String input) {
		Streamlet<State> st = Read.from(new State(input));

		for (char ch : Util.chars(pattern))
			switch (ch) {
			case '*':
				st = st.concatMap(state -> Read.from(new Source<State>() {
					private int start = state.pos;
					private int end = state.pos;

					public State source() {
						if (end <= state.input.length()) {
							String m = state.input.substring(start, end);
							return new State(state.input, end++, IList.cons(m, state.matches));
						} else
							return null;
					}
				}));
				break;
			case '?':
				st = st.concatMap(state -> !state.eof() ? Read.from(new State(state, 1)) : noResult);
				break;
			default:
				st = st.concatMap(state -> {
					boolean isMatch = !state.eof() && state.input.charAt(state.pos) == ch;
					return isMatch ? Read.from(new State(state, 1)) : noResult;
				});
			}

		return st;
	}

}
