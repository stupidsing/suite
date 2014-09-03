package suite.immutable.btree.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suite.immutable.btree.IbTreeMutator;
import suite.immutable.btree.impl.FileSystemKeyUtil.NameKey;
import suite.primitive.Bytes;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

/**
 * Facilitates storage of unlimited length of filenames on the immutable B-tree.
 * This cuts up a full path into chunks of 30 characters and arrange them as a
 * trie over the original B-tree, indexed by hash values. Used for implementing
 * file systems.
 *
 * @author ywsing
 */
public class FileSystemNameKeySet {

	private List<NameKey> emptyKeys = Collections.<NameKey> emptyList();
	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();

	private IbTreeMutator<Bytes> transaction;

	public FileSystemNameKeySet(IbTreeMutator<Bytes> transaction) {
		this.transaction = transaction;
	}

	public Source<Bytes> list(Bytes bytes0, Bytes bytes1) {
		return list(emptyKeys, keyUtil.toNameKeys(bytes0), keyUtil.toNameKeys(bytes1));
	}

	private Source<Bytes> list(List<NameKey> prefix, List<NameKey> keys0, List<NameKey> keys1) {
		Bytes hash = keyUtil.hash(keyUtil.toName(prefix));
		NameKey minKey = keys0 != null && !keys0.isEmpty() ? Util.first(keys0) : boundingKey(hash, 0);
		NameKey maxKey = keys1 != null && !keys1.isEmpty() ? Util.first(keys1) : boundingKey(hash, 1);
		Source<Bytes> source = transaction.keys(keyUtil.toBytes(minKey), increment(keyUtil.toBytes(maxKey)));

		return FunUtil.concat(FunUtil.map(bytes -> {
			NameKey key = keyUtil.toNameKey(bytes);
			List<NameKey> prefix1 = Util.add(prefix, Arrays.asList(key));

			if (key.getSize() == 0) {
				List<NameKey> tailKeys0 = key == minKey ? !keys0.isEmpty() ? Util.right(keys0, 1) : emptyKeys : null;
				List<NameKey> tailKeys1 = key == maxKey ? !keys1.isEmpty() ? Util.right(keys1, 1) : emptyKeys : null;
				return list(prefix1, tailKeys0, tailKeys1);
			} else
				return To.source(Arrays.asList(keyUtil.toName(prefix1)));
		}, source));
	}

	public void add(Bytes name) {
		for (NameKey key : keyUtil.toNameKeys(name))
			transaction.put(keyUtil.toBytes(key));
	}

	public void remove(Bytes name) {
		List<NameKey> keys = keyUtil.toNameKeys(name);

		for (int i = keys.size() - 1; i >= 0; i--) {
			NameKey key = keys.get(i);

			if (key.getSize() == 0) {
				Bytes hash = keyUtil.hash(keyUtil.toName(keys.subList(0, i + 1)));
				NameKey minKey = boundingKey(hash, 0);
				NameKey maxKey = boundingKey(hash, 1);
				if (transaction.keys(keyUtil.toBytes(minKey), keyUtil.toBytes(maxKey)) == null)
					transaction.remove(keyUtil.toBytes(key));
			} else
				transaction.remove(keyUtil.toBytes(key));
		}
	}

	private NameKey boundingKey(Bytes hash, int minMax) {
		return keyUtil.toNameKey(hash, minMax, Bytes.emptyBytes, 0);
	}

	private Bytes increment(Bytes bytes) {
		if (!bytes.isEmpty()) {
			Bytes bytes1 = bytes.subbytes(0, -1);
			byte b1 = (byte) (bytes.get(-1) + 1);
			if (b1 != 0)
				return Bytes.concat(bytes1, Bytes.asList(b1));
			else
				return Bytes.concat(increment(bytes1), Bytes.asList((byte) 0));
		} else
			return bytes;
	}

}
