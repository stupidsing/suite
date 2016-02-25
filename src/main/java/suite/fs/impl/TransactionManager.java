package suite.fs.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.concurrent.ObstructionFreeStm;
import suite.concurrent.ObstructionFreeStm.Memory;
import suite.concurrent.Stm;
import suite.concurrent.Stm.TransactionStatus;
import suite.fs.KeyValueStore;
import suite.fs.KeyValueStoreMutator;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

/**
 * Implements software transaction memory in a key-value storage.
 *
 * TODO clean up memories
 *
 * @author ywsing
 */
public class TransactionManager<Key, Value> {

	private Source<KeyValueStoreMutator<Key, Value>> source;
	private ObstructionFreeStm stm = new ObstructionFreeStm();
	private Map<Key, Memory<Value>> memoryByKey = new ConcurrentHashMap<>();

	public class Transaction implements KeyValueStoreMutator<Key, Value> {
		private KeyValueStoreMutator<Key, Value> mutator;
		private Stm.Transaction st = stm.begin();

		public Transaction(KeyValueStoreMutator<Key, Value> mutator) {
			this.mutator = mutator;
		}

		@Override
		public KeyValueStore<Key, Value> store() {
			KeyValueStore<Key, Value> store = mutator.store();

			return new KeyValueStore<Key, Value>() {
				public Streamlet<Key> keys(Key start, Key end) {
					return store.keys(start, end);
				}

				public Value get(Key key) {
					return stm.get(st, getMemory(key));
				}

				public void put(Key key, Value value) {
					stm.put(st, getMemory(key), value);
				}

				public void remove(Key key) {
					stm.put(st, getMemory(key), null);
					store.remove(key);
				}

				private Memory<Value> getMemory(Key key) {
					return memoryByKey.computeIfAbsent(key, key_ -> stm.create(store.get(key_)));
				}
			};
		}

		@Override
		public void end(boolean isComplete) {
			st.end(isComplete ? TransactionStatus.DONE____ : TransactionStatus.ROLLBACK);
			mutator.end(isComplete);
			if (isComplete)
				flush();
		}
	}

	public TransactionManager(Source<KeyValueStoreMutator<Key, Value>> source) {
		this.source = source;
	}

	private synchronized void flush() {
		Map<Key, Value> map = new HashMap<>();

		boolean ok = stm.transaction(transaction -> {
			List<Key> keys = new ArrayList<>(memoryByKey.keySet());
			for (Key key : keys)
				memoryByKey.compute(key, (key_, memory) -> {
					if (memory != null)
						map.put(key_, stm.get(transaction, memory));
					return null;
				});
			return true;
		});

		if (ok) {
			KeyValueStoreMutator<Key, Value> mutator = source.source();
			KeyValueStore<Key, Value> store = mutator.store();
			map.forEach((k, v) -> {
				if (v != null)
					store.put(k, v);
				else
					store.remove(k);
			});
			mutator.end(ok);
		}
	}

	public <T> T begin(Fun<KeyValueStore<Key, Value>, T> fun) {
		KeyValueStoreMutator<Key, Value> mutator = begin();
		boolean ok = false;
		try {
			T t = fun.apply(mutator.store());
			ok = true;
			return t;
		} finally {
			mutator.end(ok);
		}
	}

	public KeyValueStoreMutator<Key, Value> begin() {
		return new Transaction(source.source());
	}

}
