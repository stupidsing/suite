package suite.fs.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.concurrent.ObstructionFreeStm;
import suite.concurrent.ObstructionFreeStm.Memory;
import suite.concurrent.Stm;
import suite.concurrent.Stm.TransactionStatus;
import suite.fs.KeyValueStoreMutator;
import suite.os.LogUtil;
import suite.streamlet.Streamlet;
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
		private Stm.Transaction st = stm.beginTransaction();

		public Transaction(KeyValueStoreMutator<Key, Value> mutator) {
			this.mutator = mutator;
		}

		@Override
		public void commit() {
			st.stop(TransactionStatus.DONE____);
			mutator.commit();
			flush();
		}

		public void rollback() {
			st.stop(TransactionStatus.ROLLBACK);
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

	// TODO synchronization
	public void flush() {
		KeyValueStoreMutator<Key, Value> transaction = begin();
		boolean ok = false;
		try {
			KeyValueStoreMutator<Key, Value> mutator = source.source();
			Iterator<Key> iterator = memoryByKey.keySet().iterator();
			while (iterator.hasNext()) {
				Key key = iterator.next();
				mutator.put(key, transaction.get(key));
				iterator.remove();
			}
			ok = true;
		} catch (Exception ex) {
			LogUtil.error(ex);
		} finally {
			if (ok)
				transaction.commit();
		}
	}

	public KeyValueStoreMutator<Key, Value> begin() {
		return new Transaction(source.source());
	}

}
