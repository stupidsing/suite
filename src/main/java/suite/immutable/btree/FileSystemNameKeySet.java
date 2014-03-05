package suite.immutable.btree;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suite.immutable.btree.FileSystemKeyUtil.NameKey;
import suite.primitive.Bytes;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
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

	private IbTree<Bytes>.Transaction transaction;

	public FileSystemNameKeySet(IbTree<Bytes>.Transaction transaction) {
		this.transaction = transaction;
	}

	public Source<Bytes> list(Bytes start, Bytes end) {
		return list(emptyKeys, keyUtil.toNameKeys(start), keyUtil.toNameKeys(end));
	}

	private Source<Bytes> list(final List<NameKey> prefix, final List<NameKey> keys0, final List<NameKey> keys1) {
		Bytes hash = keyUtil.hash(keyUtil.toName(prefix));
		NameKey minKey = !keys0.isEmpty() ? Util.first(keys0) : keyUtil.toNameKey(hash, 0, Bytes.emptyBytes, 0);
		NameKey maxKey = !keys1.isEmpty() ? Util.first(keys1) : keyUtil.toNameKey(hash, 1, Bytes.emptyBytes, 0);
		Source<Bytes> source = transaction.source(minKey.toBytes(), increment(maxKey.toBytes()));

		return FunUtil.concat(FunUtil.map(new Fun<Bytes, Source<Bytes>>() {
			public Source<Bytes> apply(Bytes bytes) {
				NameKey key = keyUtil.toNameKey(bytes);
				List<NameKey> prefix1 = Util.add(prefix, Arrays.asList(key));

				if (key.getSize() == 0) {
					List<NameKey> tailKeys0 = !keys0.isEmpty() ? Util.right(keys0, 1) : emptyKeys;
					List<NameKey> tailKeys1 = !keys1.isEmpty() ? Util.right(keys1, 1) : emptyKeys;
					return list(prefix1, tailKeys0, tailKeys1);
				} else
					return To.source(Arrays.asList(keyUtil.toName(prefix1)));
			}
		}, source));
	}

	public void add(Bytes name) {
		for (NameKey key : keyUtil.toNameKeys(name))
			transaction.add(key.toBytes());
	}

	public void remove(Bytes name) {
		List<NameKey> keys = keyUtil.toNameKeys(name);

		for (int i = keys.size() - 1; i >= 0; i--) {
			NameKey key = keys.get(i);

			if (key.getSize() == 0) {
				Bytes hash = keyUtil.hash(keyUtil.toName(keys.subList(0, i + 1)));
				NameKey minKey = keyUtil.toNameKey(hash, 0, Bytes.emptyBytes, 0);
				NameKey maxKey = keyUtil.toNameKey(hash, 1, Bytes.emptyBytes, 0);
				if (transaction.source(minKey.toBytes(), maxKey.toBytes()) == null)
					transaction.remove(key.toBytes());
			} else
				transaction.remove(key.toBytes());
		}
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
