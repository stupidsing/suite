package suite.btree.impl;

import java.io.IOException;
import java.io.RandomAccessFile;

import suite.btree.Allocator;

/**
 * Manages B-tree pages on disk.
 */
public class AllocatorImpl implements Allocator {

	private static int maxPages = 4096;

	private RandomAccessFile allocMapFile;
	private byte allocMap[];

	public AllocatorImpl(String filename) throws IOException {
		allocMap = new byte[maxPages];

		allocMapFile = new RandomAccessFile(filename, "rw");
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

			// TODO optimize by only saving that byte

		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
