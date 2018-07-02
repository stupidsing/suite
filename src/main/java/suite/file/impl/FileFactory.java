package suite.file.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import suite.file.ExtentAllocator.Extent;
import suite.file.ExtentFile;
import suite.file.PageFile;
import suite.node.util.Singleton;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.serialize.SerInput;
import suite.serialize.SerOutput;
import suite.serialize.Serialize.Serializer;
import suite.util.Fail;
import suite.util.Util;

public class FileFactory {

	private static class Block {
		private Extent extent;
		private Bytes bytes;

		private Block(Extent extent, Bytes bytes) {
			this.extent = extent;
			this.bytes = bytes;
		}
	}

	public static ExtentFile extentFile(PageFile pf) {
		var serialize = Singleton.me.serialize;
		var extentSerializer = serialize.extent();
		var bytesSerializer = serialize.variableLengthBytes;

		var pageFile = SerializedFileFactory.serialized(pf, new Serializer<Block>() {
			public Block read(SerInput si) throws IOException {
				var extent = extentSerializer.read(si);
				var bytes = bytesSerializer.read(si);
				return new Block(extent, bytes);
			}

			public void write(SerOutput so, Block block) throws IOException {
				extentSerializer.write(so, block.extent);
				bytesSerializer.write(so, block.bytes);
			}
		});

		return new ExtentFile() {
			public void close() throws IOException {
				pageFile.close();
			}

			public void sync() {
				pageFile.sync();
			}

			public Bytes load(Extent extent) {
				var bb = new BytesBuilder();
				for (var pointer = extent.start; pointer < extent.end; pointer++) {
					var block = pageFile.load(pointer);
					Util.assert_(Objects.equals(block.extent, extent));
					bb.append(block.bytes);
				}
				return bb.toBytes();
			}

			public void save(Extent extent, Bytes bytes) {
				for (int pointer = extent.start, p = 0; pointer < extent.end; pointer++) {
					var p1 = p + blockSize;
					pageFile.save(pointer, new Block(extent, bytes.range(p, p1)));
					p = p1;
				}
			}

			public List<Extent> scan(int start, int end) {
				var extents = new ArrayList<Extent>();
				var pointer = start;
				while (pointer < end) {
					var extent = pageFile.load(pointer).extent;
					if (start <= extent.start && extent.end <= end)
						extents.add(extent);
					pointer = extent.end;
				}
				return extents;
			}
		};
	}

	public static PageFile logged(PageFile pageFile) {
		return new PageFile() {
			public void close() throws IOException {
				LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].close()");
				pageFile.close();
			}

			public void sync() {
				LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].sync()");
				pageFile.sync();
			}

			public Bytes load(int pointer) {
				LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].load(" + pointer + ")");
				return pageFile.load(pointer);
			}

			public void save(int pointer, Bytes bytes) {
				LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].save(" + pointer + ")");
				pageFile.save(pointer, bytes);
			}
		};
	}

	public static PageFile pageFile(Path path, int pageSize) {
		var file = new RandomAccessibleFile(path);

		return new PageFile() {
			public void close() {
				file.close();
			}

			public void sync() {
				file.sync();
			}

			public Bytes load(int pointer) {
				int start = pointer * pageSize, end = start + pageSize;
				return file.load(start, end);
			}

			public void save(int pointer, Bytes bytes) {
				Util.assert_(bytes.size() <= pageSize);
				file.save(pointer * pageSize, bytes);
			}
		};
	}

	public static PageFile[] subPageFiles(PageFile parent, int... pointers) {
		var pageFiles = new PageFile[pointers.length - 1];
		for (var i = 0; i < pointers.length - 1; i++)
			pageFiles[i] = subPageFile(parent, pointers[i], pointers[i + 1]);
		return pageFiles;
	}

	private static PageFile subPageFile(PageFile parent, int startPointer, int endPointer) {
		return new PageFile() {
			public void close() {
			}

			public void sync() {
				parent.sync();
			}

			public Bytes load(int pointer) {
				return parent.load(convert(pointer));
			}

			public void save(int pointer, Bytes bytes) {
				parent.save(convert(pointer), bytes);
			}

			private Integer convert(Integer pointer0) {
				var pointer1 = pointer0 + startPointer;

				if (startPointer <= pointer1 && pointer1 < endPointer)
					return pointer1;
				else
					return Fail.t("page index out of range");
			}
		};
	}

}
