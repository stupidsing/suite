package suite.file.impl;

import java.io.IOException;

import suite.file.PageFile;
import suite.os.LogUtil;
import suite.primitive.Bytes;

public class PageFileLoggerImpl implements PageFile {

	private PageFile pageFile;

	public PageFileLoggerImpl(PageFile pageFile) {
		this.pageFile = pageFile;
	}

	@Override
	public void close() throws IOException {
		LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].close()");
		pageFile.close();
	}

	@Override
	public void sync() {
		LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].sync()");
		pageFile.sync();
	}

	@Override
	public Bytes load(Integer pointer) {
		LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].load(" + pointer + ")");
		return pageFile.load(pointer);
	}

	@Override
	public void save(Integer pointer, Bytes bytes) {
		LogUtil.info("PageFile[" + System.identityHashCode(pageFile) + "].save(" + pointer + ")");
		pageFile.save(pointer, bytes);
	}

}
