package org.btree;

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
public class FilePersister<Key, Value> implements Persister<B_Tree.Page<Key>> {

	private String filename = "B_Tree.bt";
	private String allocMapFilename = "B_Tree.bt.alloc";

	private ByteBufferAccessor<Key> keyAccessor;
	private ByteBufferAccessor<Value> valueAccessor;

	private static final int pageSize = 4096;
	private static final int maxPages = 4096;

	private static final char LEAF = 'L';
	private static final char INTERNAL = 'I';

	private RandomAccessFile file;
	private FileChannel channel;

	private RandomAccessFile allocMapFile;
	private byte allocMap[];

	public FilePersister(String filename, ByteBufferAccessor<Key> keyAccessor,
			ByteBufferAccessor<Value> valueAccessor) {
		this.filename = filename;
		this.allocMapFilename = filename + ".alloc";
		this.allocMap = new byte[maxPages];
		this.keyAccessor = keyAccessor;
		this.valueAccessor = valueAccessor;
	}

	public void start() throws IOException {
		file = new RandomAccessFile(filename, "rw");
		channel = file.getChannel();

		allocMapFile = new RandomAccessFile(allocMapFilename, "rw");
		int allocMapSize = Math.max(maxPages, (int) allocMapFile.length());
		allocMap = new byte[allocMapSize];
		allocMapFile.read(allocMap);
	}

	public void stop() throws IOException {
		allocMapFile.close();
		channel.close();
		file.close();
	}

	public int allocate() {
		int pageNo;
		for (pageNo = 0; pageNo < allocMap.length; pageNo++)
			if (allocMap[pageNo] == 0) {
				allocMap[pageNo] = 1;
				break;
			}

		// TODO extends allocation map if all pages are used

		saveAllocMap();
		return pageNo;
	}

	public void deallocate(int pageNo) {
		allocMap[pageNo] = 0;
		saveAllocMap();
	}

	private void saveAllocMap() {
		try {
			allocMapFile.seek(0);
			allocMapFile.write(allocMap);

			// TODO optimize to only save that byte

		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public B_Tree.Page<Key> load(int pageNo) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			channel.read(buffer, pageNo * pageSize);
			buffer.rewind();

			B_Tree.Page<Key> page = new B_Tree.Page<>(pageNo);
			List<B_Tree.KeyPointer<Key>> keyPointers = page.keyPointers;

			char nodeType = buffer.getChar();
			int size = buffer.getInt();

			for (int i = 0; i < size; i++) {
				Key key = keyAccessor.read(buffer);

				if (nodeType == INTERNAL) {
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

	public void save(int pageNo, B_Tree.Page<Key> page) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			List<B_Tree.KeyPointer<Key>> ptrs = page.keyPointers;
			boolean isBranch = !ptrs.isEmpty()
					&& ptrs.get(0).t2 instanceof B_Tree.Branch;

			buffer.putChar(isBranch ? INTERNAL : LEAF);
			buffer.putInt(ptrs.size());

			for (B_Tree.KeyPointer<Key> keyPtr : ptrs) {
				keyAccessor.write(buffer, keyPtr.t1);

				if (keyPtr.t2 instanceof B_Tree.Branch) {
					int branch = ((B_Tree.Branch) keyPtr.t2).branch;
					buffer.putInt(branch);
				} else if (keyPtr.t2 instanceof B_Tree.Leaf) {
					@SuppressWarnings("unchecked")
					Value value = ((B_Tree.Leaf<Value>) keyPtr.t2).value;
					valueAccessor.write(buffer, value);
				}
			}

			buffer.flip();
			channel.write(buffer, pageNo * pageSize);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
