package suite.immutable.btree.impl;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class FileSystemKeyUtil {

	private int hashPosition = 0;
	private int idPosition = hashPosition + 256 / 8;
	private int pathPosition = idPosition + 1;
	private int sizePosition = pathPosition + 24;

	private int keyLength = sizePosition + 1;

	public class NameKey extends Key {
		private Bytes path; // Path characters
		private int size; // 0 if this key has children keys

		private NameKey(Bytes hash, int id, Bytes path, int size) {
			super(hash, id);
			this.path = path;
			this.size = size;
		}

		public Bytes toBytes() {
			return Bytes.concat(super.toBytes(), path, Bytes.asList((byte) size)).pad(keyLength, (byte) 0);
		}

		public Bytes getPath() {
			return path;
		}

		public int getSize() {
			return size;
		}
	}

	public class DataKey extends Key {
		private int seq;

		private DataKey(Bytes hash, int id, int seq) {
			super(hash, id);
			this.seq = seq;
		}

		public Bytes toBytes() {
			byte bs[] = ByteBuffer.allocate(4).putInt(Integer.reverseBytes(seq)).array();
			return Bytes.concat(super.toBytes(), new Bytes(bs)).pad(keyLength, (byte) 0);
		}

		public int getSeq() {
			return seq;
		}
	}

	private class Key {
		private Bytes hash; // Path prefix hashed using SHA-256
		private int id; // 0 - Name key, 1 - comparer, 64 - data, 65 - size

		private Key(Bytes hash, int id) {
			this.hash = hash;
			this.id = id;
		}

		public Bytes toBytes() {
			return Bytes.concat(hash, Bytes.asList((byte) id));
		}

		public Bytes getHash() {
			return hash;
		}

		public int getId() {
			return id;
		}
	}

	public Bytes toName(List<NameKey> keys) {
		BytesBuilder bb = new BytesBuilder();
		for (NameKey key : keys)
			if (key.size > 0)
				bb.append(key.path.subbytes(0, key.size));
			else
				bb.append(key.path);
		return bb.toBytes();
	}

	public List<NameKey> toNameKeys(Bytes name) {
		if (name != null) {
			List<NameKey> keys = new ArrayList<>();
			int pos = 0, size = name.size();

			while (pos < size) {
				int pathLength = sizePosition - pathPosition;
				int pos1 = Math.min(pos + pathLength, size);
				keys.add(toNameKey(hash(name.subbytes(0, pos)) //
						, 0 //
						, name.subbytes(pos, pos1).pad(pathLength, (byte) 0) //
						, pos1 == size ? pos1 - pos : 0));
				pos = pos1;
			}

			return keys;
		} else
			return null;
	}

	public NameKey toNameKey(Bytes bytes) {
		return new NameKey(bytes.subbytes(hashPosition, idPosition) //
				, bytes.get(idPosition) //
				, bytes.subbytes(pathPosition, sizePosition) //
				, bytes.get(sizePosition));
	}

	public NameKey toNameKey(Bytes hash, int id, Bytes path, int size) {
		return new NameKey(hash, id, path, size);
	}

	public DataKey toDataKey(Bytes hash, int id, int seq) {
		return new DataKey(hash, id, seq);
	}

	public Bytes hash(Bytes bytes) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}

		md.update(bytes.getBytes());
		return new Bytes(md.digest());
	}

	public Serializer<Bytes> serializer() {
		return SerializeUtil.bytes(keyLength);
	}

}
