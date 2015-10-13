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
		savePageNos(0, size);
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

		for (int p = pageNo; p < pageNo + count; p++)
			allocMap[p] = 1;
		savePageNos(pageNo, count);
		return lastAllocatedPageNo = pageNo + count;
	}

	private void deallocate(int pageNo, int count) {
		for (int p = pageNo; p < pageNo + count; p++)
			allocMap[p] = 0;
		savePageNos(pageNo, count);
	}

	private int findFreeExtentPages(int size) {
		int start = lastAllocatedPageNo + 1;
		int pageNo = start;
		for (int i = 0; i < allocMap.length; i++)
			if (!isEmptyExtent(pageNo, size)) {
				pageNo++;
				if (pageNo == allocMap.length)
					pageNo = 0;
			} else
				return pageNo;
		return -1;
	}

	private boolean isEmptyExtent(int pageNo, int count) {
		boolean result = pageNo + count <= allocMap.length;
		for (int i = 0; result && i < count; i++) {
			result &= allocMap[pageNo] == 0;
			pageNo++;
		}
		return result;
	}

	private void savePageNos(int startPageNo, int count) {
		int p0 = startPageNo / pageSize;
		int px = (startPageNo + count - 1) / pageSize + 1;
		for (int p = p0; p < px; p++) {
			int start = p * pageSize, end = Math.min(size, start + pageSize);
			allocMapFile.save(p, Bytes.of(allocMap, start, end));
		}
	}

}
