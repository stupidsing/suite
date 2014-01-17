package suite.algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Pair;

public class LempelZivWelch<Unit> {

	private class Trie {
		private Integer index;
		private Map<Unit, Trie> branches = new HashMap<>();

		public Trie(Integer index) {
			this.index = index;
		}
	}

	public void encode(Source<Unit> source, Sink<Pair<Integer, Unit>> sink) {
		Trie root = new Trie(null);
		int index = 0;
		Unit unit;

		while ((unit = source.source()) != null) {
			Trie trie0 = null, trie = root;

			while (unit != null && trie != null) {
				trie = (trie0 = trie).branches.get(unit);
				unit = source.source();
			}

			if (trie0 != root)
				sink.sink(Pair.<Integer, Unit> create(trie0.index, null));

			if (unit != null) {
				sink.sink(Pair.<Integer, Unit> create(null, unit));
				trie0.branches.put(unit, new Trie(index++));
			}
		}
	}

	public void decode(Source<Pair<Integer, Unit>> source, Sink<Unit> sink) {
		Map<Integer, List<Unit>> dictionary = new HashMap<>();
		int index = 0;
		Pair<Integer, Unit> pair;
		List<Unit> word = new ArrayList<>();

		while ((pair = source.source()) != null)
			if (pair.t0 != null)
				for (Unit unit : dictionary.get(pair.t0))
					sink.sink(unit);
			else {
				sink.sink(pair.t1);
				word.add(pair.t1);
				dictionary.put(index++, word);
				word = new ArrayList<>();
			}
	}

}
