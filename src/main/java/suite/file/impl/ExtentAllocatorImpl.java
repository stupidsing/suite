package suite.file.impl;

import java.io.IOException;

import suite.file.ExtentAllocator;
import suite.file.PageAllocator;
import suite.file.PageFile;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Fun;

/**
 * Manage B-tree pages on disk.
 */
public class ExtentAllocatorImpl implements PageAllocator, ExtentAllocator {

	private int size = 4096;
	private int pageSize = PageFile.defaultPageSize;

	private SerializedPageFile<Bytes> allocMapFile;
	private int lastAllocatedPageNo;

	public ExtentAllocatorImpl(SerializedPageFile<Bytes> pageFile) throws IOException {
		allocMapFile = pageFile;
	}

	@Override
	public synchronized void close() throws IOException {
		allocMapFile.close();
	}

	@Override
	public synchronized void create() {
		updateAllocMap(0, size, (byte) 0);
	}

	@Override
	public synchronized int allocate() {
		return allocate_(1);
	}

	@Override
	public synchronized void deallocate(int pageNo) {
		int count = 1;
		deallocate_(pageNo, count);
	}

	@Override
	public ExtentPointer allocate(int count) {
		return new ExtentPointer(allocate_(count), count);
	}

	@Override
	public void deallocate(ExtentPointer pointer) {
		deallocate_(pointer.pageNo, pointer.count);
	}

	private int allocate_(int count) {
		int pageNo = findFreeExtentPages(count);

		// TODO extends allocation map if all pages are used

		updateAllocMap(pageNo, pageNo + count, (byte) 1);
		return lastAllocatedPageNo = pageNo + count;
	}

	private void deallocate_(int pageNo, int count) {
		updateAllocMap(pageNo, pageNo + count, (byte) 0);
	}

	private int findFreeExtentPages(int count) {
		Fun<Integer, Byte> read = new Fun<Integer, Byte>() {
			private int start, end;
			private Bytes bytes;

			public Byte apply(Integer pageNo) {
				if (bytes == null || pageNo < start || end <= pageNo) {
					int p = pageNo / pageSize;
					start = p * pageSize;
					end = start + pageSize;
					bytes = allocMapFile.load(p);
				}
				return bytes.get(pageNo - start);
			}
		};

		int start = lastAllocatedPageNo + 1;
		int pos = start;
		while ((pos = checkNextEmptyExtent(read, pos)) < size) {
			int pos0 = pos;
			pos = checkEmptyExtent(read, pos, count);
			if (pos - pos0 >= count)
				return pos0;
		}
		pos = 0;
		while ((pos = checkNextEmptyExtent(read, pos)) < start) {
			int pos0 = pos;
			pos = checkEmptyExtent(read, pos, count);
			if (pos - pos0 >= count)
				return pos0;
		}
		return -1;
	}

	private void updateAllocMap(int start, int end, byte b) {
		while (start < end) {
			int p = start / pageSize;
			int s = p * pageSize;
			int e = s + pageSize;
			int end_ = Math.min(e, end);
			int p0 = 0;
			int p1 = start - s;
			int p2 = end_ - s;
			int p3 = pageSize;
			Bytes bytes = allocMapFile.load(p);

			BytesBuilder bb = new BytesBuilder();
			bb.append(bytes.subbytes(p0, p1));
			for (int i = p1; i < p2; i++)
				bb.append(b);
			bb.append(bytes.subbytes(p2, p3));

			allocMapFile.save(p, bb.toBytes());
			start = end_;
		}
	}

	private int checkEmptyExtent(Fun<Integer, Byte> read, int pos, int max) {
		int end = Math.min(size, pos + max);
		while (pos < end && read.apply(pos) == 0)
			pos++;
		return pos;
	}

	private int checkNextEmptyExtent(Fun<Integer, Byte> read, int pos) {
		while (pos < size && read.apply(pos) == 1)
			pos++;
		return pos;
	}

}
