package suite.file.impl;

import java.io.IOException;
import java.nio.file.Path;

import suite.file.PageFile;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.util.Util;

public class PageFileFactory {

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

			public Bytes load(Integer pointer) {
				LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].load(" + pointer + ")");
				return pageFile.load(pointer);
			}

			public void save(Integer pointer, Bytes bytes) {
				LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].save(" + pointer + ")");
				pageFile.save(pointer, bytes);
			}
		};
	}

	public static PageFile pageFile(Path path, int pageSize) {
		RandomAccessibleFile file = new RandomAccessibleFile(path);

		return new PageFile() {
			public void close() {
				file.close();
			}

			public void sync() {
				file.sync();
			}

			public Bytes load(Integer pointer) {
				int start = pointer * pageSize, end = start + pageSize;
				return file.load(start, end);
			}

			public void save(Integer pointer, Bytes bytes) {
				Util.assert_(bytes.size() <= pageSize);
				file.save(pointer * pageSize, bytes);
			}
		};
	}

	public static PageFile subPageFile(PageFile parent, int startPointer, int endPointer) {
		return new PageFile() {
			public void close() {
			}

			public void sync() {
				parent.sync();
			}

			public Bytes load(Integer pointer) {
				return parent.load(convert(pointer));
			}

			public void save(Integer pointer, Bytes bytes) {
				parent.save(convert(pointer), bytes);
			}

			private Integer convert(Integer pointer0) {
				int pointer1 = pointer0 + startPointer;

				if (startPointer <= pointer1 && pointer1 < endPointer)
					return pointer1;
				else
					throw new RuntimeException("Page index out of range");
			}
		};
	}

}
