package suite.fs.impl;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class FileSystemKeyUtil {

	private int hashOffset = 0;
	private int idOffset = hashOffset + 256 / 8;
	private int pathOffset = idOffset + 1;
	private int sizeOffset = pathOffset + 24;

	private int keyLength = sizeOffset + 1;

	public class NameKey extends Key {
		public final Bytes path; // path characters
		public final int size; // 0 if this key has children keys

		private NameKey(Bytes hash, int id, Bytes path, int size) {
			super(hash, id);
			this.path = path;
			this.size = size;
		}
	}

	public class DataKey extends Key {
		public final int seq;

		private DataKey(Bytes hash, int id, int seq) {
			super(hash, id);
			this.seq = seq;
		}
	}

	private class Key {
		public final Bytes hash; // path prefix hashed using SHA-256
		public final int id; // 0 - Name key, 1 - comparer, 64 - data, 65 - size

		private Key(Bytes hash, int id) {
			this.hash = hash;
			this.id = id;
		}
	}

	public Bytes toBytes(NameKey key) {
		return Bytes.concat(toBytes_(key), key.path, Bytes.asList((byte) key.size)).pad(keyLength);
	}

	public Bytes toBytes(DataKey key) {
		byte[] bs = ByteBuffer.allocate(4).putInt(Integer.reverseBytes(key.seq)).array();
		return Bytes.concat(toBytes_(key), Bytes.of(bs)).pad(keyLength);
	}

	private Bytes toBytes_(Key key) {
		return Bytes.concat(key.hash, Bytes.asList((byte) key.id));
	}

	public Bytes toName(List<NameKey> keys) {
		BytesBuilder bb = new BytesBuilder();
		for (NameKey key : keys)
			if (0 < key.size)
				bb.append(key.path.range(0, key.size));
			else
				bb.append(key.path);
		return bb.toBytes();
	}

	public List<NameKey> toNameKeys(Bytes name) {
		if (name != null) {
			List<NameKey> keys = new ArrayList<>();
			int pos = 0, size = name.size();

			while (pos < size) {
				int pathLength = sizeOffset - pathOffset;
				int pos1 = Math.min(pos + pathLength, size);
				keys.add(toNameKey( //
						hash(name.range(0, pos)), //
						0, //
						name.range(pos, pos1).pad(pathLength), //
						pos1 == size ? pos1 - pos : 0));
				pos = pos1;
			}

			return keys;
		} else
			return null;
	}

	public NameKey toNameKey(Bytes bytes) {
		return new NameKey( //
				bytes.range(hashOffset, idOffset), //
				bytes.get(idOffset), //
				bytes.range(pathOffset, sizeOffset), //
				bytes.get(sizeOffset));
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

		md.update(bytes.toArray());
		return Bytes.of(md.digest());
	}

	public Serializer<Bytes> serializer() {
		return Serialize.bytes(keyLength);
	}

}
