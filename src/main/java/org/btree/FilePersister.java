package org.btree;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * Persists B-tree pages to file on disk.
 * 
 * The following must holds when using this class:
 * 
 * pageSize >= sizeof(char) + sizeof(int) + branchFactor * sizeof(int)
 * 
 * pageSize >= sizeof(char) + sizeof(int) + leafFactor * sizeof(Value)
 */
public class FilePersister<Key, Value> implements
		Persister<B_Tree<Key, Value>.Page>, Closeable {

	private B_Tree<Key, Value> b_tree;
	private Serializer<Key> keyAccessor;
	private Serializer<Value> valueAccessor;

	private static final int pageSize = 4096;

	private static final char LEAF = 'L';
	private static final char BRANCH = 'I';

	private RandomAccessFile file;
	private FileChannel channel;

	public FilePersister(B_Tree<Key, Value> b_tree //
			, String filename //
			, Serializer<Key> keyAccessor //
			, Serializer<Value> valueAccessor) throws FileNotFoundException {
		this.b_tree = b_tree;
		this.keyAccessor = keyAccessor;
		this.valueAccessor = valueAccessor;

		file = new RandomAccessFile(filename, "rw");
		channel = file.getChannel();
	}

	@Override
	public void close() throws IOException {
		channel.close();
		file.close();
	}

	@Override
	public B_Tree<Key, Value>.Page load(int pageNo) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			channel.read(buffer, pageNo * pageSize);
			buffer.rewind();

			B_Tree<Key, Value>.Page page = b_tree.new Page(pageNo);
			List<B_Tree<Key, Value>.KeyPointer> keyPointers = page.keyPointers;

			int size = buffer.getInt();

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
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void addLeaf(List<B_Tree<Key, Value>.KeyPointer> kps, Key k, Value v) {
		kps.add(b_tree.new KeyPointer(k, b_tree.new Leaf(v)));
	}

	private void addBranch(List<B_Tree<Key, Value>.KeyPointer> kps, Key k,
			int branch) {
		kps.add(b_tree.new KeyPointer(k, b_tree.new Branch(branch)));
	}

	@Override
	public void save(int pageNo, B_Tree<Key, Value>.Page page) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			List<B_Tree<Key, Value>.KeyPointer> keyPointers = page.keyPointers;

			buffer.putInt(keyPointers.size());

			for (B_Tree<Key, Value>.KeyPointer kp : keyPointers)
				if (kp.pointer instanceof B_Tree.Branch) {
					buffer.putChar(BRANCH);
					keyAccessor.write(buffer, kp.key);
					buffer.putInt(b_tree.toBranch(kp));
				} else if (kp.pointer instanceof B_Tree.Leaf) {
					buffer.putChar(LEAF);
					keyAccessor.write(buffer, kp.key);
					valueAccessor.write(buffer, b_tree.getLeafValue(kp));
				}

			buffer.flip();
			channel.write(buffer, pageNo * pageSize);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
