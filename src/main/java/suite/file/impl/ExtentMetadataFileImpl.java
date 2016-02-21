package suite.file.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import suite.file.ExtentAllocator.Extent;
import suite.file.ExtentFile;
import suite.file.PageFile;
import suite.net.NetUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;

public class ExtentMetadataFileImpl implements Closeable, ExtentFile {

	private PageFile pageFile;
	private int blockSize;

	private class Block {
		Extent extent;
		Bytes bytes;

		private Block(Extent extent, Bytes bytes) {
			this.extent = extent;
			this.bytes = bytes;
		}
	}

	public ExtentMetadataFileImpl(PageFile pageFile) {
		this.pageFile = pageFile;
	}

	@Override
	public void close() throws IOException {
		pageFile.close();
	}

	@Override
	public void sync() {
		pageFile.sync();
	}

	@Override
	public Bytes load(Extent extent) {
		BytesBuilder bb = new BytesBuilder();
		for (int pointer = extent.start; pointer < extent.end; pointer++) {
			Block block = load(pointer);
			assert block.extent.start == extent.start;
			assert block.extent.end == extent.end;
			bb.append(block.bytes);
		}
		return bb.toBytes();
	}

	@Override
	public void save(Extent extent, Bytes bytes) {
		for (int pointer = extent.start; pointer < extent.end; pointer++) {
			save(pointer, new Block(extent, bytes.subbytes(extent.start * blockSize, extent.end * blockSize)));
		}
	}

	public Extent next(Extent extent) {
		return load(extent.end).extent;
	}

	public List<Extent> scan(int start, int end) {
		List<Extent> extents = new ArrayList<>();
		int pointer = start;
		while (pointer < end) {
			Extent extent = load(pointer).extent;
			if (extent.end <= end) {
				extents.add(extent);
				pointer = extent.end;
			}
		}
		return extents;
	}

	private Block load(int pointer) {
		Bytes bytes = pageFile.load(pointer);
		Extent extent = new Extent(NetUtil.bytesToInt(bytes.subbytes(0, 4)), NetUtil.bytesToInt(bytes.subbytes(4, 8)));
		return new Block(extent, bytes.subbytes(8));
	}

	private void save(int pointer, Block block) {
		Extent extent = block.extent;
		pageFile.save(pointer, NetUtil.intToBytes(extent.start).append(NetUtil.intToBytes(extent.end)).append(block.bytes));
	}

}
