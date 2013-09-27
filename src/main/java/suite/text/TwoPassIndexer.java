package suite.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import suite.text.Indexer.Key;
import suite.util.DefaultValueMap;
import suite.util.FunUtil.Fun;

public class TwoPassIndexer {

	private static final int minimumWordLength = 3;

	private Set<String> dictionary = new HashSet<>();

	private DefaultValueMap<String, List<Key>> keysByWord = new DefaultValueMap<>(new Fun<String, List<Key>>() {
		public List<Key> apply(String word) {
			return new ArrayList<>();
		}
	});

	public void pass1(String id, String text) {
		int length = text.length();

		for (int start = 0; start < length; start++) {
			int end = start + 1;

			while (end <= length && dictionary.contains(text.substring(start, end)))
				end++;

			if (end <= length)
				dictionary.add(text.substring(start, end));
		}
	}

	public void pass2(String id, String text) {
		int length = text.length();

		for (int start = 0; start < length; start++) {
			int end = start + 1;
			Key key = new Key(id, start);

			while (end <= length && dictionary.contains(text.substring(start, end))) {
				if (end - start >= minimumWordLength)
					keysByWord.get(text.substring(start, end)).add(key);

				end++;
			}
		}

		dictionary.clear(); // Saves memory
	}

	public DefaultValueMap<String, List<Key>> getKeysByWord() {
		return keysByWord;
	}

}
