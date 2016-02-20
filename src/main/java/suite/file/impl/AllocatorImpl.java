package suite.file.impl;

import java.io.IOException;

import suite.file.ExtentAllocator;
import suite.file.PageAllocator;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Fun;

/**
 * Manage B-tree pages on disk.
 */
public class AllocatorImpl implements PageAllocator, ExtentAllocator {

	private int size = 4096;
	private int pageSize = PageFile.defaultPageSize;

	private SerializedPageFile<Bytes> allocMapFile;
	private int lastAllocatedPointer;

	public AllocatorImpl(SerializedPageFile<Bytes> pageFile) throws IOException {
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
	public synchronized void deallocate(int pointer) {
		int count = 1;
		deallocate_(pointer, count);
	}

	@Override
	public Extent allocate(int count) {
		return new Extent(allocate_(count), count);
	}

	@Override
	public void deallocate(Extent pointer) {
		deallocate_(pointer.start, pointer.end);
	}

	private int allocate_(int count) {
		int pointer = findFreeExtentPages(count);

		// TODO extends allocation map if all pages are used

		updateAllocMap(pointer, pointer + count, (byte) 1);
		return lastAllocatedPointer = pointer + count;
	}

	private void deallocate_(int start, int end) {
		updateAllocMap(start, end, (byte) 0);
	}

	private int findFreeExtentPages(int count) {
		Fun<Integer, Byte> read = new Fun<Integer, Byte>() {
			private int start, end;
			private Bytes bytes;

			public Byte apply(Integer pointer) {
				if (bytes == null || pointer < start || end <= pointer) {
					int p = pointer / pageSize;
					start = p * pageSize;
					end = start + pageSize;
					bytes = allocMapFile.load(p);
				}
				return bytes.get(pointer - start);
			}
		};

		int start = lastAllocatedPointer + 1;
		int pos = start;
		while ((pos = checkNextEmptyExtent(read, pos)) < size) {
			int pos0 = pos;
			pos = checkEmptyExtent(read, pos, count);
			if (count <= pos - pos0)
				return pos0;
		}
		pos = 0;
		while ((pos = checkNextEmptyExtent(read, pos)) < start) {
			int pos0 = pos;
			pos = checkEmptyExtent(read, pos, count);
			if (count <= pos - pos0)
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
