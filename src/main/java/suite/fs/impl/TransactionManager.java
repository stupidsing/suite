package suite.fs.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import suite.fs.KeyValueStoreMutator;
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
	private Map<Key, Ownership> ownershipsByKey = new ConcurrentHashMap<>();

	private class Ownership {
		private Transaction transaction;

		private Ownership(TransactionManager<Key, Value>.Transaction transaction) {
			this.transaction = transaction;
		}
	}

	public class Transaction implements KeyValueStoreMutator<Key, Value> {
		private KeyValueStoreMutator<Key, Value> mutator;
		private List<Key> keys = new ArrayList<>();

		public Transaction(KeyValueStoreMutator<Key, Value> mutator) {
			this.mutator = mutator;
		}

		public void commit() {
			mutator.commit();

			// Clean up keys table
			keys.forEach(ownershipsByKey::remove);
		}

		public Source<Key> keys() {
			return mutator.keys();
		}

		public Source<Key> keys(Key start, Key end) {
			return mutator.keys(start, end);
		}

		public Value get(Key key) {
			acquireOwnership(key);
			return mutator.get(key);
		}

		public void put(Key key, Value data) {
			acquireOwnership(key);
			mutator.put(key, data);
		}

		public void remove(Key key) {
			acquireOwnership(key);
			mutator.remove(key);
		}

		private Ownership acquireOwnership(Key key) {
			Ownership ownership;
			while (ownershipsByKey.putIfAbsent(key, ownership = new Ownership(this)).transaction != this)
				backoff();
			keys.add(key);
			return ownership;
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
