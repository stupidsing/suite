package suite.btree.impl;

import java.io.IOException;
import java.util.Arrays;

import suite.btree.Allocator;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;

/**
 * Manage B-tree pages on disk.
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

		int pageNo = 0, p = 0;

		while (pageNo < size) {
			int pageNo1 = Math.min(size, pageNo + pageSize);
			System.arraycopy(allocMapFile.load(p++).toBytes(), 0, allocMap, pageNo, pageNo1 - pageNo);
			pageNo = pageNo1;
		}
	}

	@Override
	public void close() throws IOException {
		allocMapFile.close();
	}

	@Override
	public void create() {
		Arrays.fill(allocMap, (byte) 0);

		for (int pageNo = 0; pageNo < size; pageNo = Math.min(size, pageNo + pageSize))
			savePageNo(pageNo);
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
		int start = p * pageSize, end = Math.min(size, start + pageSize);
		allocMapFile.save(p, Bytes.of(allocMap, start, end));
	}

}
