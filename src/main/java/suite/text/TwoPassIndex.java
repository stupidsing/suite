package suite.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import primal.MoreVerbs.Read;
import primal.fp.Funs.Source;
import primal.streamlet.Streamlet;

public class TwoPassIndex {

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
		var length = text.length();

		for (var start = 0; start < length; start++) {
			var end = start + minimumWordLength;

			while (end <= length && dictionary.contains(text.substring(start, end)))
				end++;

			if (end <= length)
				dictionary.add(text.substring(start, end));
		}
	}

	public void pass1(String id, String text) {
		var length = text.length();

		for (var start = 0; start < length; start++) {
			var end = start + minimumWordLength;
			var key = new Reference(id, start);

			while (end <= length && dictionary.contains(text.substring(start, end)))
				end++;

			if (end <= length)
				getReferencesByWord(text.substring(start, end - 1)).add(key);
		}

		keys = new TreeSet<>(referencesByWord.keySet());
	}

	public Streamlet<Reference> search(String searchKey) {
		dictionary.clear(); // saves memory

		var iter = keys.tailSet(searchKey).iterator();

		Source<String> source = () -> {
			var key = iter.hasNext() ? iter.next() : null;
			return key != null && key.startsWith(searchKey) ? key : null;
		};

		return Read.from(() -> source).flatMap(this::getReferencesByWord);
	}

	public List<Reference> getReferencesByWord(String word) {
		return referencesByWord.computeIfAbsent(word, key -> new ArrayList<>());
	}

	public Map<String, List<Reference>> getReferencesByWord() {
		return referencesByWord;
	}

}
