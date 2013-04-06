package org.btree;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.util.IoUtil;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same accessor should always put in same number of bytes.
 */
public interface Serializer<V> {

	public V read(ByteBuffer buffer);

	public void write(ByteBuffer buffer, V value);

	public static class B_TreeSuperBlockSerializer<Key, Value> implements
			Serializer<B_Tree<Key, Value>.SuperBlock> {
		private B_Tree<Key, Value> b_tree;
		private IntSerializer intSerializer = new IntSerializer();

		public B_TreeSuperBlockSerializer(B_Tree<Key, Value> b_tree) {
			this.b_tree = b_tree;
		}

		public B_Tree<Key, Value>.SuperBlock read(ByteBuffer buffer) {
			B_Tree<Key, Value>.SuperBlock superBlock = b_tree.new SuperBlock();
			superBlock.root = intSerializer.read(buffer);
			return superBlock;
		}

		public void write(ByteBuffer buffer, B_Tree<Key, Value>.SuperBlock value) {
			intSerializer.write(buffer, value.root);
		}
	}

	public static class B_TreePageSerializer<Key, Value> implements
			Serializer<B_Tree<Key, Value>.Page> {
		private B_Tree<Key, Value> b_tree;
		private Serializer<Key> keyAccessor;
		private Serializer<Value> valueAccessor;

		private static final char LEAF = 'L';
		private static final char BRANCH = 'I';

		public B_TreePageSerializer(B_Tree<Key, Value> b_tree //
				, Serializer<Key> keyAccessor //
				, Serializer<Value> valueAccessor) {
			this.b_tree = b_tree;
			this.keyAccessor = keyAccessor;
			this.valueAccessor = valueAccessor;
		}

		public B_Tree<Key, Value>.Page read(ByteBuffer buffer) {
			int pageNo = buffer.getInt();
			int size = buffer.getInt();

			B_Tree<Key, Value>.Page page = b_tree.new Page(pageNo);
			List<B_Tree<Key, Value>.KeyPointer> keyPointers = page.keyPointers;

			for (int i = 0; i < size; i++) {
				char nodeType = buffer.getChar();
				Key key = keyAccessor.read(buffer);

				if (nodeType == BRANCH) {
					int branch = buffer.getInt();
					addBranch(keyPointers, key, branch);
				} else if (nodeType == LEAF) {
					Value value = valueAccessor.read(buffer);
					addLeaf(keyPointers, key, value);
				}
			}

			return page;
		}

		public void write(ByteBuffer buffer, B_Tree<Key, Value>.Page page) {
			List<B_Tree<Key, Value>.KeyPointer> keyPointers = page.keyPointers;

			buffer.putInt(page.pageNo);
			buffer.putInt(keyPointers.size());

			for (B_Tree<Key, Value>.KeyPointer kp : keyPointers)
				if (kp.pointer instanceof B_Tree.Branch) {
					buffer.putChar(BRANCH);
					keyAccessor.write(buffer, kp.key);
					buffer.putInt(b_tree.getBranchPageNo(kp));
				} else if (kp.pointer instanceof B_Tree.Leaf) {
					buffer.putChar(LEAF);
					keyAccessor.write(buffer, kp.key);
					valueAccessor.write(buffer, b_tree.getLeafValue(kp));
				}
		}

		private void addLeaf(List<B_Tree<Key, Value>.KeyPointer> kps, Key k,
				Value v) {
			kps.add(b_tree.new KeyPointer(k, b_tree.new Leaf(v)));
		}

		private void addBranch(List<B_Tree<Key, Value>.KeyPointer> kps, Key k,
				int branch) {
			kps.add(b_tree.new KeyPointer(k, b_tree.new Branch(branch)));
		}

	}

	public static class FixedStringSerializer implements Serializer<String> {
		private int length;

		public FixedStringSerializer(int length) {
			this.length = length;
		}

		public String read(ByteBuffer buffer) {
			byte bs[] = new byte[length];
			int l = buffer.getInt();
			buffer.get(bs);
			return new String(bs, IoUtil.charset).substring(0, l);
		}

		public void write(ByteBuffer buffer, String value) {
			byte bs[] = Arrays.copyOf(value.getBytes(IoUtil.charset), length);
			buffer.putInt(value.length());
			buffer.put(bs);
		}
	}

	public static class IntSerializer implements Serializer<Integer> {
		public Integer read(ByteBuffer buffer) {
			return buffer.getInt();
		}

		public void write(ByteBuffer buffer, Integer value) {
			buffer.putInt(value);
		}
	}

}
