package suite.text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import suite.util.DefaultValueMap;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class TwoPassIndexer {

	private static final int minimumWordLength = 3;

	private Set<String> dictionary = new HashSet<>();

	private TreeSet<String> keys = new TreeSet<>();

	private DefaultValueMap<String, List<Reference>> referencesByWord = new DefaultValueMap<>(new Fun<String, List<Reference>>() {
		public List<Reference> apply(String word) {
			return new ArrayList<>();
		}
	});

	public static class Reference {
		private String id;
		private int offset;

		public Reference(String id, int offset) {
			this.id = id;
			this.offset = offset;
		}

		public String toString() {
			return id + "(" + offset + ")";
		}

		public String getId() {
			return id;
		}

		public int getOffset() {
			return offset;
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
				referencesByWord.get(text.substring(start, end - 1)).add(key);
		}

		keys = new TreeSet<>(referencesByWord.keySet());
	}

	public Source<Reference> search(final String searchKey) {
		dictionary.clear(); // Saves memory

		Source<String> keySource = new Source<String>() {
			private Iterator<String> iter = keys.tailSet(searchKey).iterator();

			public String source() {
				String key = iter.hasNext() ? iter.next() : null;
				return key != null && key.startsWith(searchKey) ? key : null;
			}
		};

		return FunUtil.concat(FunUtil.map(new Fun<String, Source<Reference>>() {
			public Source<Reference> apply(String key) {
				return FunUtil.asSource(referencesByWord.get(key));
			}
		}, keySource));
	}

	public DefaultValueMap<String, List<Reference>> getKeysByWord() {
		return referencesByWord;
	}

}
