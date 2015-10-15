package suite.fs.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import suite.fs.KeyValueStoreMutator;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Source;

/**
 * Implements software transaction memory in a key-value storage.
 *
 * Now this is implemented using exclusive locking for both read and write.
 *
 * @author ywsing
 */
public class TransactionManager<Key, Value> {

	private Source<KeyValueStoreMutator<Key, Value>> source;
	private Map<Key, Transaction> transactionByKey = new ConcurrentHashMap<>();

	public class Transaction implements KeyValueStoreMutator<Key, Value> {
		private KeyValueStoreMutator<Key, Value> mutator;
		private List<Key> keys = new ArrayList<>();

		public Transaction(KeyValueStoreMutator<Key, Value> mutator) {
			this.mutator = mutator;
		}

		@Override
		public void commit() {
			mutator.commit();

			// Clean up keys table
			keys.forEach(transactionByKey::remove);
		}

		@Override
		public Streamlet<Key> keys(Key start, Key end) {
			return acquireReads(mutator.keys(start, end));
		}

		@Override
		public Value get(Key key) {
			acquireRead(key);
			return mutator.get(key);
		}

		@Override
		public void put(Key key, Value value) {
			acquireWrite(key);
			mutator.put(key, value);
		}

		@Override
		public void remove(Key key) {
			acquireWrite(key);
			mutator.remove(key);
		}

		private Streamlet<Key> acquireReads(Streamlet<Key> st) {
			return Read.from(() -> {
				Key key = st.first();
				if (key != null)
					acquireRead(key);
				return key;
			});
		}

		private void acquireRead(Key key) {
			acquireOwnership(key);
		}

		private void acquireWrite(Key key) {
			acquireOwnership(key);
		}

		private void acquireOwnership(Key key) {
			while (transactionByKey.putIfAbsent(key, this) != this)
				backoff();
			keys.add(key);
		}

		private void backoff() {
			try {
				Thread.sleep(300 + ThreadLocalRandom.current().nextInt(500));
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public TransactionManager(Source<KeyValueStoreMutator<Key, Value>> source) {
		this.source = source;
	}

	public KeyValueStoreMutator<Key, Value> begin() {
		return new Transaction(source.source());
	}

}
