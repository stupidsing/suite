package suite.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;

public class TwoPassIndexer {

	private static int minimumWordLength = 3;

	private Set<String> dictionary = new HashSet<>();
	private TreeSet<String> keys = new TreeSet<>();
	private Map<String, List<Reference>> referencesByWord = new HashMap<>();

	public static class Reference {
		public final String id;
		public final int offset;

		public Reference(String id, int offset) {
			this.id = id;
			this.offset = offset;
		}

		public String toString() {
			return id + "(" + offset + ")";
		}
	}

	public void pass0(String id, String text) {
		int length = text.length();

		for (int start = 0; start < length; start++) {
			int end = start + minimumWordLength;

			while (end <= length && dictionary.contains(text.substring(start, end)))
				end++;

			if (end <= length)
				dictionary.add(text.substring(start, end));
		}
	}

	public void pass1(String id, String text) {
		int length = text.length();

		for (int start = 0; start < length; start++) {
			int end = start + minimumWordLength;
			Reference key = new Reference(id, start);

			while (end <= length && dictionary.contains(text.substring(start, end)))
				end++;

			if (end <= length)
				getReferencesByWord(text.substring(start, end - 1)).add(key);
		}

		keys = new TreeSet<>(referencesByWord.keySet());
	}

	public Streamlet<Reference> search(String searchKey) {
		dictionary.clear(); // Saves memory

		Iterator<String> iter = keys.tailSet(searchKey).iterator();

		return Read.from(() -> {
			String key = iter.hasNext() ? iter.next() : null;
			return key != null && key.startsWith(searchKey) ? key : null;
		}).concatMap(key -> Read.from(getReferencesByWord(key)));
	}

	public List<Reference> getReferencesByWord(String word) {
		return referencesByWord.computeIfAbsent(word, key -> new ArrayList<>());
	}

	public Map<String, List<Reference>> getReferencesByWord() {
		return referencesByWord;
	}

}
