package suite.parser;

import java.util.List;

import primal.Verbs.Build;
import primal.adt.Pair;
import primal.fp.Funs.Source;
import primal.primitive.ChrMoreVerbs.ConcatChr;
import primal.streamlet.Streamlet;
import suite.persistent.PerList;
import suite.streamlet.Read;

public class Wildcard {

	private static Matcher matcher = new Matcher();

	public static boolean isMatch(String pattern, String s) {
		if (!pattern.isEmpty()) {
			var ph = pattern.charAt(0);
			var pt = pattern.substring(1);

			if (ph != '*')
				return !s.isEmpty() && s.charAt(0) == ph && isMatch(pt, s.substring(1));
			else
				return isMatch(pt, s) || !s.isEmpty() && isMatch(pattern, s.substring(1));
		} else
			return s.isEmpty();
	}

	public static boolean isMatch2(String p0, String p1) {
		if (!p0.isEmpty() && !p1.isEmpty()) {
			char h0 = p0.charAt(0), h1 = p1.charAt(0);
			String t0 = p0.substring(1), t1 = p1.substring(1);

			return h0 == '*' && (isMatch2(t0, p1) || isMatch2(p0, t1)) //
					|| h1 == '*' && (isMatch2(p0, t1) || isMatch2(t0, p1)) //
					|| h0 == h1 && isMatch2(t0, t1);
		} else
			return ConcatChr.of(Read.chars(p0), Read.chars(p1)).isAll(c -> c == '*');
	}

	public static String[] match(String pattern, String input) {
		List<String[]> matches = matches(pattern, input);
		return matches.size() == 1 ? matches.get(0) : null;
	}

	public static List<String[]> matches(String pattern, String input) {
		return matcher.matches(pattern, input);
	}

	public static Pair<String[], String> matchStart(String pattern, String input) {
		return matcher.matchStart(pattern, input);
	}

	public static String apply(String pattern, String[] input) {
		return Build.string(sb -> {
			var i = 0;
			for (var ch : Read.chars(pattern))
				switch (ch) {
				case '*':
				case '?':
					sb.append(input[i++]);
					break;
				default:
					sb.append(ch);
				}
		});
	}

}

class Matcher {

	private class State {
		private String input;
		private int pos;
		private PerList<String> matches;

		private State(String input) {
			this(input, 0, PerList.<String> end());
		}

		private State(State state, int advance) {
			this(state.input, state.pos + advance, state.matches);
		}

		private State(String input, int position, PerList<String> matches) {
			this.input = input;
			pos = position;
			this.matches = matches;
		}

		private boolean eof() {
			return pos == input.length();
		}
	}

	List<String[]> matches(String pattern, String input) {
		return applyPattern(pattern, input) //
				.filter(State::eof) //
				.map(state -> state.matches.reverse().toArray(new String[0])) //
				.toList();
	}

	Pair<String[], String> matchStart(String pattern, String input) {
		var state = applyPattern(pattern, input).first();
		return Pair.of(state.matches.reverse().toArray(new String[0]), input.substring(state.pos));
	}

	private Streamlet<State> applyPattern(String pattern, String input) {
		var st = Read.each(new State(input));

		for (var ch : Read.chars(pattern))
			switch (ch) {
			case '*':
				st = st.concatMap(state -> Read.from(() -> new Source<State>() {
					private int start = state.pos;
					private int end = state.pos;

					public State g() {
						if (end <= state.input.length()) {
							String m = state.input.substring(start, end);
							return new State(state.input, end++, PerList.cons(m, state.matches));
						} else
							return null;
					}
				}));
				break;
			case '?':
				st = st.concatMap(state -> !state.eof() ? Read.each(new State(state, 1)) : Read.empty());
				break;
			default:
				st = st.concatMap(state -> {
					var isMatch = !state.eof() && state.input.charAt(state.pos) == ch;
					return isMatch ? Read.each(new State(state, 1)) : Read.empty();
				});
			}

		return st;
	}

}
