package suite.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class LempelZivWelch<Unit> {

	private List<Unit> units;

	private class Trie {
		private Integer index;
		private Map<Unit, Trie> branches = new HashMap<>();

		public Trie(Integer index) {
			this.index = index;
		}
	}

	public LempelZivWelch(List<Unit> units) {
		this.units = units;
	}

	public void encode(Source<Unit> source, Sink<Integer> sink) {
		Trie root = new Trie(null);
		int index = 0;

		for (Unit unit : units)
			root.branches.put(unit, new Trie(index++));

		Trie trie = root;
		Unit unit;

		while ((unit = source.source()) != null) {
			Trie trie1 = trie.branches.get(unit);

			if (trie1 == null) {
				sink.sink(trie.index);
				trie.branches.put(unit, new Trie(index++));
				trie = root.branches.get(unit);
			} else
				trie = trie1;
		}

		if (trie != root)
			sink.sink(trie.index);
	}

	public void decode(Source<Integer> source, Sink<Unit> sink) {
		List<List<Unit>> dict = new ArrayList<>();

		for (Unit unit : units)
			dict.add(Arrays.asList(unit));

		Integer index;
		List<Unit> word = new ArrayList<>();

		while ((index = source.source()) != null) {
			List<Unit> w0 = word;
			boolean isExists = index < dict.size();
			List<Unit> newWord;

			if (isExists)
				newWord = Util.add(w0, word = dict.get(index));
			else
				newWord = word = Util.add(w0, Util.left(w0, 1));

			if (!w0.isEmpty())
				dict.add(newWord);

			for (Unit unit : word)
				sink.sink(unit);
		}
	}

}
