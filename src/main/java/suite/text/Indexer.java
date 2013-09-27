package suite.text;

public class Indexer {

	public static class Key {
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

}
