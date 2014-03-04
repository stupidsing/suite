package suite.immutable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;
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
public class IbNameKeySet {

	private static int hashPosition = 0;
	private static int idPosition = hashPosition + 256 / 8;
	private static int pathPosition = idPosition + 1;
	private static int sizePosition = pathPosition + 24;
	private static int keyLength = sizePosition + 1;

	private List<Key> emptyKeys = Collections.<Key> emptyList();

	public static Serializer<Bytes> serializer = SerializeUtil.bytes(keyLength);

	private class Key {
		private Bytes hash; // Path prefix hashed using SHA-256
		private int id; // Comparison use only
		private Bytes path; // Path characters
		private int size; // 0 if this key has children keys

		private Key(Bytes bytes) {
			this(bytes.subbytes(hashPosition, idPosition) //
					, bytes.get(idPosition) //
					, bytes.subbytes(pathPosition, sizePosition) //
					, bytes.get(sizePosition));
		}

		private Key(Bytes hash, Bytes path, int size) {
			this(hash, 0, path, size);
		}

		private Key(Bytes hash, int id, Bytes path, int size) {
			this.hash = hash;
			this.id = id;
			this.path = path;
			this.size = size;
		}

		private Bytes toBytes() {
			return Bytes.concat(hash, Bytes.asList((byte) id), path, Bytes.asList((byte) size));
		}
	}

	private IbTree<Bytes>.Transaction transaction;

	public IbNameKeySet(IbTree<Bytes>.Transaction transaction) {
		this.transaction = transaction;
	}

	public Source<Bytes> source(Bytes start, Bytes end) {
		return transaction.source(start, end);
	}

	public Source<Bytes> list(Bytes start, Bytes end) {
		return list(emptyKeys, toKeys(start), toKeys(end));
	}

	private Source<Bytes> list(final List<Key> prefix, final List<Key> keys0, final List<Key> keys1) {
		Bytes hash = hash(toName(prefix));
		Key minKey = !keys0.isEmpty() ? Util.first(keys0) : new Key(hash, 0, Bytes.emptyBytes, 0);
		Key maxKey = !keys1.isEmpty() ? Util.first(keys1) : new Key(hash, 1, Bytes.emptyBytes, 0);
		Source<Bytes> source = transaction.source(minKey.toBytes(), increment(maxKey.toBytes()));

		return FunUtil.concat(FunUtil.map(new Fun<Bytes, Source<Bytes>>() {
			public Source<Bytes> apply(Bytes bytes) {
				Key key = new Key(bytes);
				List<Key> prefix1 = Util.add(prefix, Arrays.asList(key));

				if (key.size == 0) {
					List<Key> tailKeys0 = !keys0.isEmpty() ? Util.right(keys0, 1) : emptyKeys;
					List<Key> tailKeys1 = !keys1.isEmpty() ? Util.right(keys1, 1) : emptyKeys;
					return list(prefix1, tailKeys0, tailKeys1);
				} else
					return To.source(Arrays.asList(toName(prefix1)));
			}
		}, source));
	}

	public void add(Bytes name) {
		for (Key key : toKeys(name))
			transaction.add(key.toBytes());
	}

	public void remove(Bytes name) {
		List<Key> keys = toKeys(name);

		for (int i = keys.size() - 1; i >= 0; i--) {
			Key key = keys.get(i);

			if (key.size == 0) {
				Bytes hash = hash(toName(keys.subList(0, i + 1)));
				Key minKey = new Key(hash, 0, Bytes.emptyBytes, 0);
				Key maxKey = new Key(hash, 1, Bytes.emptyBytes, 0);
				if (transaction.source(minKey.toBytes(), maxKey.toBytes()) == null)
					transaction.remove(key.toBytes());
			} else
				transaction.remove(key.toBytes());
		}
	}

	private List<Key> toKeys(Bytes name) {
		name = Bytes.concat();
		List<Key> keys = new ArrayList<>();
		int pos = 0, size = name.size();

		while (pos < size) {
			int pathLength = sizePosition - pathPosition;
			int pos1 = Math.min(pos + pathLength, size);
			keys.add(new Key(hash(name.subbytes(0, pos)) //
					, pad(name.subbytes(pos, pos1), pathLength, (byte) 0) //
					, pos1 == size ? pos1 - pos : 0));
			pos = pos1;
		}

		return keys;
	}

	private Bytes toName(List<Key> keys) {
		BytesBuilder bb = new BytesBuilder();
		for (Key key : keys)
			bb.append(key.path.subbytes(0, key.size));
		return bb.toBytes();
	}

	private Bytes hash(Bytes bytes) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}

		md.update(bytes.getBytes());
		return new Bytes(md.digest());
	}

	private Bytes pad(Bytes bytes, int size, byte pad) {
		BytesBuilder bb = new BytesBuilder();
		bb.append(bytes);
		while (bytes.size() < size)
			bb.append(pad);
		return bb.toBytes();
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
