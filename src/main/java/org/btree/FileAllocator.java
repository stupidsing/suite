package org.btree;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Manages B-tree pages on disk.
 */
public class FileAllocator implements Allocator, Closeable {

	private static final int maxPages = 4096;

	private RandomAccessFile allocMapFile;
	private byte allocMap[];

	public FileAllocator(String filename) throws IOException {
		this.allocMap = new byte[maxPages];

		allocMapFile = new RandomAccessFile(filename + ".alloc", "rw");
		int allocMapSize = Math.max(maxPages, (int) allocMapFile.length());
		allocMap = new byte[allocMapSize];
		allocMapFile.read(allocMap);
	}

	@Override
	public void close() throws IOException {
		allocMapFile.close();
	}

	@Override
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

	@Override
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

}
