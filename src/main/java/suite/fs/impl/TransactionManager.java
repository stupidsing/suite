package suite.fs.impl;

import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.streamlet.Streamlet;
import suite.concurrent.stm.ObstructionFreeStm;
import suite.concurrent.stm.ObstructionFreeStm.Memory;
import suite.concurrent.stm.Stm;
import suite.concurrent.stm.Stm.TransactionStatus;
import suite.fs.KeyValueMutator;
import suite.fs.KeyValueStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements software transaction memory in a key-value storage.
 *
 * TODO clean up memories
 *
 * @author ywsing
 */
public class TransactionManager<Key, Value> {

	private Source<KeyValueStore<Key, Value>> source;
	private ObstructionFreeStm stm = new ObstructionFreeStm();
	private Map<Key, Memory<Value>> memoryByKey = new ConcurrentHashMap<>();

	public class Transaction implements KeyValueStore<Key, Value> {
		private KeyValueStore<Key, Value> store;
		private Stm.Transaction st = stm.begin();

		public Transaction(KeyValueStore<Key, Value> store) {
			this.store = store;
		}

		@Override
		public KeyValueMutator<Key, Value> mutate() {
			var mutator = store.mutate();

			return new KeyValueMutator<>() {
				public Streamlet<Key> keys(Key start, Key end) {
					return mutator.keys(start, end);
				}

				public Value get(Key key) {
					return stm.get(st, getMemory(key));
				}

				public void put(Key key, Value value) {
					stm.put(st, getMemory(key), value);
				}

				public void remove(Key key) {
					stm.put(st, getMemory(key), null);
				}

				private Memory<Value> getMemory(Key key) {
					return memoryByKey.computeIfAbsent(key, key_ -> stm.newMemory(mutator.get(key_)));
				}
			};
		}

		@Override
		public void end(boolean isComplete) {
			st.end(isComplete ? TransactionStatus.DONE____ : TransactionStatus.ROLLBACK);
			if (isComplete)
				flush();
		}
	}

	public TransactionManager(Source<KeyValueStore<Key, Value>> source) {
		this.source = source;
	}

	private synchronized void flush() {
		var map = new HashMap<Key, Value>();

		var ok = stm.transaction(transaction -> {
			var keys = new ArrayList<>(memoryByKey.keySet());
			for (var key : keys)
				memoryByKey.compute(key, (key_, memory) -> {
					if (memory != null)
						map.put(key_, stm.get(transaction, memory));
					return null;
				});
			return true;
		});

		if (ok) {
			var store = source.g();
			var mutator = store.mutate();
			map.forEach((k, v) -> {
				if (v != null)
					mutator.put(k, v);
				else
					mutator.remove(k);
			});
			store.end(ok);
		}
	}

	public <T> T begin(Fun<KeyValueMutator<Key, Value>, T> fun) {
		var store = new Transaction(source.g());
		var ok = false;
		try {
			var t = fun.apply(store.mutate());
			ok = true;
			return t;
		} finally {
			store.end(ok);
		}
	}

}
