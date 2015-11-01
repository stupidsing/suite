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
		public void end(boolean isComplete) {
			st.end(isComplete ? TransactionStatus.DONE____ : TransactionStatus.ROLLBACK);
			mutator.end(isComplete);
			if (isComplete)
				flush();
		}

		@Override
		public Streamlet<Key> keys(Key start, Key end) {
			return mutator.keys(start, end);
		}

		@Override
		public Value get(Key key) {
			return stm.get(st, getMemory(key));
		}

		@Override
		public void put(Key key, Value value) {
			stm.put(st, getMemory(key), value);
		}

		@Override
		public void remove(Key key) {
			stm.put(st, getMemory(key), null);
			mutator.remove(key);
		}

		private Memory<Value> getMemory(Key key) {
			return memoryByKey.computeIfAbsent(key, key_ -> stm.create(mutator.get(key_)));
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
			map.forEach((k, v) -> {
				if (v != null)
					mutator.put(k, v);
				else
					mutator.remove(k);
			});
			mutator.end(ok);
		}
	}

	public <T> T begin(Fun<KeyValueStoreMutator<Key, Value>, T> fun) {
		KeyValueStoreMutator<Key, Value> mutator = begin();
		boolean ok = false;
		try {
			T t = fun.apply(mutator);
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
