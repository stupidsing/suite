package suite.btree.impl;

import java.io.IOException;

import suite.btree.Allocator;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;

/**
 * Manages B-tree pages on disk.
 */
public class AllocatorImpl implements Allocator {

	private int size = 4096;
	private int pageSize = PageFile.defaultPageSize;

	private SerializedPageFile<Bytes> allocMapFile;
	private byte allocMap[];

	private int lastAllocatedPageNo;

	public AllocatorImpl(SerializedPageFile<Bytes> pageFile) throws IOException {
		allocMapFile = pageFile;
		allocMap = new byte[size];

		int i = 0, p = 0;

		while (i < size) {
			int i1 = Math.min(size, i + pageSize);
			System.arraycopy(allocMapFile.load(p++).toBytes(), 0, allocMap, i, i1 - i);
			i = i1;
		}
	}

	@Override
	public void close() throws IOException {
		allocMapFile.close();
	}

	@Override
	public int allocate() {
		int pageNo = findFreePage();

		// TODO extends allocation map if all pages are used

		allocMap[pageNo] = 1;
		savePageNo(pageNo);
		return lastAllocatedPageNo = pageNo;
	}

	@Override
	public void deallocate(int pageNo) {
		allocMap[pageNo] = 0;
		savePageNo(pageNo);
	}

	private int findFreePage() {
		int start = lastAllocatedPageNo + 1;
		for (int pageNo = start; pageNo < allocMap.length; pageNo++)
			if (allocMap[pageNo] == 0)
				return pageNo;
		for (int pageNo = 0; pageNo < start; pageNo++)
			if (allocMap[pageNo] == 0)
				return pageNo;
		return -1;
	}

	private void savePageNo(int pageNo) {
		int p = pageNo / pageSize;
		int start = p * pageSize, end = start + pageSize;
		allocMapFile.save(p, Bytes.of(allocMap, start, end));
	}

}
