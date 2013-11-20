package suite.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import suite.util.FunUtil.Fun;
import suite.util.IterUtil;
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
	private static final Iterator<State> noResult = Collections.<State> emptySet().iterator();

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

		validate();
	}

	private void validate() {
		for (Entry<String, List<List<String>>> entry : grammars.entrySet()) {
			String key = entry.getKey();

			for (List<String> list : entry.getValue())
				if (Util.equals(key, list.get(0)))
					throw new RuntimeException("Head recursion in rule " + key);
		}
	}

	public boolean recursiveDescent(String s) {
		Iterator<State> iter = recursiveDescent(target, s, 0);
		boolean result = false;

		while (iter.hasNext())
			result |= iter.next().end == s.length();

		return result;
	}

	public Iterator<State> recursiveDescent(String target, final String s, int end0) {
		while (end0 < s.length() && Character.isWhitespace(s.charAt(end0)))
			end0++;

		final int end = end0;
		List<List<String>> grammar;
		Iterator<State> result;

		if (target.length() > 1 && target.endsWith("?"))
			result = IterUtil.cons(new State(end) //
					, recursiveDescent(target.substring(0, target.length() - 1), s, end));
		else if (target.startsWith(inputCharExcept)) {
			String exceptChars = target.substring(inputCharExcept.length());

			if (s.length() > end && exceptChars.indexOf(s.indexOf(end)) < 0)
				result = IterUtil.asIter(new State(end + 1));
			else
				result = noResult;
		} else if ((grammar = grammars.get(target)) != null)
			result = IterUtil.concat(IterUtil.map(new Fun<List<String>, Iterator<State>>() {
				public Iterator<State> apply(List<String> list) {
					Iterator<State> iter = IterUtil.asIter(new State(end));

					for (final String item : list)
						iter = IterUtil.concat(IterUtil.map(new Fun<State, Iterator<State>>() {
							public Iterator<State> apply(State state) {
								return recursiveDescent(item, s, state.end);
							}
						}, iter));

					return iter;
				}
			}, grammar.iterator()));
		else if (s.startsWith(target, end))
			result = IterUtil.asIter(new State(end + target.length()));
		else
			result = noResult;

		return result;
	}
}
