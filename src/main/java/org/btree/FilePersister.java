package org.btree;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class FilePersister<Key, Value> implements
		Persister<B_Tree<Key, Value>.Page> {

	private String filename = "B_Tree.bt";
	private String allocMapFilename = "B_Tree.bt.alloc";

	private int branchFactor = 256;
	private ByteBufferAccessor<Key> keyAccessor;
	private ByteBufferAccessor<Value> valueAccessor;

	private B_Tree<Key, Value> btree;

	private static final int pageSize = 4096;
	private static final int maxPages = 4096;

	private static final char LEAF = 'L';
	private static final char INTERNAL = 'I';

	private RandomAccessFile file;
	private FileChannel channel;

	private RandomAccessFile allocMapFile;
	private byte allocMap[];

	public FilePersister(String filename) {
		this.filename = filename;
		this.allocMapFilename = filename + ".alloc";
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

	public B_Tree<Key, Value>.Page load(int pageNo) {
		try {
			// file.seek(pageNo * pageSize);

			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			ByteBuffer buffers[] = { buffer };
			channel.read(buffers, pageNo * pageSize, pageSize);

			B_Tree<Key, Value>.Page page = btree.new Page(pageNo);
			List<B_Tree<Key, Value>.KeyPointer> keyPointers = page.keyPointers;

			char nodeType = buffer.getChar();
			for (int i = 0; i < branchFactor; i++) {
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

	private void addLeaf(List<B_Tree<Key, Value>.KeyPointer> keyPointers,
			Key key, Value value) {
		keyPointers.add(btree.new KeyPointer(key, btree.new Leaf(value)));
	}

	private void addBranch(List<B_Tree<Key, Value>.KeyPointer> keyPointers,
			Key key, int branch) {
		keyPointers.add(btree.new KeyPointer(key, btree.new Branch(branch)));
	}

	public void save(int pageNo, B_Tree<Key, Value>.Page page) {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(pageSize);
			ByteBuffer buffers[] = { buffer };
			channel.read(buffers, pageNo * pageSize, pageSize);

			List<B_Tree<Key, Value>.KeyPointer> ptrs = page.keyPointers;

			char nodeType = (ptrs.get(0).t2 instanceof B_Tree<?, ?>.Branch) ? 'I'
					: 'L';
			buffer.putChar(nodeType);

			for (B_Tree<Key, Value>.KeyPointer keyPtr : ptrs) {
				keyAccessor.write(buffer, keyPtr.t1);

				if (keyPtr.t2 instanceof B_Tree<?, ?>.Branch) {
					@SuppressWarnings("unchecked")
					int branch = ((B_Tree<Key, Key>.Branch) keyPtr.t2).branch;
					buffer.putInt(branch);
				} else if (keyPtr.t2 instanceof B_Tree<?, ?>.Leaf) {
					@SuppressWarnings("unchecked")
					Value value = ((B_Tree<Key, Value>.Leaf) keyPtr.t2).value;
					valueAccessor.write(buffer, value);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
