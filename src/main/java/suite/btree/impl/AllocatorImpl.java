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
		saveAllocMap(0, size);
	}

	@Override
	public int allocate() {
		return allocate(1);
	}

	@Override
	public void deallocate(int pageNo) {
		int count = 1;
		deallocate(pageNo, count);
	}

	private int allocate(int count) {
		int pageNo = findFreeExtentPages(count);

		// TODO extends allocation map if all pages are used

		updateAllocMap(pageNo, count, (byte) 1);
		return lastAllocatedPageNo = pageNo + count;
	}

	private void deallocate(int pageNo, int count) {
		updateAllocMap(pageNo, count, (byte) 0);
	}

	private int findFreeExtentPages(int count) {
		int start = lastAllocatedPageNo + 1;
		int pageNo = start;
		while ((pageNo = checkNextEmptyExtent(pageNo)) < size) {
			int pageNo0 = pageNo;
			pageNo = checkEmptyExtent(pageNo, count);
			if (pageNo - pageNo0 >= count)
				return pageNo0;
		}
		pageNo = 0;
		while ((pageNo = checkNextEmptyExtent(pageNo)) < start) {
			int pageNo0 = pageNo;
			pageNo = checkEmptyExtent(pageNo, count);
			if (pageNo - pageNo0 >= count)
				return pageNo0;
		}
		return -1;
	}

	private int checkEmptyExtent(int startPageNo, int max) {
		int endPageNo = Math.min(size, startPageNo + max);
		for (int p = startPageNo; p < endPageNo; p++)
			if (allocMap[p] != 0)
				return p;
		return endPageNo;
	}

	private int checkNextEmptyExtent(int startPageNo) {
		for (int p = startPageNo; p < size; p++)
			if (allocMap[p] != 1)
				return p;
		return size;
	}

	private void updateAllocMap(int pageNo, int count, byte b) {
		for (int p = pageNo; p < pageNo + count; p++)
			allocMap[p] = b;
		saveAllocMap(pageNo, count);
	}

	private void saveAllocMap(int pageNo, int count) {
		int p0 = pageNo / pageSize;
		int px = (pageNo + count - 1) / pageSize + 1;
		for (int p = p0; p < px; p++) {
			int start = p * pageSize, end = Math.min(size, start + pageSize);
			allocMapFile.save(p, Bytes.of(allocMap, start, end));
		}
	}

}
