package suite.file.impl;

import java.io.IOException;

import suite.file.PageFile;
import suite.util.SerializeUtil;

public class JournalledPageFileImpl extends JournalledDataFileImpl<Integer>implements PageFile {

	public JournalledPageFileImpl(String filename, int pageSize) throws IOException {
		super(new PageFileImpl(filename, pageSize), filename, pageSize, SerializeUtil.intSerializer);
	}

}
