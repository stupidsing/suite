package suite.file.impl;

import primal.primitive.IntPrim.Int_Obj;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Bytes.BytesBuilder;
import suite.file.ExtentAllocator;
import suite.file.PageAllocator;
import suite.file.PageFile;
import suite.file.SerializedPageFile;

import java.io.IOException;

import static java.lang.Math.min;

/**
 * Manage B-tree pages on disk.
 */
public class AllocatorImpl implements PageAllocator, ExtentAllocator {

	private int size = 4096;
	private int pageSize = PageFile.defaultPageSize;

	private SerializedPageFile<Bytes> allocMapFile;
	private int lastAllocatedPointer;

	public AllocatorImpl(SerializedPageFile<Bytes> pageFile) {
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
		var count = 1;
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
		var pointer = findFreeExtentPages(count);

		// TODO extends allocation map if all pages are used

		updateAllocMap(pointer, pointer + count, (byte) 1);
		return lastAllocatedPointer = pointer + count;
	}

	private void deallocate_(int start, int end) {
		updateAllocMap(start, end, (byte) 0);
	}

	private int findFreeExtentPages(int count) {
		var read = new Int_Obj<Byte>() {
			private int start, end;
			private Bytes bytes;

			public Byte apply(int pointer) {
				if (bytes == null || pointer < start || end <= pointer) {
					var p = pointer / pageSize;
					start = p * pageSize;
					end = start + pageSize;
					bytes = allocMapFile.load(p);
				}
				return bytes.get(pointer - start);
			}
		};

		var start = lastAllocatedPointer + 1;
		var pos = start;
		while ((pos = checkNextEmptyExtent(read, pos)) < size) {
			var pos0 = pos;
			pos = checkEmptyExtent(read, pos, count);
			if (count <= pos - pos0)
				return pos0;
		}
		pos = 0;
		while ((pos = checkNextEmptyExtent(read, pos)) < start) {
			var pos0 = pos;
			pos = checkEmptyExtent(read, pos, count);
			if (count <= pos - pos0)
				return pos0;
		}
		return -1;
	}

	private void updateAllocMap(int start, int end, byte b) {
		while (start < end) {
			var p = start / pageSize;
			var s = p * pageSize;
			var e = s + pageSize;
			var end_ = min(e, end);
			var p0 = 0;
			var p1 = start - s;
			var p2 = end_ - s;
			var p3 = pageSize;
			var bytes = allocMapFile.load(p);
			var bb = new BytesBuilder();

			bb.append(bytes.range(p0, p1));
			for (var i = p1; i < p2; i++)
				bb.append(b);
			bb.append(bytes.range(p2, p3));

			allocMapFile.save(p, bb.toBytes());
			start = end_;
		}
	}

	private int checkEmptyExtent(Int_Obj<Byte> read, int pos, int max) {
		var end = min(size, pos + max);
		while (pos < end && read.apply(pos) == 0)
			pos++;
		return pos;
	}

	private int checkNextEmptyExtent(Int_Obj<Byte> read, int pos) {
		while (pos < size && read.apply(pos) == 1)
			pos++;
		return pos;
	}

}
