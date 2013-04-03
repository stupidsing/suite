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
public class FilePersister<Key, Value> implements Persister<B_Tree.Page<Key>>,
		Closeable {

	private ByteBufferAccessor<Key> keyAccessor;
	private ByteBufferAccessor<Value> valueAccessor;

	private static final int pageSize = 4096;

	private static final char LEAF = 'L';
	private static final char BRANCH = 'I';

	private RandomAccessFile file;
	private FileChannel channel;

	public FilePersister(String filename //
			, ByteBufferAccessor<Key> keyAccessor //
			, ByteBufferAccessor<Value> valueAccessor)
			throws FileNotFoundException {
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
	public B_Tree.Page<Key> load(int pageNo) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			channel.read(buffer, pageNo * pageSize);
			buffer.rewind();

			B_Tree.Page<Key> page = new B_Tree.Page<>(pageNo);
			List<B_Tree.KeyPointer<Key>> keyPointers = page.keyPointers;

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

	private void addLeaf(List<B_Tree.KeyPointer<Key>> kps, Key k, Value v) {
		kps.add(new B_Tree.KeyPointer<>(k, new B_Tree.Leaf<>(v)));
	}

	private void addBranch(List<B_Tree.KeyPointer<Key>> kps, Key k, int branch) {
		kps.add(new B_Tree.KeyPointer<>(k, new B_Tree.Branch(branch)));
	}

	@Override
	public void save(int pageNo, B_Tree.Page<Key> page) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			List<B_Tree.KeyPointer<Key>> keyPointers = page.keyPointers;

			buffer.putInt(keyPointers.size());

			for (B_Tree.KeyPointer<Key> keyPointer : keyPointers)
				if (keyPointer.t2 instanceof B_Tree.Branch) {
					int branch = ((B_Tree.Branch) keyPointer.t2).branch;
					buffer.putChar(BRANCH);
					keyAccessor.write(buffer, keyPointer.t1);
					buffer.putInt(branch);
				} else if (keyPointer.t2 instanceof B_Tree.Leaf) {
					@SuppressWarnings("unchecked")
					Value value = ((B_Tree.Leaf<Value>) keyPointer.t2).value;
					buffer.putChar(LEAF);
					keyAccessor.write(buffer, keyPointer.t1);
					valueAccessor.write(buffer, value);
				}

			buffer.flip();
			channel.write(buffer, pageNo * pageSize);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
