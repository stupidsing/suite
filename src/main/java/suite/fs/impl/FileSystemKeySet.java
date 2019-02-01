package suite.fs.impl;

import java.util.List;

import suite.fs.KeyDataStore;
import suite.fs.impl.FileSystemKeyUtil.NameKey;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.List_;

/**
 * Facilitates storage of unlimited length of filenames on the persistent
 * B-tree. This cuts up a full path into chunks of 24 characters and arrange
 * them as a trie over the original B-tree, indexed by hash values. Used for
 * implementing file systems.
 *
 * @author ywsing
 */
public class FileSystemKeySet {

	private List<NameKey> emptyKeys = List.of();
	private FileSystemKeyUtil keyUtil;
	private KeyDataStore<Bytes> store;

	public FileSystemKeySet(FileSystemKeyUtil keyUtil, KeyDataStore<Bytes> store) {
		this.keyUtil = keyUtil;
		this.store = store;
	}

	public Streamlet<Bytes> list(Bytes bytes0, Bytes bytes1) {
		return list(emptyKeys, keyUtil.toNameKeys(bytes0), keyUtil.toNameKeys(bytes1));
	}

	private Streamlet<Bytes> list(List<NameKey> prefix, List<NameKey> keys0, List<NameKey> keys1) {
		var hash = keyUtil.hash(keyUtil.toName(prefix));
		var minKey = keys0 != null && !keys0.isEmpty() ? List_.first(keys0) : boundingKey(hash, 0);
		var maxKey = keys1 != null && !keys1.isEmpty() ? List_.first(keys1) : boundingKey(hash, 1);
		var st = store.mutateData().keys(keyUtil.toBytes(minKey), increment(keyUtil.toBytes(maxKey)));

		return st.concatMap(bytes -> {
			var key = keyUtil.toNameKey(bytes);
			var prefix1 = List_.concat(prefix, List.of(key));

			if (key.size == 0) {
				var tailKeys0 = key == minKey ? !keys0.isEmpty() ? List_.right(keys0, 1) : emptyKeys : null;
				var tailKeys1 = key == maxKey ? !keys1.isEmpty() ? List_.right(keys1, 1) : emptyKeys : null;
				return list(prefix1, tailKeys0, tailKeys1);
			} else
				return Read.each(keyUtil.toName(prefix1));
		});
	}

	public void add(Bytes name) {
		var mutator = store.mutateData();
		for (var key : keyUtil.toNameKeys(name))
			mutator.putTerminal(keyUtil.toBytes(key));
	}

	public void remove(Bytes name) {
		var mutator = store.mutateData();
		var keys = keyUtil.toNameKeys(name);

		for (var i = keys.size() - 1; 0 <= i; i--) {
			var key = keys.get(i);

			if (key.size == 0) {
				var hash = keyUtil.hash(keyUtil.toName(keys.subList(0, i + 1)));
				var minKey = boundingKey(hash, 0);
				var maxKey = boundingKey(hash, 1);
				if (mutator.keys(keyUtil.toBytes(minKey), keyUtil.toBytes(maxKey)) == null)
					mutator.removeTerminal(keyUtil.toBytes(key));
			} else
				mutator.removeTerminal(keyUtil.toBytes(key));
		}
	}

	private NameKey boundingKey(Bytes hash, int minMax) {
		return keyUtil.toNameKey(hash, minMax, Bytes.empty, 0);
	}

	private Bytes increment(Bytes bytes) {
		if (!bytes.isEmpty()) {
			var bytes1 = bytes.range(0, -1);
			var b1 = (byte) (bytes.get(-1) + 1);
			if (b1 != 0)
				return Bytes.concat(bytes1, Bytes.of(b1));
			else
				return Bytes.concat(increment(bytes1), Bytes.of((byte) 0));
		} else
			return bytes;
	}

}
