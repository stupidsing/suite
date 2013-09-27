package suite.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import suite.util.DefaultValueMap;
import suite.util.FunUtil.Fun;

public class Indexer {

	private DefaultValueMap<String, List<Key>> keysByMatch = new DefaultValueMap<String, List<Key>>(new Fun<String, List<Key>>() {
		public List<Key> apply(String key) {
			return new ArrayList<>();
		}
	});

	public class Key {
		private String id;
		private int offset;

		public Key(String id, int offset) {
			this.id = id;
			this.offset = offset;
		}

		public String getId() {
			return id;
		}

		public int getOffset() {
			return offset;
		}
	}

	public void index(String id, String text) {
		int length = text.length();

		for (int start = 0; start < length; start++) {
			int end = start + 1;

			List<Key> list = null;

			while (end < length && !(list = keysByMatch.get(text.substring(start, end))).isEmpty())
				end++;

			if (list != null)
				list.add(new Key(id, start));
		}
	}

	public Collection<Key> getKeys(String match) {
		return keysByMatch.get(match);
	}

	public DefaultValueMap<String, List<Key>> getKeysByMatch() {
		return keysByMatch;
	}

}
