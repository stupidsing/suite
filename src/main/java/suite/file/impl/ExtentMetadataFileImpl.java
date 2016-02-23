package suite.file.impl;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import suite.file.DataFile;
import suite.file.ExtentAllocator.Extent;
import suite.file.ExtentFile;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class ExtentMetadataFileImpl implements Closeable, ExtentFile {

	private Serializer<Extent> extentSerializer = Serialize.extent();
	private Serializer<Bytes> bytesSerializer = Serialize.bytes(defaultPageSize - 8);

	private Serializer<Block> serializer = new Serializer<Block>() {
		public Block read(DataInput dataInput) throws IOException {
			Extent extent = extentSerializer.read(dataInput);
			Bytes bytes = bytesSerializer.read(dataInput);
			return new Block(extent, bytes);
		}

		public void write(DataOutput dataOutput, Block block) throws IOException {
			extentSerializer.write(dataOutput, block.extent);
			bytesSerializer.write(dataOutput, block.bytes);
		}
	};

	private SerializedPageFile<Block> pageFile;

	private class Block {
		Extent extent;
		Bytes bytes;

		private Block(Extent extent, Bytes bytes) {
			this.extent = extent;
			this.bytes = bytes;
		}
	}

	public ExtentMetadataFileImpl(PageFile pageFile) {
		this.pageFile = new SerializedPageFileImpl<>(pageFile, serializer);
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
			Block block = pageFile.load(pointer);
			assert block.extent.start == extent.start;
			assert block.extent.end == extent.end;
			bb.append(block.bytes);
		}
		return bb.toBytes();
	}

	@Override
	public void save(Extent extent, Bytes bytes) {
		int bs = DataFile.defaultPageSize - 8;
		for (int pointer = extent.start, p = 0; pointer < extent.end; pointer++) {
			int p1 = p + bs;
			pageFile.save(pointer, new Block(extent, bytes.subbytes(p, p1)));
			p = p1;
		}
	}

	public Extent next(Extent extent) {
		return pageFile.load(extent.end).extent;
	}

	public List<Extent> scan(int start, int end) {
		List<Extent> extents = new ArrayList<>();
		int pointer = start;
		while (pointer < end) {
			Extent extent = pageFile.load(pointer).extent;
			if (extent.end <= end) {
				extents.add(extent);
				pointer = extent.end;
			}
		}
		return extents;
	}

}
